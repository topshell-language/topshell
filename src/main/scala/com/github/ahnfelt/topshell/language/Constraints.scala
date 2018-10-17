package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

import scala.annotation.tailrec
import scala.collection.mutable

class Constraints(val unification : Unification, initialTypeVariable : Int = 0, initialConstraints : List[Type] = List()) {

    private var previousTypeVariable = initialTypeVariable
    def freshTypeVariable() = {
        previousTypeVariable += 1
        TVariable(previousTypeVariable)
    }

    private var constraints = initialConstraints

    def copy() = new Constraints(unification.copy(), previousTypeVariable, constraints)

    def add(constraint : Type) : Unit = {
        constraints ::= constraint
    }

    private def simplifyConstraint(constraint : Type) : Option[Type] = constraint match {
        case FieldConstraint(record, label, t, optional) =>
            record match {
                case TRecord(fields) =>
                    fields.find(_.name == label).map { field =>
                        try {
                            unification.unify(t, instantiate(Some(field.scheme)))
                        } catch {
                            case e : RuntimeException =>
                                throw new RuntimeException(e.getMessage + " (when checking ." + label + ")")
                        }
                        None
                    }.getOrElse {
                        if(optional) None else throw new RuntimeException(
                            "Field ." + label + " not found in {" + fields.map(_.name).mkString(", ") + "}"
                        )
                    }
                case TConstructor("Json") =>
                    unification.unify(t, TConstructor("Json"))
                    None
                case TParameter(_) =>
                    Some(constraint)
                case TVariable(_) =>
                    Some(constraint)
                case _ =>
                    throw new RuntimeException("Non-record field access: " + record + "." + label)
            }
        case TApply(TApply(TConstructor("=="), a), b) =>
            unification.unify(a, b)
            None
        case TApply(TConstructor(c), target) if c == "Add" || c == "Equal" || c == "Order" =>
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
                    throw new RuntimeException("Not satisfiable: " + constraint)
            }
        case _ =>
            throw new RuntimeException("Invalid constraint: " + constraint)
    }

    @tailrec
    private def simplifyConstraints(constraints : List[Type]) : List[Type] = {
        val expandedConstraints = constraints.map(unification.expand).distinct
        val seen = mutable.Map[(Type, String), Type]()
        val newConstraints = expandedConstraints.flatMap(simplifyConstraint).flatMap {
            case c@FieldConstraint(record, label, t, _) =>
                seen.get((record, label)).map { t0 =>
                    try {
                        unification.unify(t0, t)
                    } catch {
                        case e : RuntimeException =>
                            throw new RuntimeException(
                                e.getMessage +
                                " (if ." + label + " should be polymorphic, please add a type annotation)"
                            )
                    }
                    List()
                }.getOrElse {
                    seen.put((record, label), t)
                    List(c)
                }
            case c =>
                List(c)
        }
        if(newConstraints != constraints) {
            simplifyConstraints(newConstraints)
        } else {
            newConstraints
        }
    }

    def generalize(theType : Type, nonFree : Set[Int], topLevel : Boolean) : Scheme = {
        constraints = simplifyConstraints(constraints)
        val reversed = constraints.reverse
        val t = unification.expand(theType)
        var free = Pretty.determinedInType(t, false).map(_.toInt).filterNot(nonFree)
        @tailrec
        def findConstraints(found : List[Type]) : List[Type] = {
            val cs1 = reversed.filter(Pretty.freeInType(_).exists(free.contains))
            val cs2 = (cs1 ++ found).distinct
            if(cs2 != found) {
                val determined = cs2.flatMap(Pretty.determinedInConstraint(_, false)).map(_.toInt)
                free = (free ++ determined).filterNot(nonFree).distinct
                findConstraints(cs2)
            } else {
                found
            }
        }
        val cs1 = findConstraints(List())
        val cs2 = if(topLevel) constraints.reverse else cs1
        if(topLevel) free = (free ++ Pretty.freeInType(t) ++ cs2.flatMap(Pretty.freeInType)).distinct
        constraints = constraints.filterNot(cs2.contains)
        val replacementList = free.map(id => TVariable(id) -> TParameter("$" + id))
        val replacement = replacementList.toMap[Type, Type]
        val cs3 = cs2.map(unification.replace(_, replacement))
        val generalized = unification.replace(t, replacement)
        val parameters = replacementList.map { case (_, p) => TypeParameter(p.name, KStar()) } // Kind
        val scheme1 = Scheme(parameters, cs3, generalized)
        val scheme2 = Pretty.renameParameterNames(scheme1, unification.sub.get)
        checkAmbiguousScheme(scheme2)
        scheme2
    }

    def instantiate(scheme : Option[Scheme]) : Type = scheme.map { s =>
        val (t, cs) = internalInstantiate(s)
        constraints = cs ++ constraints
        t
    }.getOrElse(freshTypeVariable())

    private def internalInstantiate(scheme : Scheme) : (Type, List[Type]) = {
        val replacement = scheme.parameters.map(p => TParameter(p.name) -> freshTypeVariable()).toMap[Type, Type]
        val t = unification.replace(scheme.generalized, replacement) // Kind
        t -> scheme.constraints.map(unification.replace(_, replacement))
    }

    def checkTypeAnnotation(at : Location, annotation : Scheme, actual : Scheme) : Unit = {
        if(actual.parameters.length > annotation.parameters.length) {
            throw ParseException(at, "Type annotation is missing type parameter(s) vs. " + actual)
        }
        val temporary = copy()
        val (t, cs) = temporary.internalInstantiate(actual)
        try {
            temporary.unification.unify(annotation.generalized, t)
        } catch {
            case e : RuntimeException =>
                throw ParseException(at, "Type annotation differs vs. " + actual + " (" + e.getMessage + ")")
        }
        val unsatisfied = cs.find { c1 =>
            !annotation.constraints.exists { c2 =>
                try {
                    val u = temporary.unification.copy()
                    u.unify(c2, c1)
                    true
                } catch {
                    case _ : RuntimeException =>
                        false
                }
            }
        }
        unsatisfied.foreach { c =>
            throw ParseException(at, "Type annotation lacks " + unification.expand(c))
        }
    }

    def checkAmbiguousScheme(annotation : Scheme) : Unit = {
        val determined = Pretty.determinedInScheme(annotation, true).toSet
        annotation.parameters.find(p => !determined(p.name)).foreach { p =>
            println(Pretty.showScheme(annotation, true))
            throw new RuntimeException("Ambiguous " + p.name + " in: " + annotation)
        }
        checkAmbiguousSchemesInType(annotation.generalized)
    }

    private def checkAmbiguousSchemesInType(theType : Type) : Unit = theType match {
        case TVariable(id) =>
        case TParameter(name) =>
        case TConstructor(name) =>
        case TApply(constructor, argument) =>
            checkAmbiguousSchemesInType(constructor)
            checkAmbiguousSchemesInType(argument)
        case TSymbol(name) =>
        case TRecord(fields) =>
            fields.foreach(f => checkAmbiguousScheme(f.scheme))
    }

    def checkRemains() : Unit = {
        constraints.foreach(c => println("Unsatisfied: " + c))
    }

}
