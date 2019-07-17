package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

import scala.scalajs.js.JSON

object Pretty {

    def showType(typeToShow : Type) : String = typeToShow match {
        case TVariable(id) => "_" + id
        case TParameter(name) => name
        case TConstructor(name) => name
        case TSymbol(name) => JSON.stringify(name)
        case TVariant(variants) =>
            "[" + variants.map { case (n, t1) => n + t1.map(t2 => " " + showTypeEnclosed(t2)).mkString }.mkString(", ") + "]"
        case VariantConstraint(label, variantType) =>
            label + " : " + variantType
        case StructureConstraint(p, s1, c1, s2, c2, cs) if s1 == s2 && c1 == c2 =>
            val x = cs.map(" | " + showType(_)).mkString
            "{" + showStructureConstraintPart(p, s1, c1) + x + "}"
        case StructureConstraint(p, s1, c1, s2, c2, cs) =>
            val x = cs.map(" | " + showType(_)).mkString
            "{" + showStructureConstraintPart(p, s1, c1) + ", " + showStructureConstraintPart(p, s2, c2) + x + "}"
        case RecordConstraint(t, required, optional) =>
            val requiredFields = required.map(f => f.name + ": " + f.scheme.generalized)
            val optionalFields = optional.map(f => "?" + f.name + ": " + f.scheme.generalized)
            t + " ~ {" + (requiredFields ++ optionalFields).mkString(", ") + "}"
        case TRecord(fields) =>
            "{" + fields.map(b => b.name + ": " + showScheme(b.scheme, true)).mkString(", ") + "}"
        case TApply(TApply(TApply(TConstructor(o), TSymbol(l)), t1), t2) if o == "." || o == ".?" =>
            t2 + o + l + ": " + t1 // Escape label
        case TApply(TApply(TConstructor("=="), a), b) => a + " == " + b
        case TApply(TApply(TConstructor("->"), a@TApply(TApply(TConstructor("->"), _), _)), b) => "(" + a + ") -> " + b
        case TApply(TApply(TConstructor("->"), a), b) => a + " -> " + b
        case TApply(constructor, argument : TApply) => constructor + " (" + argument + ")"
        case TApply(constructor, argument) => constructor + " " + argument
    }

    def showStructureConstraintPart(parameter : String, structure : Type, constructor : Option[Type]) = {
        showType(structure) + " : " + constructor.map(showType(_) + " ").getOrElse("") + parameter
    }

    def showTypeEnclosed(t : Type) : String = t match {
        case TApply(_, _) => "(" + showType(t) + ")"
        case _ => showType(t)
    }

    def showScheme(scheme : Scheme, explicit : Boolean) = {
        (if(explicit) scheme.parameters.map(_.name + " => ").mkString else "") +
        scheme.generalized + scheme.constraints.map(" | " + _).mkString
    }

    val alphabet = Stream.from(0).flatMap(i => Stream.range('a', ('z' + 1).toChar).map(_ -> i)).map {
        case (c, 0) => c.toString
        case (c, i) => c + i.toString
    }

    def renameParameterNames(scheme : Scheme, expand : Int => Option[Type]) = {
        val used = usedParameterNamesInScheme(scheme, expand)
        val alpha = alphabet.filterNot(used)
        val pairs = scheme.parameters.map(_.name).zip(alpha).map { case (k, v) => TParameter(k) -> TParameter(v) }
        val replacement = pairs.toMap[Type, Type]
        val constraints = scheme.constraints.map(replace(_, replacement, expand))
        val generalized = replace(scheme.generalized, replacement, expand)
        val names = pairs.map(_._2.name)
        val kinds = scheme.parameters.map(_.kind)
        Scheme(names.zip(kinds).map(TypeParameter.tupled), constraints, generalized)
    }

    def usedParameterNames(search : Type, expand : Int => Option[Type]) : Set[String] = search match {
        case TVariable(id) => expand(id).map(usedParameterNames(_, expand)).getOrElse(Set.empty)
        case TParameter(name) => Set(name)
        case TConstructor(name) => Set.empty
        case TSymbol(name) => Set.empty
        case TVariant(variants) =>
            variants.map { case (_, t) => t.map(usedParameterNames(_, expand)).fold(Set.empty)(_ ++ _) }.
                fold(Set.empty)(_ ++ _)
        case TApply(constructor, argument) =>
            usedParameterNames(constructor, expand) ++ usedParameterNames(argument, expand)
        case TRecord(fields) =>
            fields.map(f => usedParameterNamesInScheme(f.scheme, expand)).fold(Set.empty)(_ ++ _)
    }

