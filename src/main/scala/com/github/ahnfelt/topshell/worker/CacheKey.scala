package com.github.ahnfelt.topshell.worker

import com.github.ahnfelt.topshell.language.Syntax._

case class CacheKey private (s : TopBlock, dependencies : Set[CacheKey])

object CacheKey {

    def cacheKey(symbol : TopBlock, symbols : Map[String, TopBlock], seen : Set[String]) : CacheKey = {
        val newSeen = seen + symbol.name
        CacheKey(withoutLocation(symbol), symbol.dependencies.filterNot(newSeen).map { s =>
            cacheKey(symbols(s), symbols, newSeen)
        }.toSet)
    }

    private val zeroLocation = Location("", 0, 0)

    private def withoutLocation(topBlock: TopBlock) : TopBlock = topBlock match {
        case s : TopSymbol => s.copy(binding = withoutLocation(s.binding))
        case i : TopImport => i.copy(at = zeroLocation)
    }

    private def withoutLocation(b : Binding) : Binding = b.copy(at = zeroLocation, value = withoutLocation(b.value))

    private def withoutLocation(t : Term) : Term = t match {
        case e : EString => e.copy(at = zeroLocation)
        case e : EInt => e.copy(at = zeroLocation)
        case e : EFloat => e.copy(at = zeroLocation)
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
        case EVariant(_, name, argument) =>
            EVariant(zeroLocation, name, argument.map(withoutLocation))
        case ERecord(_, fields, rest) =>
            ERecord(zeroLocation, fields.map(withoutLocation), rest.map(withoutLocation))
        case EField(_, record, field, optional) =>
            EField(zeroLocation, withoutLocation(record), field, optional)
        case EIf(_, condition, thenBody, elseBody) =>
            EIf(zeroLocation, withoutLocation(condition), withoutLocation(thenBody), elseBody.map(withoutLocation))
        case EUnary(_, operator, operand) =>
            EUnary(zeroLocation, operator, withoutLocation(operand))
        case EBinary(_, operator, left, right) =>
            EBinary(zeroLocation, operator, withoutLocation(left), withoutLocation(right))
    }

}
