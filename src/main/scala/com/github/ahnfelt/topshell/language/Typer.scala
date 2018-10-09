package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

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

    def generalize(theType : Type) : Scheme = {
        val t = unification.expand(theType)
        val nonFree = freeInEnvironment().toSet
        val free = Pretty.freeInType(t).filterNot(nonFree)
        val replacementList = free.map(id => TVariable(id) -> TParameter("$" + id))
        val replacement = replacementList.toMap[Type, Type]
        val cs1 = constraints.map(unification.replace(_, replacement))
        val (cs2, cs3) = constraints.zip(cs1).partition { case (c1, c2) => c1 != c2 }
        constraints = cs3.map(_._2)
        val simplified = cs2.map(_._2) // Simplify constraints
        val generalized = unification.replace(t, replacement)
        val parameters = replacementList.map { case (_, p) => TypeParameter(p.name, KStar()) } // Kind
        Scheme(parameters, simplified, generalized)
    }

    def instantiate(scheme : Option[Scheme]) : Type = scheme.map { s =>
        val replacement = s.parameters.map(p => TParameter(p.name) -> freshTypeVariable())
        constraints = s.constraints.map(unification.replace(_, replacement.toMap)) ++ constraints
        unification.replace(s.generalized, replacement.toMap) // Kind
    }.getOrElse(freshTypeVariable())

    def check(imports : List[TopImport], symbols : List[TopSymbol]) : List[TopSymbol] = {
        // Imports
        var schemes = symbols.map(s =>
            s.binding.name -> s.binding.scheme.getOrElse(Scheme(List(), List(), freshTypeVariable()))
        ).toMap
        symbols.map { s =>
            val expected = instantiate(s.binding.scheme) // Don't instantiate here?
            try {
                withVariables(symbols.map(x => x.binding.name -> schemes(x.binding.name))) {
                    val v = checkTerm(s.binding.value, expected)
                    val scheme = generalize(expected) // Check existing scheme, if present
                    println("Generalized: " + scheme)
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
            val (fs, ss) = fields.map { f =>
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
            val t4 = TRecord(ss ++ ss2)
            unification.unify(expected, t4)
            ERecord(at, fs, r)

        case EField(at, record, field, optional) =>
            val t1 = freshTypeVariable()
            val r = checkTerm(record, t1)
            val ss2 = unification.expand(t1) match {
                case TRecord(fields2) =>
                    fields2
                case other =>
                    throw new RuntimeException("No such field: " + field + " in non-record: " + other)
            }
            val t2 = ss2.find(_.name == field) match {
                case Some(b) =>
                    instantiate(Some(b.scheme))
                case None =>
                    throw new RuntimeException("No such field: " + field + " in: " + ss2)
            }
            unification.unify(expected, t2)
            EField(at, r, field, optional)

        case EIf(at, condition, thenBody, elseBody) =>
            val t1 = freshTypeVariable()
            val c = checkTerm(condition, TConstructor("Bool"))
            val t = checkTerm(thenBody, t1)
            val e = checkTerm(elseBody, t1)
            unification.unify(expected, t1)
            EIf(at, c, t, e)

        case EUnary(at, operator, operand) =>
            val t1 = freshTypeVariable() // Use operator type
            val o = checkTerm(operand, t1)
            unification.unify(expected, t1)
            EUnary(at, operator, o)

        case EBinary(at, operator, left, right) =>
            val t1 = freshTypeVariable() // Use operator type
            val l = checkTerm(left, t1)
            val r = checkTerm(right, t1)
            unification.unify(expected, t1)
            EBinary(at, operator, l, r)

    }

}