    def usedParameterNamesInScheme(scheme : Scheme, expand : Int => Option[Type]) : Set[String] = {
        scheme.parameters.map(_.name).toSet ++
        usedParameterNames(scheme.generalized, expand) ++
        scheme.constraints.map(usedParameterNames(_, expand)).fold(Set.empty)(_ ++ _)
    }

    def freeParameterNames(search : Type, expand : Int => Option[Type]) : Set[String] = search match {
        case TVariable(id) => expand(id).map(freeParameterNames(_, expand)).getOrElse(Set.empty)
        case TParameter(name) => Set(name)
        case TConstructor(name) => Set.empty
        case TSymbol(name) => Set.empty
        case TApply(constructor, argument) =>
            freeParameterNames(constructor, expand) ++ freeParameterNames(argument, expand)
        case TVariant(variants) =>
            variants.map { case (_, ts) => ts.map(freeParameterNames(_, expand)).fold(Set.empty)(_ ++ _) }.
                fold(Set.empty)(_ ++ _)
        case TRecord(fields) =>
            fields.map(f => freeParameterNamesInScheme(f.scheme, expand)).fold(Set.empty)(_ ++ _)
    }

    def freeParameterNamesInScheme(scheme : Scheme, expand : Int => Option[Type]) : Set[String] = {
        (freeParameterNames(scheme.generalized, expand) :: scheme.constraints.map(freeParameterNames(_, expand))).
            fold(Set.empty)(_ ++ _) -- scheme.parameters.map(_.name).toSet
    }

    def replace(search : Type, replacement : Map[Type, Type], expand : Int => Option[Type]) : Type = search match {
        case t if replacement.contains(t) =>
            replacement(t)
        case TVariable(id) =>
            expand(id).map(replace(_, replacement, expand)).getOrElse(search)
        case TParameter(name) =>
            search
        case TConstructor(name) =>
            search
        case TSymbol(name) =>
            search
        case TApply(constructor, argument) =>
            TApply(replace(constructor, replacement, expand), replace(argument, replacement, expand))
        case TVariant(variants) =>
            TVariant(variants.map { case (n, t) => n -> t.map(replace(_, replacement, expand)) })
        case TRecord(fields) =>
            TRecord(fields.map { f => f.copy(scheme = replaceInScheme(f.scheme, replacement, expand)) })
    }

    def replaceInScheme(search : Scheme, replacement : Map[Type, Type], expand : Int => Option[Type]) : Scheme = {
        search.copy(
            constraints = search.constraints.map(replace(_, replacement, expand)),
            generalized = replace(search.generalized, replacement, expand)
        )
    }

    def freeInScheme(theScheme : Scheme) : List[Int] = {
        theScheme.constraints.flatMap(freeInType) ++ freeInType(theScheme.generalized)
    }

    def freeInType(theType : Type) : List[Int] = (theType match {
        case TVariable(id) => List(id)
        case TParameter(name) => List()
        case TConstructor(name) => List()
        case TSymbol(name) => List()
        case TApply(constructor, argument) => freeInType(constructor) ++ freeInType(argument)
        case TVariant(variants) => variants.flatMap { case (_, ts) => ts.flatMap(freeInType) }
        case TRecord(fields) => fields.flatMap(f => freeInScheme(f.scheme))
    }).distinct

    // Type variables that are *fully determined* (in the type class with functional dependencies sense)
    // To avoid inferring: f : a | b.y: a = (Json.toAny (Json.read "{}")).y
    // But still infer: g : a -> b | a.y: c | c.z: b = x -> x.y.z
    def determinedInConstraint(constraint : Type, parameters : Boolean) : List[String] = constraint match {
        case FieldConstraint(_, _, t, _) => determinedInType(t, parameters)
        case VariantConstraint(_, t) => VariantConstraint.fieldTypes(t).flatMap(determinedInType(_, parameters))
        // case StructureConstraint ?
        case _ => List()
    }

    def determinedInType(theType : Type, parameters : Boolean) : List[String] = (theType match {
        case TVariable(id) => if(parameters) List() else List(id.toString)
        case TParameter(name) => if(parameters) List(name) else List()
        case TConstructor(name) => List()
        case TApply(constructor, argument) =>
            determinedInType(constructor, parameters) ++ determinedInType(argument, parameters)
        case TSymbol(name) => List()
        case TVariant(variants) => variants.flatMap { case (_, t) => t.toList.flatMap(determinedInType(_, parameters)) }
        case TRecord(fields) => fields.flatMap(f => determinedInScheme(f.scheme, parameters))
    }).distinct

    def determinedInScheme(scheme : Scheme, parameters : Boolean) : List[String] = {
        determinedInType(scheme.generalized, parameters) ++
        scheme.constraints.flatMap(determinedInConstraint(_, parameters))
    }.distinct

}
