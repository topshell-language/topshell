package com.github.ahnfelt.topshell.language

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
    case class TRecord(fields : List[TypeBinding]) extends Type

    sealed abstract class Kind
    case class KStar() extends Kind
    case class KArrow(left : Kind, right : Kind) extends Kind

    case class TypeParameter(name : String, kind : Kind)

    case class Scheme(parameters : List[TypeParameter], constraints : List[Type], generalized : Type) {
        override def toString = Pretty.showScheme(this)
    }

    case class TypeBinding(name : String, scheme : Scheme)

}
