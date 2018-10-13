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

    def generalize(theType : Type, nonFree : Set[Int]) : Scheme = {
        constraints = simplifyConstraints(constraints)
        val t = unification.expand(theType)
        var free = Pretty.freeInType(t).filterNot(nonFree)
        @tailrec
        def findConstraints(found : List[Type]) : List[Type] = {
            val c1 = constraints.filter(Pretty.freeInType(_).exists(free.contains))
            val c2 = (found ++ c1).distinct
            if(c2 != found) {
                free = (free ++ c2.flatMap(Pretty.freeInType).filterNot(nonFree)).distinct
                findConstraints(c2)
            } else {
                found
            }
        }
        val cs1 = findConstraints(List())
        constraints = constraints.filterNot(cs1.contains)
        val replacementList = free.map(id => TVariable(id) -> TParameter("$" + id))
        val replacement = replacementList.toMap[Type, Type]
        val cs2 = cs1.map(unification.replace(_, replacement))
        val generalized = unification.replace(t, replacement)
        val parameters = replacementList.map { case (_, p) => TypeParameter(p.name, KStar()) } // Kind
        val scheme = Scheme(parameters, cs2, generalized)
        Pretty.renameParameterNames(scheme, unification.sub.get)
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
        println(cs)
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
            throw ParseException(at, "Type annotation doesn't guarantee " + unification.expand(c))
        }
    }

    def checkRemains() : Unit = {
        constraints.foreach(c => println("Unsatisfied: " + c))
    }

}
