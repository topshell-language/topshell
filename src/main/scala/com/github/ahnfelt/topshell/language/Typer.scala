package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

import scala.annotation.tailrec
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
    val constraints = new Constraints(unification)

    def freeInEnvironment() : Set[Int] = {
        environment = environment.mapValues(v => v.copy(
            constraints = v.constraints.map(unification.expand),
            generalized = unification.expand(v.generalized)
        ))
        environment.values.toList.flatMap(Pretty.freeInScheme)
    }.toSet

    def withVariables[T](variables : Seq[(String, Scheme)])(body : => T) = {
        val oldEnvironment = environment
        try {
            environment = environment ++ variables
            body
        } finally {
            environment = oldEnvironment
        }
    }

    def check(coreModules : Map[String, List[ModuleSymbol]], imports : List[TopImport], symbols : List[TopSymbol]) : List[TopSymbol] = {
        for(i <- imports; symbols <- coreModules.get(i.name)) {
            val fields = symbols.map { field =>
                val s = Parser.easy(i.url, field.`type`, _.parseScheme(false))
                TypeBinding(field.name, s)
            }
            environment += i.name -> Scheme(List(), List(), TRecord(fields))
        }
        var schemes = symbols.flatMap(s => s.binding.scheme.map(s.binding.name -> _).toList).toMap
        val result = symbols.zipWithIndex.map { case (s, i) => if(s.error.nonEmpty) s else {
            val expected1 = s.binding.scheme.map(_.generalized).getOrElse(constraints.freshTypeVariable())
            try {
                s.binding.scheme.foreach(constraints.checkAmbiguousScheme)
                withVariables(schemes.toList) {
                    val v = checkTerm(s.binding.value, expected1)
                    val expected3 = if(!s.bind) expected1 else {
                        val expected2 = constraints.freshTypeVariable()
                        unification.unify(TApply(TConstructor("Task"), expected2), expected1)
                        expected2
                    }
                    val actual = constraints.generalize(expected3, freeInEnvironment(), topLevel = true)
                    s.binding.scheme.foreach(constraints.checkTypeAnnotation(s.binding.at, _, actual))
                    constraints.assureEmpty()
                    val scheme = s.binding.scheme.getOrElse(actual)
                    schemes += (s.binding.name -> scheme)
                    s.copy(binding = s.binding.copy(value = v, scheme = Some(scheme)))
                }
            } catch {
                case e : RuntimeException =>
                    e.printStackTrace()
                    val parseException = e match {
                        case parseException : ParseException => parseException
                        case _ => ParseException(Location("unknown", 0, 0), e.getMessage)
                    }
                    constraints.clearConstraints()
                    s.copy(error = Some(parseException))
            }
        }}
        result
    }

    def checkTerm(term : Term, expected : Type) : Term = term match {

        case EString(at, value) =>
            unification.unify(expected, TConstructor("String"))
            term

        case ENumber(at, value) =>
            unification.unify(expected, TConstructor("Number"))
            term

        case EVariable(at, name) =>
            environment.get(name) match {
                case Some(scheme) =>
                    unification.unify(expected, constraints.instantiate(Some(scheme)))
                    term
                case None =>
                    throw ParseException(at, "Forward reference requires a type annotation: " + name)
            }

        case EFunction(at, variable, body) =>
            val t1 = constraints.freshTypeVariable()
            val t2 = constraints.freshTypeVariable()
            unification.unify(expected, TApply(TApply(TConstructor("->"), t1), t2))
            val b = withVariables(Seq(variable -> Scheme(List(), List(), t1))) {
                checkTerm(body, t2)
            }
            EFunction(at, variable, b)

        case EApply(at, function, argument) =>
            val t1 = constraints.freshTypeVariable()
            val t2 = TApply(TApply(TConstructor("->"), t1), expected)
            val a = checkTerm(argument, t1)
            val f = checkTerm(function, t2)
            EApply(at, f, a)

        case ELet(at, bindings, body) =>
            val variables = bindings.map { b =>
                b.name -> b.scheme.getOrElse(Scheme(List(), List(), constraints.freshTypeVariable()))
            }
            val t1 = constraints.freshTypeVariable()
            val bs = withVariables(variables) {
                val bs1 = bindings.map { b =>
                    val t2 = constraints.instantiate(Some(variables.find(b.name == _._1).get._2))
                    b.copy(
                        value = checkTerm(b.value, t2)
                    ) -> t2
                }
                bs1.map { case (b, t2) => b.copy(scheme = Some(constraints.generalize(t2, freeInEnvironment(), topLevel = false))) }
            }
            val body2 = withVariables(bs.map(b => b.name -> b.scheme.get)) {
                checkTerm(body, t1)
            }
            unification.unify(expected, t1)
            ELet(at, bs, body2)

        case EBind(at, binding, body) =>
            val t1 = constraints.freshTypeVariable()
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
            val t3 = TApply(TConstructor(constructor), constraints.freshTypeVariable())
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
            val t1 = constraints.freshTypeVariable()
            val es = elements.map(checkTerm(_, t1))
            val t2 = TApply(TConstructor("List"), t1)
            val r = rest.map(checkTerm(_, t2))
            unification.unify(expected, t2)
            EList(at, es, r)

        case ERecord(at, fields, rest) =>
            val seen = mutable.HashSet[String]()
            val expectedSchemes = (unification.expand(expected) match {
                case TRecord(expectedFields) => expectedFields.map(f => f.name -> f.scheme)
                case _ => List()
            }).toMap
            val (fs, ss) = fields.map { f =>
                if(!seen.add(f.name)) {
                    throw new RuntimeException("Duplicate field " + f.name + " in: " + term)
                }
                // There's no concrete syntax for writing explicit types directly on record literals, so no check
                val expectedScheme = expectedSchemes.get(f.name)
                val t1 = expectedScheme.map(_.generalized).getOrElse(constraints.freshTypeVariable())
                val v = checkTerm(f.value, t1)
                val s1 = constraints.generalize(t1, freeInEnvironment(), false)
                // A kind of value restriction for record fields, since the following is wrong in a non-lazy language:
                // a -> {x: a.y}  :  a -> {x: b => b | a.y : b}
                val s2 = f.value match {
                    case _ : EFunction | _ : EVariable => s1
                    case _ => Scheme(List(), List(), constraints.instantiate(Some(s1)))
                }
                expectedScheme.foreach(constraints.checkTypeAnnotation(f.at, _, s2))
                val s3 = expectedScheme.getOrElse(s2)
                f.copy(scheme = Some(s3), value = v) -> TypeBinding(f.name, s3)
            }.unzip
            val t2 = constraints.freshTypeVariable()
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
            val t1 = constraints.freshTypeVariable()
            val r = checkTerm(record, t1)
            val t3 = unification.expand(t1) match {
                case TRecord(fields2) =>
                    val ss2 = fields2
                    ss2.find(_.name == field) match {
                        case Some(b) =>
                            constraints.instantiate(Some(b.scheme))
                        case None =>
                            throw new RuntimeException(
                                "Field ." + field + " not found in {" + ss2.map(_.name).mkString(", ") + "}"
                            )
                    }
                case other =>
                    val t2 = constraints.freshTypeVariable()
                    constraints.add(FieldConstraint(other, field, t2, optional))
                    t2
            }
            val t4 = if(optional) TApply(TConstructor("Maybe"), t3) else t3
            unification.unify(expected, t4)
            EField(at, r, field, optional)

        case EIf(at, condition, thenBody, Some(elseBody)) =>
            val c = checkTerm(condition, TConstructor("Bool"))
            val t = checkTerm(thenBody, expected)
            val e = checkTerm(elseBody, expected)
            EIf(at, c, t, Some(e))

        case EIf(at, condition, thenBody, None) =>
            val t1 = constraints.freshTypeVariable()
            unification.unify(expected, TApply(TConstructor("Maybe"), t1))
            val c = checkTerm(condition, TConstructor("Bool"))
            val t = checkTerm(thenBody, t1)
            EIf(at, c, t, None)

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
            constraints.instantiate(Some(s)) match {
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
