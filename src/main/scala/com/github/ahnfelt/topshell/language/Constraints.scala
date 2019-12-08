package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._
import com.github.ahnfelt.topshell.worker.Timer

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

    def reset() : Unit = {
        unification.reset()
        previousTypeVariable = initialTypeVariable
        constraints = initialConstraints
    }

    private def simplifyConstraint(constraint : Type) : List[Type] = constraint match {
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
                        List()
                    }.getOrElse {
                        if(optional) List() else throw new RuntimeException(
                            "Field ." + label + " not found in {" + fields.map(_.name).mkString(", ") + "}"
                        )
                    }
                case TConstructor("Json") =>
                    unification.unify(t, TConstructor("Json"))
                    List()
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Non-record field access: " + record + "." + label)
            }
        case VariantConstraint(name, constructorType) =>
            VariantConstraint.resultType(constructorType) match {
                case TVariant(variants) =>
                    variants.find(_._1 == name).map { case (n, t) =>
                        checkVariantArgumentConstraint(VariantConstraint.fieldTypes(constructorType), t, constraint)
                        List()
                    }.getOrElse {
                        throw new RuntimeException(
                            "Variant " + name + " not found in " + constructorType
                        )
                    }
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Non-variant: " + constraint)
            }
        case StructureConstraint(p, s1, c1, s2, c2, cs) =>
            def checkStructure(s1 : Type, c1 : Option[Type], s2 : Type, c2 : Option[Type]) : Option[List[Type]] = {
                s1 match {
                    case TRecord(fields) =>
                        val (newCs, newFields) = fields.map { f =>
                            if(f.scheme.parameters.nonEmpty) throw new RuntimeException("Not a simple record: " + s1)
                            if(f.scheme.constraints.nonEmpty) throw new RuntimeException("Not a simple record: " + s1)
                            val t = freshTypeVariable()
                            unification.unify(c1.map(TApply(_, t)).getOrElse(t), f.scheme.generalized)
                            val replacement = Map[Type, Type](TParameter(p) -> unification.expand(t))
                            cs.map(unification.expand).map(unification.replace(_, replacement)) ->
                            TypeBinding(f.name, Scheme(List(), List(), c2.map(TApply(_, t)).getOrElse(t)))
                        }.unzip
                        unification.unify(TRecord(newFields), s2)
                        Some(newCs.flatten.map(unification.expand))
                    case TApply(TConstructor("List"), elementType) =>
                        val t = freshTypeVariable()
                        unification.unify(c1.map(TApply(_, t)).getOrElse(t), elementType)
                        unification.unify(TApply(TConstructor("List"), c2.map(TApply(_, t)).getOrElse(t)), s2)
                        val replacement = Map[Type, Type](TParameter(p) -> unification.expand(t))
                        Some(cs.map(unification.expand).map(unification.replace(_, replacement)))
                    case TParameter(_) => None
                    case TVariable(_) => None
                    case _ => throw new RuntimeException("Not a record or list: " + s1)
                }
            }
            checkStructure(s1, c1, s2, c2).orElse(checkStructure(s2, c2, s1, c1)).getOrElse(List(constraint))
        case RecordConstraint(record, required, List()) =>
            unification.unify(record, TRecord(required))
            List()
        case RecordConstraint(record, required, optional) =>
            record match {
                case TRecord(fields) =>
                    var missing = required.map(_.name).toSet
                    val allowed = (required ++ optional).map(field => field.name -> field.scheme.generalized).toMap
                    for(field <- fields) {
                        missing -= field.name
                        allowed.get(field.name) match {
                            case None => throw new RuntimeException("Unexpected field: " + field.name)
                            case Some(t) => unification.unify(t, field.scheme.generalized)
                        }
                    }
                    if(missing.nonEmpty) {
                        throw new RuntimeException("Missing fields: " + missing.toList.sorted.mkString(", "))
                    }
                    List()
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Record constraint on non-record: " + constraint)
            }
        case TApply(TApply(TConstructor("=="), a), b) =>
            unification.unify(a, b)
            List()
        case TApply(TConstructor(c), target) if c == "Monad" =>
            target match {
                case TConstructor("List") =>
                    List()
                case TConstructor("Task") =>
                    List()
                case TConstructor("Stream") =>
                    List()
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Not satisfiable: " + constraint)
            }
        case TApply(TConstructor(c), target) if c == "Add" =>
            target match {
                case TConstructor("Int") =>
                    List()
                case TConstructor("Float") =>
                    List()
                case TConstructor("String") =>
                    List()
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Not satisfiable: " + constraint)
            }
        case TApply(TConstructor(c), target) if c == "Equal" =>
            target match {
                case TConstructor("Int") =>
                    List()
                case TConstructor("Float") =>
                    List()
                case TConstructor("String") =>
                    List()
                case TConstructor("Bool") =>
                    List()
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Not satisfiable: " + constraint)
            }
        case TApply(TConstructor(c), target) if c == "Order" =>
            target match {
                case TConstructor("Int") =>
                    List()
                case TConstructor("Float") =>
                    List()
                case TConstructor("String") =>
                    List()
                case TConstructor("Bool") =>
                    List()
                case TApply(TConstructor("List"), t) =>
                    List(TApply(TConstructor(c), t))
                case TRecord(fields) =>
                    fields.map {
                        case TypeBinding(_, Scheme(List(), List(), t)) =>
                            TApply(TConstructor(c), t)
                        case _ =>
                            throw new RuntimeException("Not satisfiable: " + constraint)
                    }
                case TVariant(variants) =>
                    variants.flatMap { case (_, ts) =>
                        ts.map(t => TApply(TConstructor(c), t))
                    }
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Not satisfiable: " + constraint)
            }
        case TApply(TConstructor(c), target) if c == "Number" =>
            target match {
                case TConstructor("Int") =>
                    List()
                case TConstructor("Float") =>
                    List()
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Not satisfiable: " + constraint)
            }
        case TApply(TConstructor(c), target) if c == "Display" =>
            target match {
                case TConstructor("Int") =>
                    List()
                case TConstructor("Float") =>
                    List()
                case TConstructor("String") =>
                    List()
                case TConstructor("Bool") =>
                    List()
                case TParameter(_) =>
                    List(constraint)
                case TVariable(_) =>
                    List(constraint)
                case _ =>
                    throw new RuntimeException("Not satisfiable: " + constraint)
            }
        case _ =>
            throw new RuntimeException("Invalid constraint: " + constraint)
    }

    def checkVariantArgumentConstraint(t1s : List[Type], t2s : List[Type], constraint : Type) : Unit = {
        if(t1s.size < t2s.size) throw new RuntimeException("Too many parameters: " + constraint)
        if(t1s.size > t2s.size) throw new RuntimeException("Too few parameters: " + constraint)
        t1s.zip(t2s).foreach { case (t1, t2) => unification.unify(t1, t2) }
    }

    private def simplifyConstraints(constraints : List[Type]) : List[Type] =
        Timer.accumulate("simplifyConstraints") { doSimplifyConstraints(constraints) }

    @tailrec
    private def doSimplifyConstraints(constraints : List[Type]) : List[Type] = {
        val expandedConstraints = constraints.map(unification.expand).distinct
        val fieldConstraints = mutable.Map[(Type, String, Boolean), Type]()
        val variantConstraints = mutable.Map[(Type, String), List[Type]]()
        val simplifiedConstraints = expandedConstraints.flatMap(simplifyConstraint)
        val recordConstraints = simplifiedConstraints.collect { case RecordConstraint(t, r, o) => (t, r, o) }
        val newConstraints = simplifiedConstraints.flatMap {
            case FieldConstraint(record, label, fieldType, isOptional) if recordConstraints.exists(_._1 == record) =>
                val (_, required, optional) = recordConstraints.find(_._1 == record).get
                if(!isOptional && optional.map(_.name).contains(label)) {
                    throw new RuntimeException("Optional field accessed as if required: " + label)
                }
                val allowed = (required ++ optional).map(field => field.name -> field.scheme.generalized).toMap
                allowed.get(label) match {
                    case None => throw new RuntimeException("Unexpected field: " + label)
                    case Some(t) => unification.unify(fieldType, t)
                }
                List()
            case c@FieldConstraint(record, label, fieldType, isOptional) =>
                fieldConstraints.get((record, label, isOptional)).map { t0 =>
                    try {
                        unification.unify(t0, fieldType)
                    } catch {
                        case e : RuntimeException =>
                            throw new RuntimeException(
                                e.getMessage +
                                " (if ." + label + " should be polymorphic, please add a type annotation)"
                            )
                    }
                    List()
                }.getOrElse {
                    fieldConstraints.put((record, label, isOptional), fieldType)
                    List(c)
                }
            case c@VariantConstraint(label, constructorType) =>
                val variant = VariantConstraint.resultType(constructorType)
                variantConstraints.get((variant, label)).map { t =>
                    checkVariantArgumentConstraint(t, VariantConstraint.fieldTypes(constructorType), c)
                    List()
                }.getOrElse {
                    variantConstraints.put((variant, label), VariantConstraint.fieldTypes(constructorType))
                    List(c)
                }
            case c =>
                List(c)
        }
        if(newConstraints != constraints) {
            doSimplifyConstraints(newConstraints)
        } else {
            newConstraints
        }
    }

    def filterAmbiguousVariants(allConstraints : List[Type], determinedConstraints : List[Type]) : List[Type] = {
        val determined = determinedConstraints.collect {
            case VariantConstraint(_, t) => VariantConstraint.resultType(t) match {
                case TVariable(x) => Some(x)
                case _ => None
            }
        }.flatten.toSet
        allConstraints.filter {
            case VariantConstraint(_, t) => VariantConstraint.resultType(t) match {
                case TVariable(x) => determined(x)
                case _ => true
            }
            case _ => true
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
        val cs2 = if(topLevel) filterAmbiguousVariants(reversed, cs1) else cs1
        if(topLevel) free = (free ++ Pretty.freeInType(t) ++ cs2.flatMap(Pretty.freeInType)).distinct
        constraints = if(topLevel) List.empty else constraints.filterNot(cs2.contains)
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
        val expandedConstraints = annotation.constraints.flatMap {
            case c@RecordConstraint(recordType, required, optional) =>
                val r = required.map(field => FieldConstraint(recordType, field.name, field.scheme.generalized, false))
                val o = optional.map(field => FieldConstraint(recordType, field.name, field.scheme.generalized, true))
                c :: (r ++ o)
            case c => List(c)
        }
        val unsatisfied = cs.find { c1 =>
            !expandedConstraints.exists { c2 =>
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
        case TVariant(variants) =>
            variants.foreach { case (_, t) => t.foreach(checkAmbiguousSchemesInType) }
        case TRecord(fields) =>
            fields.foreach(f => checkAmbiguousScheme(f.scheme))
    }

    def assureEmpty() : Unit = {
        val oldConstraints = constraints
        constraints = List()
        oldConstraints.foreach(c =>
            throw new RuntimeException("Unsatisfied: " + c)
        )
    }

    def clearConstraints() : Unit = {
        constraints = List()
    }

}
