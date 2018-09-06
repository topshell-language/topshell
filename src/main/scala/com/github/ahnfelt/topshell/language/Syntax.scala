package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Tokenizer.ParseException

object Syntax {

    case class Location(file : String, line : Int, column : Int) {
        override def toString = "in " + file + " " + toShortString
        def toShortString = "at line " + line + ", column " + column
    }

    case class Binding(at : Location, name : String, value : Term)

    case class TopSymbol(bind : Boolean, binding : Binding, dependencies : List[String], error : Option[ParseException])

    case class TopImport(at : Location, name : String, url : String, error : Option[ParseException])

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

    private val zeroLocation = Location("", 0, 0)

    def withoutLocation(b : Binding) : Binding = b.copy(at = zeroLocation, value = withoutLocation(b.value))

    def withoutLocation(t : Term) : Term = t match {
        case e : EString => e.copy(at = zeroLocation)
        case e : ENumber => e.copy(at = zeroLocation)
        case e : EVariable => e.copy(at = zeroLocation)
        case EFunction(_, variable, body) =>
            EFunction(zeroLocation, variable, withoutLocation(body))
        case EApply(_, function, argument) =>
            EApply(zeroLocation, withoutLocation(function), withoutLocation(argument))
        case ELet(_, bindings, body) =>
            ELet(zeroLocation, bindings.map(withoutLocation), withoutLocation(body))
        case EBind(_, binding, body) =>
            EBind(zeroLocation, withoutLocation(binding), withoutLocation(body))
        case EList(_, elements, rest) =>
            EList(zeroLocation, elements.map(withoutLocation), rest.map(withoutLocation))
        case ERecord(_, fields, rest) =>
            ERecord(zeroLocation, fields.map(withoutLocation), rest.map(withoutLocation))
        case EField(_, record, field) =>
            EField(zeroLocation, withoutLocation(record), field)
        case EIf(_, condition, thenBody, elseBody) =>
            EIf(zeroLocation, withoutLocation(condition), withoutLocation(thenBody), withoutLocation(elseBody))
        case EUnary(_, operator, operand) =>
            EUnary(zeroLocation, operator, withoutLocation(operand))
        case EBinary(_, operator, left, right) =>
            EBinary(zeroLocation, operator, withoutLocation(left), withoutLocation(right))
    }

    case class FatSymbol(s : TopSymbol, dependencies : Set[FatSymbol])

    def fatSymbol(s : TopSymbol, all : Map[String, TopSymbol]) : FatSymbol = {
        FatSymbol(s, s.dependencies.map(t => fatSymbol(all(t), all)).toSet)
    }
}
