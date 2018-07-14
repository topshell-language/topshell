package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._
import com.github.ahnfelt.topshell.language.Tokenizer.ParseException

object Checker {

    private val globals = List("tag", "visual", "true", "false", "null")

    def check(symbols : List[TopSymbol]) : List[TopSymbol] = {
        for((s, i) <- symbols.zipWithIndex) yield {
            val visible =
                globals ++
                symbols.take(i).map(_.binding.name) ++
                symbols.drop(i).takeWhile(s => !s.bind && s.binding.value.isInstanceOf[EFunction]).map(_.binding.name)
            try {
                checkTerm(s.binding.value, visible.toSet)
                s
            } catch { case e : ParseException =>
                s.copy(error = Some(e))
            }
        }
    }

    private def checkTerm(term : Term, visible : Set[String]) : Unit = term match {
        case EString(at, value) =>
        case ENumber(at, value) =>
        case EVariable(at, name) =>
            if(!visible(name)) throw ParseException(at, "Unknown variable: " + name)
        case EFunction(at, variable, body) =>
            checkTerm(body, visible + variable)
        case EApply(at, function, argument) =>
            checkTerm(function, visible)
            checkTerm(argument, visible)
        case ELet(at, bindings, body) =>
            for((s, i) <- bindings.zipWithIndex) {
                val newVisible =
                    visible ++
                    bindings.take(i).map(_.name) ++
                    bindings.drop(i).takeWhile(_.value.isInstanceOf[EFunction]).map(_.name)
                checkTerm(s.value, newVisible)
            }
            checkTerm(body, visible ++ bindings.map(_.name))
        case EBind(at, binding, body) =>
            checkTerm(binding.value, visible)
            checkTerm(body, visible + binding.name)
        case EList(at, elements, rest) =>
            for(element <- elements) checkTerm(element, visible)
            for(element <- rest) checkTerm(element, visible)
        case ERecord(at, fields, rest) =>
            for(element <- fields.map(_.value)) checkTerm(element, visible)
            for(element <- rest) checkTerm(element, visible)
        case EField(at, record, field) =>
            checkTerm(record, visible)
        case EIf(at, condition, thenBody, elseBody) =>
            checkTerm(condition, visible)
            checkTerm(thenBody, visible)
            checkTerm(elseBody, visible)
        case EUnary(at, operator, operand) =>
            checkTerm(operand, visible)
        case EBinary(at, operator, left, right) =>
            checkTerm(left, visible)
            checkTerm(right, visible)
    }

}
