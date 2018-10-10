package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
trait ModuleSymbol extends js.Any {
    val name : String = js.native
    val `type` : String = js.native
}

class Typer {

    var environment = Map[String, Scheme]()
    val unification = new Unification(Map.empty)
    var constraints = List[Type]()

    var previousTypeVariable = 0
    def freshTypeVariable() = {
        previousTypeVariable += 1
        TVariable(previousTypeVariable)
    }

    def freeInEnvironment() : List[Int] = {
        environment = environment.mapValues(v => v.copy(
            constraints = v.constraints.map(unification.expand),
            generalized = unification.expand(v.generalized)
        ))
        environment.values.toList.flatMap(Pretty.freeInScheme)
    }.distinct

    def withVariables[T](variables : Seq[(String, Scheme)])(body : => T) = {
        val oldEnvironment = environment
        try {
            environment = environment ++ variables
            body
        } finally {
            environment = oldEnvironment
        }
    }

    def simplifyConstraint(constraint : Type) : Option[Type] = constraint match {
        case TApply(TApply(TApply(TConstructor(o), TSymbol(label)), t), record) if o == "." || o == ".?" =>
            val optional = o == ".?"
            record match {
                case TRecord(fields) =>
                    fields.find(_.name == label).map { field =>
                        unification.unify(t, instantiate(Some(field.scheme)))
                        None
                    }.getOrElse {
                        if(optional) None
                        else throw new RuntimeException("Missing field " + label + " in: " + record)
                    }
                case TParameter(_) =>
                    Some(constraint)
                case TVariable(_) =>
                    Some(constraint)
                case _ =>
                    throw new RuntimeException("Missing field " + label + " in non-record: " + record)
            }
        case TApply(TConstructor("Add"), target) =>
            target match {
                case TConstructor("Number") =>
                    None
                case TConstructor("String") =>
                    None
                case TParameter(_) =>
                    Some(constraint)
                case TVariable(_) =>
                    Some(constraint)
                case _ =>
                    throw new RuntimeException("Not true: " + constraint)
            }
        case TApply(TConstructor("Equal"), target) =>
            target match {
                case TConstructor("Number") =>
                    None
                case TConstructor("String") =>
                    None
                case TParameter(_) =>
                    Some(constraint)
                case TVariable(_) =>
                    Some(constraint)
                case _ =>
                    throw new RuntimeException("Not true: " + constraint)
            }
        case TApply(TConstructor("Order"), target) =>
            target match {
                case TConstructor("Number") =>
                    None
                case TConstructor("String") =>
                    None
                case TParameter(_) =>
                    Some(constraint)
                case TVariable(_) =>
                    Some(constraint)
                case _ =>
                    throw new RuntimeException("Not true: " + constraint)
            }
        case _ =>
            throw new RuntimeException("Invalid constraint: " + constraint)
    }

    def generalize(theType : Type) : Scheme = {
        var error : Option[RuntimeException] = None
        constraints = constraints.map(unification.expand).distinct.flatMap { c =>
            try {
                simplifyConstraint(c)
            } catch {
                case e : RuntimeException =>
                    error = Some(e)
                    None
            }
        }
        error.foreach(throw _)
        val t = unification.expand(theType)
        val nonFree = freeInEnvironment().toSet
        val free = Pretty.freeInType(t).filterNot(nonFree)
        val replacementList = free.map(id => TVariable(id) -> TParameter("$" + id))
        val replacement = replacementList.toMap[Type, Type]
        val cs1 = constraints.map(unification.replace(_, replacement))
        val (cs2, cs3) = constraints.zip(cs1).partition { case (c1, c2) => c1 != c2 }
        constraints = cs3.map(_._2)
        val simplified = cs2.map(_._2)
        val generalized = unification.replace(t, replacement)
        val parameters = replacementList.map { case (_, p) => TypeParameter(p.name, KStar()) } // Kind
        Pretty.renameParameterNames(Scheme(parameters, simplified, generalized), unification.sub.get)
    }

