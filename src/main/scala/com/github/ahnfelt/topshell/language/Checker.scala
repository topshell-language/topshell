package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._
import com.github.ahnfelt.topshell.language.Tokenizer.ParseException

object Checker {

    private val globals = List("tag", "visual", "true", "false", "null")

    def check(imports : List[TopImport], symbols : List[TopSymbol]) : List[TopSymbol] = {
        for((s, i) <- symbols.zipWithIndex) yield {
            val visible =
                globals ++
                imports.map(_.name) ++
                symbols.take(i).map(_.binding.name) ++
                symbols.drop(i).takeWhile(s => !s.bind).map(_.binding.name)
            try {
                val dependencies = checkTerm(s.binding.value, visible.toSet)
                s.copy(dependencies = dependencies.toList.sorted)
            } catch { case e : ParseException =>
                s.copy(error = Some(e))
            }
        }
    }

    private def checkTerm(term : Term, visible : Set[String]) : Set[String] = term match {
        case EString(at, value) =>
            Set.empty
        case ENumber(at, value) =>
            Set.empty
        case EVariable(at, name) =>
            if(!visible(name)) throw ParseException(at, "Unknown variable: " + name)
            Set(name)
        case EFunction(at, variable, body) =>
            checkTerm(body, visible + variable) - variable
        case EApply(at, function, argument) =>
            checkTerm(function, visible) ++
            checkTerm(argument, visible)
        case ELet(at, bindings, body) =>
            val newVisible = bindings.map(_.name)
            (for(s <- bindings) yield {
                checkTerm(s.value, visible ++ newVisible) -- newVisible
            }).toSet.flatten ++
            checkTerm(body, visible ++ newVisible) -- newVisible
        case EBind(at, binding, body) =>
            checkTerm(binding.value, visible) ++
            (checkTerm(body, visible + binding.name) - binding.name)
        case EList(at, elements, rest) =>
            (for(element <- elements) yield checkTerm(element, visible)).toSet.flatten ++
            (for(element <- rest) yield checkTerm(element, visible)).toSet.flatten
        case ERecord(at, fields, rest) =>
            (for(element <- fields.map(_.value)) yield checkTerm(element, visible)).toSet.flatten ++
            (for(element <- rest) yield checkTerm(element, visible)).toSet.flatten
        case EField(at, record, field) =>
            checkTerm(record, visible)
        case EIf(at, condition, thenBody, elseBody) =>
            checkTerm(condition, visible) ++
            checkTerm(thenBody, visible) ++
            checkTerm(elseBody, visible)
        case EUnary(at, operator, operand) =>
            checkTerm(operand, visible)
        case EBinary(at, operator, left, right) =>
            checkTerm(left, visible) ++
            checkTerm(right, visible)
    }

}
