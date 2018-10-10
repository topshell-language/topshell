package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

class Unification(initialEnvironment : Map[Int, Type]) {

    var sub = initialEnvironment

    def copy() = new Unification(sub)

    def bind(id : Int, theType : Type) : Unit = {
        if(Pretty.freeInType(theType).contains(id)) {
            throw new RuntimeException("Infinite type: " + expand(TVariable(id)) + " = " + expand(theType))
        }
        sub = sub + (id -> theType)
    }

    def unify(type1 : Type, type2 : Type) : Unit = (type1, type2) match {

        case (TVariable(id1), TVariable(id2)) =>
            if(id1 != id2) {
                val t1Option = sub.get(id1)
                val t2Option = sub.get(id2)
                (t1Option, t2Option) match {
                    case (None, None) => bind(id2, type1)
                    case (None, Some(t2)) => bind(id1, t2)
                    case (Some(t1), None) => bind(id2, t1)
                    case (Some(t1), Some(t2)) => unify(t1, t2)
                }
            }

        case (TVariable(id1), _) =>
            sub.get(id1) match {
                case Some(value) => unify(value, type2)
                case None => bind(id1, type2)
            }

        case (_, TVariable(id2)) =>
            sub.get(id2) match {
                case Some(value) => unify(type1, value)
                case None => bind(id2, type1)
            }

        case (TParameter(name1), TParameter(name2)) =>
            if(name1 != name2) {
                throw new RuntimeException("Got: " + name2 + ", expected: " + name1)
            }

        case (TConstructor(name1), TConstructor(name2)) =>
            if(name1 != name2) {
                throw new RuntimeException("Got: " + name2 + ", expected: " + name1)
            }

        case (TSymbol(name1), TSymbol(name2)) =>
            if(name1 != name2) {
                throw new RuntimeException("Got: " + name2 + ", expected: " + name1)
            }

        case (TApply(constructor1, argument1), TApply(constructor2, argument2)) =>
            unify(constructor1, constructor2)
            unify(argument1, argument2)

        case (TRecord(fields1), TRecord(fields2)) =>
            val sorted1 = fields1.sortBy(_.name)
            val sorted2 = fields2.sortBy(_.name)
            if(sorted1.map(_.name) != sorted2.map(_.name)) {
                throw new RuntimeException(
                    "Record fields don't match. " +
                        "Got: {" + sorted2.map(_.name).mkString(", ") + "}, " +
                        "expected: {" + sorted1.map(_.name).mkString(", ") + "}"
                )
            } else {
                sorted1.zip(sorted2).foreach { case (b1, b2) =>
                    val parameters1 = b1.scheme.parameters
                    val parameters2 = b2.scheme.parameters
                    if(parameters1.size != parameters2.size) {
                        throw new RuntimeException(
                            "Incompatible fields: " + b1.name + " and " + b2.name +
                            " have different numbers of type parameters: " +
                            parameters1.map(_.name).mkString(", ") + " vs. " +
                            parameters2.map(_.name).mkString(", ") + "."
                        )
                    }
                    parameters1.zip(parameters2).find { case (p1, p2) => p1.kind != p2.kind }.foreach { case (p1, p2) =>
                        throw new RuntimeException(
                            "Incompatible type parameters: " +
                            p1.name + " : " + p1.kind + " vs. " +
                            p2.name + " : " + p2.kind + "."
                        )
                    }
                    if(b1.scheme.constraints.size != b2.scheme.constraints.size) {
                        throw new RuntimeException(
                            "Incompatible fields: " + b1.name + " and " + b2.name +
                                " have different numbers of constraints: " +
                                b1.scheme.constraints.map(expand).mkString(", ") + " vs. " +
                                b2.scheme.constraints.map(expand).mkString(", ") + "."
                        )
                    }
                    val replacement1 = parameters1.zipWithIndex.map {
                        case (p, i) => TParameter(p.name) -> TParameter("#" + i)
                    }.toMap[Type, Type]
                    val replacement2 = parameters2.zipWithIndex.map {
                        case (p, i) => TParameter(p.name) -> TParameter("#" + i)
                    }.toMap[Type, Type]
                    // Assumes that constraints are sorted
                    val c1 = b1.scheme.constraints.map(Pretty.replace(_, replacement1, sub.get)).map(expand)
                    val c2 = b2.scheme.constraints.map(Pretty.replace(_, replacement2, sub.get)).map(expand)
                    // Don't use unification to check: forall a. a -> _1 != forall a. _2 -> a, but they unifyInternal.
                    c1.zip(c2).foreach { case (a, b) => if(a != b) {
                        throw new RuntimeException(
                            "Incompatible constraints for " + b1.name + ": " + a + " vs. " + b + "."
                        )
                    } else unify(a, b)}
                    val t1 = expand(Pretty.replace(b1.scheme.generalized, replacement1, sub.get))
                    val t2 = expand(Pretty.replace(b2.scheme.generalized, replacement2, sub.get))
                    if(t1 != t2) {
                        throw new RuntimeException(
                            "Incompatible field types for " + b1.name + ": " + t1 + " vs. " + t2 + "."
                        )
                    }
                    unify(t1, t2)
                }
            }

        case (_, _) =>
            throw new RuntimeException(
                "Incompatible types. " +
                    "Got: " + expand(type2) + ", " +
                    "expected: " + expand(type1)
            )

    }

    def expand(unexpanded : Type) : Type = unexpanded match {
        case TVariable(id) =>
            sub.get(id).map(expand).getOrElse(unexpanded)
        case TParameter(name) =>
            unexpanded
        case TConstructor(name) =>
            unexpanded
        case TSymbol(name) =>
            unexpanded
        case TApply(constructor, argument) =>
            TApply(expand(constructor), expand(argument))
        case TRecord(fields) =>
            TRecord(fields.map(f => f.copy(scheme = f.scheme.copy(
                constraints = f.scheme.constraints.map(expand),
                generalized = expand(f.scheme.generalized)
            ))))
    }

    def replace(search : Type, replacement : Map[Type, Type]) = Pretty.replace(search, replacement, sub.get)

}
