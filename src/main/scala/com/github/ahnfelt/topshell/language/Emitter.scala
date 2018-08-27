package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

import scala.scalajs.js.JSON

object Emitter {

    def emit(version : Double, topImports : List[TopImport], topSymbols : List[TopSymbol]) = {
        "var _h = _g.tsh;\n" +
        "var _n = {};\n" +
        topImports.map(emitImport).map("\n" + _ + "\n").mkString +
        topSymbols.map(emitTopSymbol).map("\n" + _ + "\n").mkString +
        "_g.tsh.setSymbols(_d, _n);\n"
    }

    def emitImport(topImport : TopImport) : String = {
        "_n." + topImport.name + "_ = {\n" +
        "kind: \"import\",\n" +
        "run: true,\n" +
        "dependencies: [],\n" +
        (topImport.error match {
            case Some(value) =>
                "error: " + JSON.stringify(value.message) + "\n"
            case None =>
                "compute: function(_s) { return _h.loadImport(" + JSON.stringify(topImport.url) + "); }\n"
        }) +
        "};\n"
    }

    def emitTopSymbol(symbol : TopSymbol) : String = {
        "_n." + symbol.binding.name + "_ = {\n" +
        "run: " + symbol.bind + ",\n" +
        "dependencies: [" + symbol.dependencies.map("\"" + _ + "_\"").mkString(", ") + "],\n" +
        (symbol.error match {
            case Some(value) =>
                "error: " + JSON.stringify(value.message) + "\n"
            case None =>
                "compute: function(_s) {\n" +
                symbol.dependencies.map(d => "var " + d + "_ = _s." + d + "_.result;\n").mkString("") +
                emitBody(symbol.binding.value) +
                "}\n"
        }) +
        "};\n"
    }

    def emitBody(term : Term) : String = {
        term match {
            case ELet(at, bindings, body) =>
                val bindingCode = for(b <- bindings) yield "var " + b.name + "_ = " + emitTerm(b.value) + ";\n"
                bindingCode.mkString + "return " + emitTerm(body) + ";\n"
            case _ =>
                "return " + emitTerm(term) + ";\n"
        }
    }

    def emitTerm(term : Term) : String = term match {
        case EString(at, value) => value
        case ENumber(at, value) => value
        case EVariable(at, name) => name + "_"
        case EFunction(at, variable, body) => "(function(" + variable + "_) {\n" + emitBody(body) + "})"
        case EApply(at, function, argument) => "(" + emitTerm(function) + ")(" + emitTerm(argument) + ")"
        case ELet(at, bindings, body) => "(function() {\n" + emitBody(term) + "})()"
        case EBind(at, binding, body) =>
            "_h.then(" + emitTerm(binding.value) + ", function(" + binding.name + "_) {\n" + emitBody(body) + "})\n"
        case EList(at, elements, rest) =>
            val list = "[" + elements.map(emitTerm).mkString(", ") + "]"
            rest.map(r => "(" + list + ".concat(" + emitTerm(r) + "))").getOrElse(list)
        case ERecord(at, fields, rest) =>
            val record = "{" + fields.map(b => b.name + ": " + emitTerm(b.value)).mkString(", ") + "}"
            rest.map(r => "_h.record(" + record + ", " + emitTerm(r) + ")").getOrElse(record)
        case EField(at, record, field) =>
            emitTerm(record) + "." + field
        case EIf(at, condition, thenBody, elseBody) =>
            "(" + emitTerm(condition) + " ? " + emitTerm(thenBody) + " : " + emitTerm(elseBody) + ")"
        case EUnary(at, operator, operand) =>
            "(" + operator + "(" + emitTerm(operand) + "))"
        case EBinary(at, "^", left, right) =>
            "Math.pow(" + emitTerm(left) + ", " + emitTerm(right) + ")"
        case EBinary(at, "|", left, right) =>
            "((" + emitTerm(right) + ")(" + emitTerm(left) + "))"
        case EBinary(at, operator, left, right) =>
            "((" + emitTerm(left) + ") " + operator + " (" + emitTerm(right) + "))"
    }

}
