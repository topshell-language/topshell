package com.github.ahnfelt.topshell.language

import scala.scalajs.js

object Syntax {

    case class Location(file : String, line : Int, column : Int) {
        override def toString = "in " + file + " " + toShortString
        def toShortString = "at line " + line + ", column " + column
    }

    case class Binding(at : Location, name : String, scheme : Option[Scheme], value : Term)

    sealed abstract class TopBlock {
        def name : String
        def dependencies : List[String]
    }

    case class TopSymbol(bind : Boolean, binding : Binding, dependencies : List[String], error : Option[ParseException]) extends TopBlock {
        override def name = binding.name
    }

    case class TopImport(at : Location, name : String, url : String, error : Option[ParseException]) extends TopBlock {
        override def dependencies = List()
    }

    sealed abstract class Term { val at : Location }
    case class EString(at : Location, value : String) extends Term
    case class EInt(at : Location, value : String) extends Term
    case class EFloat(at : Location, value : String) extends Term
    case class EVariable(at : Location, name : String) extends Term
    case class EFunction(at : Location, variable : String, body : Term) extends Term
    case class EApply(at : Location, function : Term, argument : Term) extends Term
    case class ELet(at : Location, bindings : List[Binding], body : Term) extends Term
    case class EBind(at : Location, binding : Binding, body : Term) extends Term
    case class EList(at : Location, elements : List[Term], rest : Option[Term]) extends Term
    case class EVariant(at : Location, name : String, arguments : List[Term]) extends Term
    case class ERecord(at : Location, fields : List[Binding], rest : Option[Term]) extends Term
    case class EField(at : Location, record : Term, field : String, optional : Boolean) extends Term
    case class EIf(at : Location, condition : Term, thenBody : Term, elseBody : Option[Term]) extends Term
    case class EUnary(at : Location, operator : String, operand : Term) extends Term
    case class EBinary(at : Location, operator : String, left : Term, right : Term) extends Term
    case class EMatch(at : Location, cases : List[VariantCase], defaultCase : Option[DefaultCase]) extends Term

    case class VariantCase(at : Location, variant : String, arguments : List[Option[String]], body : Term)
    case class DefaultCase(at : Location, variable : Option[String], body : Term)

    sealed abstract class Type { override def toString = Pretty.showType(this) }
    case class TVariable(id : Int) extends Type
    case class TParameter(name : String) extends Type
    case class TConstructor(name : String) extends Type
    case class TApply(constructor : Type, argument : Type) extends Type
    case class TSymbol(name : String) extends Type
    case class TVariant(variants : List[(String, List[Type])]) extends Type
    case class TRecord(fields : List[TypeBinding]) extends Type

    sealed abstract class Kind
    case class KStar() extends Kind
    case class KArrow(left : Kind, right : Kind) extends Kind

    case class TypeParameter(name : String, kind : Kind)

    case class Scheme(parameters : List[TypeParameter], constraints : List[Type], generalized : Type) {
        override def toString = Pretty.showScheme(this, false)
    }

    case class TypeBinding(name : String, scheme : Scheme)

    object FieldConstraint {

        def apply(recordType : Type, label : String, fieldType : Type, optional : Boolean) =
            TApply(TApply(TApply(TConstructor(if(optional) ".?" else "."), TSymbol(label)), fieldType), recordType)

        def unapply(constraint : Type) = constraint match {
            case TApply(TApply(TApply(TConstructor(o), TSymbol(label)), fieldType), recordType) if o == "." || o == ".?" =>
                Some((recordType, label, fieldType, o == ".?"))
            case _ =>
                None
        }

    }

    object VariantConstraint {

        def apply(variantType : Type, label : String, fieldTypes : List[Type]) = {
            val applied = fieldTypes.foldLeft[Type](TSymbol(label))(TApply)
            TApply(TApply(TConstructor("#"), applied), variantType)
        }

        def unapply(constraint : Type) = constraint match {
            case TApply(TApply(TConstructor("#"), applied), variantType) =>
                def extract(a : Type, arguments : List[Type]) : Option[(String, List[Type])] = a match {
                    case TApply(x, y) => extract(x, y :: arguments)
                    case TSymbol(l) => Some(l -> arguments)
                    case _ => None
                }
                extract(applied, List()).map { case (label, ts) =>
                    (variantType, label, ts)
                }
            case _ =>
                None
        }

    }

    val unaryOperators = Seq(
        Seq("-")                    -> "a -> a | Number a",
        Seq("!")                    -> "Bool -> Bool",
    )

    val binaryOperators = Seq(
        Seq("<|")                   -> "(a -> b) -> a -> b",
        Seq("|>")                   -> "a -> (a -> b) -> b",
        Seq("~>")                   -> "a -> b -> {key: a, value: b}",
        Seq("&&", "||")             -> "Bool -> Bool -> Bool",
        Seq(">", "<", ">=", "<=")   -> "a -> a -> Bool | Order a",
        Seq("==", "!=")             -> "a -> a -> Bool | Equal a",
        Seq("++")                   -> "List a -> List a -> List a",
        Seq("+")                    -> "a -> a -> a | Add a",
        Seq("-")                    -> "a -> a -> a | Number a",
        Seq("*")                    -> "a -> a -> a | Number a",
        Seq("/")                    -> "a -> b -> Float | Number a | Number b",
        Seq("^")                    -> "a -> a -> a | Number a",
    )

    val unaryOperatorSymbols = unaryOperators.flatMap(_._1)

    lazy val unaryOperatorSchemes = unaryOperators.flatMap { case (o, t) =>
        val s = Parser.easy("Syntax.scala", t, _.parseScheme(false))
        o.map(_ -> s)
    }.toMap

    val binaryOperatorSymbols = binaryOperators.flatMap(_._1)

    lazy val binaryOperatorSchemes = binaryOperators.flatMap { case (o, t) =>
        val s = Parser.easy("Syntax.scala", t, _.parseScheme(false))
        o.map(_ -> s)
    }.toMap

}