    def instantiate(scheme : Option[Scheme]) : Type = scheme.map { s =>
        val replacement = s.parameters.map(p => TParameter(p.name) -> freshTypeVariable()).toMap[Type, Type]
        constraints = s.constraints.map(unification.replace(_, replacement)) ++ constraints
        unification.replace(s.generalized, replacement) // Kind
    }.getOrElse(freshTypeVariable())

    def check(coreModules : Map[String, List[ModuleSymbol]], imports : List[TopImport], symbols : List[TopSymbol]) : List[TopSymbol] = {
        for(i <- imports; symbols <- coreModules.get(i.name)) {
            val fields = symbols.map { field =>
                val s = Parser.easy(i.url, field.`type`, _.parseScheme())
                TypeBinding(field.name, s)
            }
            environment += i.name -> Scheme(List(), List(), TRecord(fields))
        }
        var schemes = symbols.map(s =>
            s.binding.name -> s.binding.scheme.getOrElse(Scheme(List(), List(), freshTypeVariable()))
        ).toMap
        symbols.map { s =>
            val expected1 = instantiate(s.binding.scheme) // Don't instantiate here?
            try {
                withVariables(symbols.map(x => x.binding.name -> schemes(x.binding.name))) {
                    val v = checkTerm(s.binding.value, expected1)
                    val expected2 = if(!s.bind) expected1 else unification.expand(expected1) match {
                        case TApply(TConstructor("Task"), argument) => argument
                        case t => throw ParseException(s.binding.at, "Not a task: " + t)
                    }
                    val scheme = generalize(expected2) // Check existing scheme, if present
                    schemes += (s.binding.name -> scheme)
                    s.copy(binding = s.binding.copy(value = v, scheme = Some(scheme)))
                }
            } catch {
                case e : RuntimeException =>
                    println(unification.sub.toList.sortBy(_._1).map { case (k, v) => "_" + k + " = " + v }.mkString("\n"))
                    e.printStackTrace()
                    val parseException = ParseException(Location("unknown", 0, 0), e.getMessage)
                    s.copy(error = Some(parseException))
            }
        }
    }

