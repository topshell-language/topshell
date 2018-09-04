package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

import scala.scalajs.js.JSON

object Emitter {

    def emit(version : Double, topImports : List[TopImport], topSymbols : List[TopSymbol]) = {
        "var _h = _g.tsh;\n" +
        "var _n = {_blocks: []};\n" +
        topImports.map(emitImport).map("\n" + _ + "\n").mkString +
        topSymbols.map(emitTopSymbol).map("\n" + _ + "\n").mkString +
        "return _n;\n"
    }

    def emitImport(topImport : TopImport) : String = {
        "var " + topImport.name + "_;\n" +
        "_n." + topImport.name + "_ = {\n" +
        "name: " + JSON.stringify(topImport.name + "_") + ",\n" +
        "module: true,\n" +
        "effect: true,\n" +
        "fromLine: 0,\n" +
        "toLine: 0,\n" +
        "dependencies: [],\n" +
        (topImport.error match {
            case Some(value) =>
                "error: " + JSON.stringify(value.message) + ",\n"
            case None =>
                "compute: function(_s) { return _h.loadImport(" + JSON.stringify(topImport.url) + "); },\n"
        }) +
        "setResult: function(_s, _result) {\n" +
            topImport.name + "_ = _result;\n" +
            "_s." + topImport.name + "_.result = _result;\n" +
        "}\n" +
        "};\n" +
        "_n._blocks.push(_n." + topImport.name + "_);\n"
    }

    def emitTopSymbol(symbol : TopSymbol) : String = {
        "var " + symbol.binding.name + "_;\n" +
        "_n." + symbol.binding.name + "_ = {\n" +
        "name: " + JSON.stringify(symbol.binding.name + "_") + ",\n" +
        "module: false,\n" +
        "effect: " + symbol.bind + ",\n" +
        "fromLine: " + symbol.binding.at.line + ",\n" +
        "toLine: " + symbol.binding.at.line + ",\n" +
        "dependencies: [" + symbol.dependencies.map("\"" + _ + "_\"").mkString(", ") + "],\n" +
        (symbol.error match {
            case Some(value) =>
                "error: " + JSON.stringify(value.message) + ",\n"
            case None =>
                "compute: function(_s) {\n" +
                emitBody(symbol.binding.value) +
                "},\n"
        }) +
        "setResult: function(_s, _result) {\n" +
            symbol.binding.name + "_ = _result;\n" +
            "_s." + symbol.binding.name + "_.result = _result;\n" +
        "}\n" +
        "};\n" +
        "_n._blocks.push(_n." + symbol.binding.name + "_);\n"
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
            val record = "{" + fields.map(b => escapeField(b.name, None) + ": " + emitTerm(b.value)).mkString(", ") + "}"
            rest.map(r => "_h.record(" + record + ", " + emitTerm(r) + ")").getOrElse(record)
        case EField(at, record, field) =>
            escapeField(field, Some(record))
        case EIf(at, condition, thenBody, elseBody) =>
            "(" + emitTerm(condition) + " ? " + emitTerm(thenBody) + " : " + emitTerm(elseBody) + ")"
        case EUnary(at, operator, operand) =>
            "(" + operator + "(" + emitTerm(operand) + "))"
        case EBinary(at, "^", left, right) =>
            "Math.pow(" + emitTerm(left) + ", " + emitTerm(right) + ")"
        case EBinary(at, "|", left, right) =>
            "((" + emitTerm(right) + ")(" + emitTerm(left) + "))"
        case EBinary(at, "~>", left, right) =>
            "{key: " + emitTerm(left) + ", value: " + emitTerm(right) + "}"
        case EBinary(at, operator, left, right) =>
            "((" + emitTerm(left) + ") " + operator + " (" + emitTerm(right) + "))"
    }

    def escapeField(label : String, record : Option[Term]) = {
        if(fieldPattern.findFirstIn(label).isDefined) record.map(emitTerm).map(_ + "." + label).getOrElse(label)
        else record.map(emitTerm).map(_ + "[" + JSON.stringify(label) + "]").getOrElse(JSON.stringify(label))
    }

    val fieldPattern = """^[a-zA-z][a-zA-Z0-9_$]*$""".r

}
