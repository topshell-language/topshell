package com.github.ahnfelt.topshell.language

object Syntax {

    case class Location(file : String, line : Int, column : Int) {
        override def toString = "in " + file + " " + toShortString
        def toShortString = "at line " + line + ", column " + column
    }

    case class Binding(at : Location, name : String, value : Term)

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
    case class EField(at : Location, record : Term, field : String) extends Term
    case class EIf(at : Location, condition : Term, thenBody : Term, elseBody : Term) extends Term
    case class EUnary(at : Location, operator : String, operand : Term) extends Term
    case class EBinary(at : Location, operator : String, left : Term, right : Term) extends Term

}
