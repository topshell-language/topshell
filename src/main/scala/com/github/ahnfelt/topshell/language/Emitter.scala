package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

object Emitter {

    def emit(topSymbols : List[TopSymbol]) = {
        "(function(_g) {\n" +
        "if(!_g.tsh) _g.tsh = {};\n" +
        "var _s = _g.tsh;\n" +
        "var _p = _g.tsh_prelude;\n" +
        "var null_ = _p.null_;\n" +
        "var false_ = _p.false_;\n" +
        "var true_ = _p.true_;\n" +
        "var tag_ = _p.tag_;\n" +
        "var visual_ = _p.visual_;\n" +
        "var _a = _p.recordRest;\n" +
        topSymbols.map(emitTopSymbol).map("\n" + _ + "\n").mkString +
        (if(topSymbols.isEmpty) "" else emitStart(topSymbols)) +
        "\n})(this);\n"
    }

    def emitStart(symbols : List[Syntax.TopSymbol]) : String = symbols match {
        case List() => "_g.tsh.last = v;"
        case s::ss =>
            "_s." + s.binding.name + "_f(function(v) {\n" +
            emitStart(ss) +
            "});\n"
    }

    def emitTopSymbol(symbol : TopSymbol) : String = {
        "var " + symbol.binding.name + "_ = void 0;\n" +
        "_s." + symbol.binding.name + "_ = " + symbol.binding.name + "_;\n" +
        "_s." + symbol.binding.name + "_e = void 0;\n" +
        "_s." + symbol.binding.name + "_f = function(_c) {\n" +
        "try {\n" +
        (if(symbol.error.isEmpty) symbol.binding.name + "_ = " + emitTerm(symbol.binding.value) + ";\n" else "") +
        "} catch(e) {\n" +
        "_s." + symbol.binding.name + "_e = e;\n" +
        "}\n" +
        "_s." + symbol.binding.name + "_ = " + symbol.binding.name + "_;\n" +
        "_c(" + symbol.binding.name + "_, _s." + symbol.binding.name + "_e);\n" +
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
            "_p._then(" + emitTerm(binding.value) + ")(function(" + binding.name + "_) {\n" + emitBody(body) + "})\n"
        case EList(at, elements, rest) =>
            val list = "[" + elements.map(emitTerm).mkString(", ") + "]"
            rest.map(r => "(" + list + ".concat(" + emitTerm(r) + "))").getOrElse(list)
        case ERecord(at, fields, rest) =>
            val record = "{" + fields.map(b => b.name + "_:" + emitTerm(b.value)).mkString(", ") + "}"
            rest.map(r => "_a(" + record + ", " + emitTerm(r) + ")").getOrElse(record)
        case EField(at, record, field) =>
            field match {
                case "map" => "_p._map(" + emitTerm(record) + ")"
                case "then" => "_p._then(" + emitTerm(record) + ")"
                case "size" => "_p._size(" + emitTerm(record) + ")"
                case "at" => "_p._at(" + emitTerm(record) + ")"
                case "take" => "_p._take(" + emitTerm(record) + ")"
                case "drop" => "_p._drop(" + emitTerm(record) + ")"
                case "takeLast" => "_p._takeLast(" + emitTerm(record) + ")"
                case "dropLast" => "_p._dropLast(" + emitTerm(record) + ")"
                case "takeWhile" => "_p._takeWhile(" + emitTerm(record) + ")"
                case "dropWhile" => "_p._dropWhile(" + emitTerm(record) + ")"
                case "takeLastWhile" => "_p._takeLastWhile(" + emitTerm(record) + ")"
                case "dropLastWhile" => "_p._dropLastWhile(" + emitTerm(record) + ")"
                case "zip" => "_p._zip(" + emitTerm(record) + ")"
                case "unzip" => "_p._unzip(" + emitTerm(record) + ")"
                case "indexes" => "_p._indexes(" + emitTerm(record) + ")"
                case "filter" => "_p._filter(" + emitTerm(record) + ")"
                case "startsWith" => "_p._startsWith(" + emitTerm(record) + ")"
                case "endsWith" => "_p._endsWith(" + emitTerm(record) + ")"
                case "find" => "_p._find(" + emitTerm(record) + ")"
                case "any" => "_p._any(" + emitTerm(record) + ")"
                case "all" => "_p._all(" + emitTerm(record) + ")"
                case "empty" => "_p._empty(" + emitTerm(record) + ")"
                case "reverse" => "_p._reverse(" + emitTerm(record) + ")"
                case "join" => "_p._join(" + emitTerm(record) + ")"
                case "sort" => "_p._sort(" + emitTerm(record) + ")"
                case "first" => "_p._first(" + emitTerm(record) + ")"
                case "last" => "_p._last(" + emitTerm(record) + ")"
                case "rest" => "_p._rest(" + emitTerm(record) + ")"
                case "append" => "_p._append(" + emitTerm(record) + ")"
                case "foldLeft" => "_p._foldLeft(" + emitTerm(record) + ")"
                case "foldRight" => "_p._foldRight(" + emitTerm(record) + ")"
                case "scanLeft" => "_p._scanLeft(" + emitTerm(record) + ")"
                case "scanRight" => "_p._scanRight(" + emitTerm(record) + ")"
                case _ => emitTerm(record) + "." + field + "_"
            }
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
