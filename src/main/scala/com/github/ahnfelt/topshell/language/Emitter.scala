package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

import scala.scalajs.js.JSON

object Emitter {

    def emit(version : Double, topImports : List[TopImport], topSymbols : List[TopSymbol]) = {
        "var _h = _g.tsh;\n" +
        "var _n = [];\n" +
        topImports.map(emitImport).map("\n" + _ + "\n").mkString +
        topSymbols.map(emitTopSymbol).map("\n" + _ + "\n").mkString +
        "return _n;\n"
    }

    def emitImport(topImport : TopImport) : String = {
        "var " + topImport.name + "_;\n" +
        "_n.push({\n" +
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
                "compute: function() { return _h.loadImport(" + JSON.stringify(topImport.url) + "); },\n"
        }) +
        "setResult: function(_result) {\n" +
            topImport.name + "_ = _result;\n" +
        "}\n" +
        "});\n"
    }

    def emitTopSymbol(symbol : TopSymbol) : String = {
        "var " + symbol.binding.name + "_;\n" +
        "_n.push({\n" +
        "name: " + JSON.stringify(symbol.binding.name + "_") + ",\n" +
        "module: false,\n" +
        "effect: " + symbol.bind + ",\n" +
        "fromLine: " + symbol.binding.at.line + ",\n" +
        "toLine: " + lastLine(symbol.binding.value) + ",\n" +
        "dependencies: [" + symbol.dependencies.map("\"" + _ + "_\"").mkString(", ") + "],\n" +
        (symbol.error match {
            case Some(value) =>
                "error: " + JSON.stringify(value.message) + ",\n"
            case None =>
                "compute: function() {\n" +
                emitBody(symbol.binding.value) +
                "},\n"
        }) +
        "setResult: function(_result) {\n" +
            symbol.binding.name + "_ = _result;\n" +
        "}\n" +
        "});\n"
    }

    def lastLine(term : Syntax.Term) : Int = term match {
        case EString(at, value) =>
            at.line
        case EInt(at, value) =>
            at.line
        case EFloat(at, value) =>
            at.line
        case EVariable(at, name) =>
            at.line
        case EFunction(at, variable, body) =>
            Math.max(at.line, lastLine(body))
        case EApply(at, function, argument) =>
            Math.max(at.line, Math.max(lastLine(function), lastLine(argument)))
        case ELet(at, bindings, body) =>
            Math.max(at.line, Math.max((0 :: bindings.map(bindingLastLine)).max, lastLine(body)))
        case EBind(at, binding, body) =>
            Math.max(at.line, Math.max(bindingLastLine(binding), lastLine(body)))
        case EList(at, items) =>
            Math.max(at.line, (0 :: items.map(_.value).map(lastLine)).max)
        case EVariant(at, name, argument) =>
            at.line
        case EMatch(at, cases, defaultCase) =>
            Math.max(
                (at.line :: cases.map(c => lastLine(c.body))).max,
                defaultCase.map(c => lastLine(c.body)).getOrElse(0)
            )
        case ERecord(at, fields, rest) =>
            Math.max(at.line, Math.max((0 :: fields.map(bindingLastLine)).max, rest.map(lastLine).getOrElse(0)))
        case EField(at, record, field, _) =>
            Math.max(at.line, lastLine(record))
        case EIf(at, condition, thenBody, elseBody) =>
            Math.max(at.line, Math.max(lastLine(condition), Math.max(lastLine(thenBody), elseBody.map(lastLine).getOrElse(0))))
        case EUnary(at, operator, operand) =>
            Math.max(at.line, lastLine(operand))
        case EBinary(at, operator, left, right) =>
            Math.max(at.line, Math.max(lastLine(left), lastLine(right)))
    }

    def bindingLastLine(binding : Binding) : Int = {
        Math.max(binding.at.line, lastLine(binding.value))
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
        case EInt(at, value) => value
        case EFloat(at, value) => value
        case EVariable(at, name) => name + "_"
        case EFunction(at, variable, body) => "(function(" + variable + "_) {\n" + emitBody(body) + "})"
        case EApply(at, function, argument) => "(" + emitTerm(function) + ")(" + emitTerm(argument) + ")"
        case ELet(at, bindings, body) => "(function() {\n" + emitBody(term) + "})()"
        case EBind(at, binding, body) =>
            "_h.then(" + emitTerm(binding.value) + ", function(" + binding.name + "_) {\n" + emitBody(body) + "})\n"
        case EList(at, items) =>
            val allNormal = items.forall { case ListItem(_, None, false, _) => true; case _ => false }
            if(allNormal) "[" + items.map(_.value).map(emitTerm).mkString(", ") + "]" else {
                val itemCodes = for(ListItem(_, condition, spread, value) <- items) yield {
                    val valueCode = emitTerm(value)
                    val spreadCode = if(spread) valueCode else "[" + valueCode + "]"
                    condition match {
                        case None => spreadCode
                        case Some(c) => emitTerm(c) + " ? " + spreadCode + " : []"
                    }
                }
                "[" + itemCodes.mkString(", ") + "].flat()"
            }
        case EVariant(at, name, arguments) =>
            "{_: " + JSON.stringify(name) + arguments.zipWithIndex.map { case (a, i) =>
                ", _" + (i + 1) + ": " + emitTerm(a)
            }.mkString + "}"
        case EMatch(at, cases, defaultCase) =>
            "function(_v) { switch(_v._) {\n" +
            (for(VariantCase(_, variant, arguments, body) <- cases) yield
                "case " + JSON.stringify(variant) + ":\n" +
                (for((o, i) <- arguments.zipWithIndex; a <- o.toSeq) yield
                    "var " + a + "_ = _v._" + (i + 1) + ";\n"
                ).mkString +
                emitBody(body)
            ).mkString +
            (defaultCase match {
                case Some(DefaultCase(_, x, body)) =>
                    "default:\n" +
                    x.map("var " + _ + "_ = _v;\n").getOrElse("") +
                    emitBody(body)
                case None =>
                    "default: throw 'Unmatched variant: ' + _v._;\n"
            }) +
            "}}"
        case ERecord(at, fields, rest) =>
            val record = "{" + fields.map(b => escapeField(b.name, None) + ": " + emitTerm(b.value)).mkString(", ") + "}"
            rest.map(r => "_h.record(" + record + ", " + emitTerm(r) + ")").getOrElse(record)
        case EField(at, record, field, false) =>
            escapeField(field, Some(record))
        case EField(at, record, field, true) =>
            "_h.lookup(" + emitTerm(record) + ", " + JSON.stringify(field) + ")"
        case EIf(at, condition, thenBody, Some(elseBody)) =>
            "(" + emitTerm(condition) + " ? " + emitTerm(thenBody) + " : " + emitTerm(elseBody) + ")"
        case EIf(at, condition, thenBody, None) =>
            "(" + emitTerm(condition) + " ? self.tsh.some(" + emitTerm(thenBody) + ") : self.tsh.none)"
        case EUnary(at, "\\{}", operand) =>
            emitTerm(operand)
        case EUnary(at, operator, operand) =>
            "(" + operator + "(" + emitTerm(operand) + "))"
        case EBinary(at, "^", left, right) =>
            "Math.pow(" + emitTerm(left) + ", " + emitTerm(right) + ")"
        case EBinary(at, "<|", left, right) =>
            "((" + emitTerm(left) + ")(" + emitTerm(right) + "))"
        case EBinary(at, "|>", left, right) =>
            "((" + emitTerm(right) + ")(" + emitTerm(left) + "))"
        case EBinary(at, "~>", left, right) =>
            "{key: " + emitTerm(left) + ", value: " + emitTerm(right) + "}"
        case EBinary(at, "++", left, right) =>
            "(" + emitTerm(left) + ").concat(" + emitTerm(right) + ")"
        case EBinary(at, operator, left, right) =>
            "((" + emitTerm(left) + ") " + operator + " (" + emitTerm(right) + "))"
    }

    def escapeField(label : String, record : Option[Term]) = {
        if(fieldPattern.findFirstIn(label).isDefined) record.map(emitTerm).map(_ + "." + label).getOrElse(label)
        else record.map(emitTerm).map(_ + "[" + JSON.stringify(label) + "]").getOrElse(JSON.stringify(label))
    }

    val fieldPattern = """^[a-zA-z][a-zA-Z0-9_$]*$""".r

}
