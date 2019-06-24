package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._
import com.github.ahnfelt.topshell.worker.Timer

class Unification(initialEnvironment : Map[Int, Type]) {

    var sub = initialEnvironment

    def copy() = new Unification(sub)

    def reset() = sub = initialEnvironment

    def occursCheck(id : Int, theType : Type) : Unit = {
        if(Pretty.freeInType(theType).contains(id)) {
            throw new RuntimeException("Infinite type: " + TVariable(id) + " == " + theType)
        }
    }

    def bind(id : Int, theType : Type) : Unit = if(TVariable(id) != theType) {
        occursCheck(id, theType)
        val replacement = Map[Type, Type](TVariable(id) -> theType)
        sub = sub.mapValues(replace(_, replacement))
        sub += (id -> theType)
    }

    def unify(type1 : Type, type2 : Type) : Unit = doUnify(type1, type2)

    private def doUnify(type1 : Type, type2 : Type) : Unit = (type1, type2) match {

        case (TVariable(id1), TVariable(id2)) =>
            if(id1 != id2) {
                val t1Option = sub.get(id1)
                val t2Option = sub.get(id2)
                (t1Option, t2Option) match {
                    case (None, None) => bind(id2, type1)
                    case (None, Some(t2)) => bind(id1, t2)
                    case (Some(t1), None) => bind(id2, t1)
                    case (Some(t1), Some(t2)) => doUnify(t1, t2)
                }
            }

        case (TVariable(id1), _) =>
            sub.get(id1) match {
                case Some(value) => doUnify(value, type2)
                case None => bind(id1, type2)
            }

        case (_, TVariable(id2)) =>
            sub.get(id2) match {
                case Some(value) => doUnify(type1, value)
                case None => bind(id2, type1)
            }

        case (TParameter(name1), TParameter(name2)) =>
            if(name1 != name2) {
                throw new RuntimeException("Type mismatch: " + name2 + " vs. " + name1)
            }

        case (TConstructor(name1), TConstructor(name2)) =>
            if(name1 != name2) {
                throw new RuntimeException("Type mismatch: " + name2 + " vs. " + name1)
            }

        case (TSymbol(name1), TSymbol(name2)) =>
            if(name1 != name2) {
                throw new RuntimeException("Type mismatch: " + name2 + " vs. " + name1)
            }

        case (TApply(constructor1, argument1), TApply(constructor2, argument2)) =>
            doUnify(constructor1, constructor2)
            doUnify(argument1, argument2)

        case (TVariant(variants1), TVariant(variants2)) =>
            val sorted1 = variants1.sortBy(_._1)
            val sorted2 = variants2.sortBy(_._1)
            if(sorted1.map(_._1) != sorted2.map(_._1)) {
                throw new RuntimeException(
                    "Variant names don't match: " +
                        sorted2.map(_._1).mkString(", ") + " vs. " +
                        sorted1.map(_._1).mkString(", ")
                )
            } else {
                sorted1.zip(sorted2).foreach { case ((x1, ts1), (x2, ts2)) =>
                    if(ts1.size < ts2.size) throw new RuntimeException("Too many arguments: " + x2 + ts2.map(" " + _).mkString)
                    if(ts1.size > ts2.size) throw new RuntimeException("Too few arguments: " + x2 + ts2.map(" " + _).mkString)
                    ts1.zip(ts2).foreach { case (t1, t2) => doUnify(t1, t2) }
                }
            }

        case (TRecord(fields1), TRecord(fields2)) =>
            val sorted1 = fields1.sortBy(_.name)
            val sorted2 = fields2.sortBy(_.name)
            if(sorted1.map(_.name) != sorted2.map(_.name)) {
                throw new RuntimeException(
                    "Record fields don't match: " +
                        "{" + sorted2.map(_.name).mkString(", ") + "} vs. " +
                        "{" + sorted1.map(_.name).mkString(", ") + "}"
                )
            } else {
                sorted1.zip(sorted2).foreach { case (b1, b2) =>
                    val parameters1 = b1.scheme.parameters.sortBy(_.name)
                    val parameters2 = b2.scheme.parameters.sortBy(_.name)
                    if(parameters1.size != parameters2.size) {
                        throw new RuntimeException(
                            "Incompatible type parameters: " +
                                Pretty.showScheme(b1.scheme, true) + " vs. " +
                                Pretty.showScheme(b2.scheme, true)
                        )
                    }
                    parameters1.zip(parameters2).find { case (p1, p2) => p1.kind != p2.kind }.foreach { case (p1, p2) =>
                        throw new RuntimeException(
                            "Incompatible kinds: " +
                                p1.name + " : " + p1.kind + " vs. " +
                                p2.name + " : " + p2.kind + "."
                        )
                    }
                    if(b1.scheme.constraints.size != b2.scheme.constraints.size) {
                        throw new RuntimeException(
                            "Incompatible constraints: " +
                                Pretty.showScheme(b1.scheme, true) + " vs. " +
                                Pretty.showScheme(b2.scheme, true)
                        )
                    }
                    val replacement1 = parameters1.zipWithIndex.map {
                        case (p, i) => TParameter(p.name) -> TParameter("_p" + (i + 1))
                    }.toMap[Type, Type]
                    val replacement2 = parameters2.zipWithIndex.map {
                        case (p, i) => TParameter(p.name) -> TParameter("_p" + (i + 1))
                    }.toMap[Type, Type]
                    // Assumes that constraints are sorted
                    val c1 = b1.scheme.constraints.map(Pretty.replace(_, replacement1, sub.get)).map(expand)
                    val c2 = b2.scheme.constraints.map(Pretty.replace(_, replacement2, sub.get)).map(expand)
                    c1.zip(c2).foreach { case (a, b) => doUnify(a, b) }
                    val t1 = expand(Pretty.replace(b1.scheme.generalized, replacement1, sub.get))
                    val t2 = expand(Pretty.replace(b2.scheme.generalized, replacement2, sub.get))
                    doUnify(t1, t2)
                }
            }

        case (_, _) =>
            throw new RuntimeException("Type mismatch: " + expand(type2) + " vs. " + expand(type1))

    }

    def expand(unexpanded : Type) : Type = Timer.accumulate("expand") { doExpand(unexpanded) }

    private def doExpand(unexpanded : Type) : Type = unexpanded match {
        case TVariable(id) =>
            sub.get(id).map { t1 =>
                occursCheck(id, t1)
                val t2 = doExpand(t1)
                bind(id, t2)
                t2
            }.getOrElse(unexpanded)
        case TParameter(name) =>
            unexpanded
        case TConstructor(name) =>
            unexpanded
        case TSymbol(name) =>
            unexpanded
        case TVariant(variants) =>
            TVariant(variants.map { case (n, t) => n -> t.map(doExpand) })
        case TApply(constructor, argument) =>
            TApply(doExpand(constructor), doExpand(argument))
        case TRecord(fields) =>
            TRecord(fields.map(f => f.copy(scheme = f.scheme.copy(
                constraints = f.scheme.constraints.map(doExpand),
                generalized = doExpand(f.scheme.generalized)
            ))))
    }

    def replace(search : Type, replacement : Map[Type, Type]) = Pretty.replace(search, replacement, sub.get)

}
