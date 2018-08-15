package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

object Emitter {

    def emit(version : Double, topImports : List[TopImport], topSymbols : List[TopSymbol]) = {
        "if(!_g.tsh) _g.tsh = {};\n" +
        "var _s = _g.tsh;\n" +
        """
function _a(m, r) {
    for(var k in r) {
        if(Object.prototype.hasOwnProperty.call(r, k) && !Object.prototype.hasOwnProperty.call(m, k)) m[k] = r[k];
    }
    return m;
};
        """ +
        topImports.map(emitImport).map("\n" + _ + "\n").mkString +
        topSymbols.map(emitTopSymbol).map("\n" + _ + "\n").mkString +
        (if(topSymbols.isEmpty) "" else emitStart(version, topImports.map(_.name) ++ topSymbols.map(_.binding.name)))
    }

    def emitStart(version : Double, symbols : List[String]) : String = symbols match {
        case List() => "_g.tsh.last = v;\n"
        case s::ss =>
            "_s." + s + "_f(function(v) {\n" +
            "if(_g._tsh_code_version === " + version + ") " + emitStart(version, ss) +
            "});\n"
    }

    def emitImport(topImport : TopImport) : String = {
        "var " + topImport.name + "_ = void 0;\n" +
        "_s." + topImport.name + "_ = " + topImport.name + "_;\n" +
        "_s." + topImport.name + "_e = void 0;\n" +
        "_s." + topImport.name + "_f = function(_c) {\n" +
        (if(topImport.error.isEmpty) emitImportFetch(topImport) else "") +
        "};\n"
    }

    def emitImportFetch(topImport : TopImport) : String = {
        s"""
            var xhr = new XMLHttpRequest();
            xhr.open('GET', '${topImport.url.replaceAll("[\\r\\n'\\\"\\\\]", "")}');
            xhr.onload = function() {
                if(xhr.status === 200) {
                    var f = new Function('exports', xhr.responseText);
                    var exported = {};
                    f(exported);
                    ${topImport.name}_ = exported;
                } else {
                    _s.${topImport.name}_e = 'Could not load module';
                }
                _s.${topImport.name}_ = ${topImport.name}_;
                _d("${topImport.name}", {_tag: "span", children: ["Module ${topImport.url}"]}, _s.${topImport.name}_e);
                _c(${topImport.name}_, _s.${topImport.name}_e);
            };
            xhr.send();
        """
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
        s"""_d("${symbol.binding.name}", ${symbol.binding.name}_, _s.${symbol.binding.name}_e);""" + "\n" +
        "_g.setTimeout(() => _c(" + symbol.binding.name + "_, _s." + symbol.binding.name + "_e), 0);\n" +
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
            // TODO: Opened modules should be stored in _o, and then it should be _o.then_
            "List_.then_(" + emitTerm(binding.value) + ")(function(" + binding.name + "_) {\n" + emitBody(body) + "})\n"
        case EList(at, elements, rest) =>
            val list = "[" + elements.map(emitTerm).mkString(", ") + "]"
            rest.map(r => "(" + list + ".concat(" + emitTerm(r) + "))").getOrElse(list)
        case ERecord(at, fields, rest) =>
            val record = "{" + fields.map(b => b.name + "_:" + emitTerm(b.value)).mkString(", ") + "}"
            rest.map(r => "_a(" + record + ", " + emitTerm(r) + ")").getOrElse(record)
        case EField(at, record, field) =>
            emitTerm(record) + "." + field + "_"
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