    def checkTerm(term : Term, expected : Type) : Term = term match {

        case EString(at, value) =>
            unification.unify(expected, TConstructor("String"))
            term

        case ENumber(at, value) =>
            unification.unify(expected, TConstructor("Number"))
            term

        case EVariable(at, name) =>
            unification.unify(expected, instantiate(Some(environment(name))))
            term

        case EFunction(at, variable, body) =>
            val t1 = freshTypeVariable()
            val t2 = freshTypeVariable()
            val b = withVariables(Seq(variable -> Scheme(List(), List(), t1))) {
                checkTerm(body, t2)
            }
            unification.unify(expected, TApply(TApply(TConstructor("->"), t1), t2))
            EFunction(at, variable, b)

        case EApply(at, function, argument) =>
            val t1 = freshTypeVariable()
            val t2 = TApply(TApply(TConstructor("->"), t1), expected)
            val a = checkTerm(argument, t1)
            val f = checkTerm(function, t2)
            EApply(at, f, a)

        case ELet(at, bindings, body) =>
            val variables = bindings.map { b =>
                b.name -> b.scheme.getOrElse(Scheme(List(), List(), freshTypeVariable()))
            }
            val t1 = freshTypeVariable()
            val bs = withVariables(variables) {
                val bs1 = bindings.map { b =>
                    val t2 = instantiate(Some(variables.find(b.name == _._1).get._2))
                    b.copy(
                        value = checkTerm(b.value, t2)
                    ) -> t2
                }
                bs1.map { case (b, t2) => b.copy(scheme = Some(generalize(t2))) }
            }
            val body2 = withVariables(bs.map(b => b.name -> b.scheme.get)) {
                checkTerm(body, t1)
            }
            unification.unify(expected, t1)
            ELet(at, bs, body2)

        case EBind(at, binding, body) =>
            val t1 = freshTypeVariable()
            val v = checkTerm(binding.value, t1)
            val (constructor, t2) = unification.expand(t1) match {
                case TApply(TConstructor(name), argument) =>
                    if(name != "List" && name != "Task") {
                        throw new RuntimeException("Expected List or Task, got: " + name)
                    }
                    name -> argument
                case unbindableType =>
                    throw new RuntimeException("Not bindable: " + unbindableType)
            }
            val t3 = TApply(TConstructor(constructor), freshTypeVariable())
            val b = withVariables(Seq(binding.name -> Scheme(List(), List(), t2))) {
                checkTerm(body, t3)
            }
            unification.unify(expected, t3)
            if(binding.scheme.exists(s => s.parameters.nonEmpty || s.constraints.nonEmpty)) {
                throw new RuntimeException(
                    "Bind may not have type parameters or constraints: " +
                    binding.name + " : " + binding.scheme.get
                )
            }
            val s = Scheme(List(), List(), t1)
            EBind(at, Binding(binding.at, binding.name, Some(s), v), b)

        case EList(at, elements, rest) =>
            val t1 = freshTypeVariable()
            val es = elements.map(checkTerm(_, t1))
            val t2 = TApply(TConstructor("List"), t1)
            val r = rest.map(checkTerm(_, t2))
            unification.unify(expected, t2)
            EList(at, es, r)

        case ERecord(at, fields, rest) =>
            val seen = mutable.HashSet[String]()
            val (fs, ss) = fields.map { f =>
                if(!seen.add(f.name)) {
                    throw new RuntimeException("Duplicate field " + f.name + " in: " + term)
                }
                val t1 = freshTypeVariable()
                val v = checkTerm(f.value, t1)
                // Also use the explicit scheme, if present
                val s = Scheme(List(), List(), t1) // Generalize, check constraints.
                f.copy(scheme = Some(s), value = v) -> TypeBinding(f.name, s)
            }.unzip
            val t2 = freshTypeVariable()
            val r = rest.map(checkTerm(_, t2))
            val ss2 = r.map(_ => unification.expand(t2) match {
                case TRecord(fields2) =>
                    fields2
                case other =>
                    throw new RuntimeException("Not a record: " + other)
            }).getOrElse(List())
            val ss3 = ss.map(_.name).toSet
            val t4 = TRecord(ss ++ ss2.filterNot(s => ss3(s.name)))
            unification.unify(expected, t4)
            ERecord(at, fs, r)

        case EField(at, record, field, optional) =>
            val t1 = freshTypeVariable()
            val r = checkTerm(record, t1)
            val t3 = unification.expand(t1) match {
                case TRecord(fields2) =>
                    val ss2 = fields2
                    ss2.find(_.name == field) match {
                        case Some(b) =>
                            instantiate(Some(b.scheme))
                        case None =>
                            throw new RuntimeException("No such field: " + field + " in: " + ss2)
                    }
                case other =>
                    val t2 = freshTypeVariable()
                    val o = if(optional) ".?" else "."
                    constraints ::= TApply(TApply(TApply(TConstructor(o), TSymbol(field)), t2), other)
                    t2
            }
            val t4 = if(optional) TApply(TConstructor("Maybe"), t3) else t3
            unification.unify(expected, t4)
            EField(at, r, field, optional)

        case EIf(at, condition, thenBody, elseBody) =>
            val t1 = freshTypeVariable()
            val c = checkTerm(condition, TConstructor("Bool"))
            val t = checkTerm(thenBody, t1)
            val e = checkTerm(elseBody, t1)
            unification.unify(expected, t1)
            EIf(at, c, t, e)

        case EUnary(at, operator, operand) =>
            val t1 =
                if(operator == "-") TConstructor("Number")
                else if(operator == "!") TConstructor("Bool")
                else throw ParseException(at, "Unknown operator: " + operator)
            val o = checkTerm(operand, t1)
            unification.unify(expected, t1)
            EUnary(at, operator, o)

        case EBinary(at, operator, left, right) =>
            val s = Syntax.binaryOperatorSchemes(operator)
            instantiate(Some(s)) match {
                case TApply(TApply(TConstructor("->"), t1), TApply(TApply(TConstructor("->"), t2), t3)) =>
                    val l = checkTerm(left, t1)
                    val r = checkTerm(right, t2)
                    unification.unify(expected, t3)
                    EBinary(at, operator, l, r)
                case _ =>
                    throw ParseException(at, "Bad type for binary operator: " + s)
            }

    }

}
