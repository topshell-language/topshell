package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

object UsedImports {

    def completeImports(topSymbols : List[TopSymbol], topImports : List[TopImport]) : List[TopImport] = {
        val used = topSymbols.map(_.binding.value).map(usedImports).foldLeft(Map.empty[String, Location])(_ ++ _)
        val missing = used -- topImports.map(_.name)
        val generated = missing.map { case (name, at) => TopImport(at, name, "/topshell/core/" + name + ".js", None) }
        topImports ++ generated
    }

    private def usedImports(term : Term) : Map[String, Location] = term match {
        case EString(at, value) => Map.empty
        case EInt(at, value) => Map.empty
        case EFloat(at, value) => Map.empty
        case EVariable(at, name) if name.headOption.exists(_.isUpper) =>
            Map(name -> at)
        case EVariable(at, name) =>
            Map.empty
        case EFunction(at, variable, body) =>
            usedImports(body)
        case EApply(at, function, argument) =>
            usedImports(function) ++
            usedImports(argument)
        case ELet(at, bindings, body) =>
            val list = for(b <- bindings) yield usedImports(b.value)
            list.foldLeft(usedImports(body))(_ ++ _)
        case EBind(at, binding, body) =>
            usedImports(binding.value) ++
            usedImports(body)
        case EList(at, elements, rest) =>
            val list = for(element <- elements) yield usedImports(element)
            list.foldLeft(rest.map(usedImports).getOrElse(Map.empty))(_ ++ _)
        case EVariant(at, name, arguments) =>
            arguments.map(usedImports).foldLeft(Map.empty[String, Location])(_ ++ _)
        case ERecord(at, fields, rest) =>
            val list = for(field <- fields.map(_.value)) yield usedImports(field)
            list.foldLeft(rest.map(usedImports).getOrElse(Map.empty))(_ ++ _)
        case EField(at, record, field, optional) =>
            usedImports(record)
        case EIf(at, condition, thenBody, elseBody) =>
            usedImports(condition) ++
            usedImports(thenBody) ++
            elseBody.map(usedImports).getOrElse(Map.empty)
        case EUnary(at, operator, operand) =>
            usedImports(operand)
        case EBinary(at, operator, left, right) =>
            usedImports(left) ++
            usedImports(right)
    }

}
