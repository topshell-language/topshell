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
    case class ENumber(at : Location, value : String) extends Term
    case class EVariable(at : Location, name : String) extends Term
    case class EFunction(at : Location, variable : String, body : Term) extends Term
    case class EApply(at : Location, function : Term, argument : Term) extends Term
    case class ELet(at : Location, bindings : List[Binding], body : Term) extends Term
    case class EBind(at : Location, binding : Binding, body : Term) extends Term
    case class EList(at : Location, elements : List[Term], rest : Option[Term]) extends Term
    case class ERecord(at : Location, fields : List[Binding], rest : Option[Term]) extends Term
    case class EField(at : Location, record : Term, field : String, optional : Boolean) extends Term
    case class EIf(at : Location, condition : Term, thenBody : Term, elseBody : Term) extends Term
    case class EUnary(at : Location, operator : String, operand : Term) extends Term
    case class EBinary(at : Location, operator : String, left : Term, right : Term) extends Term

    sealed abstract class Type { override def toString = Pretty.showType(this) }
    case class TVariable(id : Int) extends Type
    case class TParameter(name : String) extends Type
    case class TConstructor(name : String) extends Type
    case class TApply(constructor : Type, argument : Type) extends Type
    case class TSymbol(name : String) extends Type
    case class TRecord(fields : List[TypeBinding]) extends Type

    sealed abstract class Kind
    case class KStar() extends Kind
    case class KArrow(left : Kind, right : Kind) extends Kind

    case class TypeParameter(name : String, kind : Kind)

    case class Scheme(parameters : List[TypeParameter], constraints : List[Type], generalized : Type) {
        override def toString = Pretty.showScheme(this)
    }

    case class TypeBinding(name : String, scheme : Scheme)


    val binaryOperators = Seq(
        Seq("|")                    -> "a -> (a -> b) -> b",
        Seq("~>")                   -> "a -> b -> {key: a, value: b}",
        Seq("&&", "||")             -> "Bool -> Bool -> Bool",
        Seq(">", "<", ">=", "<=")   -> "a -> a -> Bool | Order a",
        Seq("==", "!=")             -> "a -> a -> Bool | Equal a",
        Seq("+")                    -> "a -> a -> a | Add a",
        Seq("-")                    -> "Number -> Number -> Number",
        Seq("*", "/")               -> "Number -> Number -> Number",
        Seq("^")                    -> "Number -> Number -> Number",
    )

    val binaryOperatorSymbols = binaryOperators.flatMap(_._1)

    lazy val binaryOperatorSchemes = binaryOperators.flatMap { case (o, t) =>
        val s = Parser.easy("Syntax.scala", t, _.parseScheme())
        o.map(_ -> s)
    }.toMap

}
