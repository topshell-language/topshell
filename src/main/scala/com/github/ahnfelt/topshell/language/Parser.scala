package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._
import com.github.ahnfelt.topshell.language.Tokenizer.{ParseException, Token}

import scala.scalajs.js.JSON

class Parser(file : String, tokens : Array[Token]) {

    //println(tokens.map(_.kind).mkString(" "))

    private var nextAnonymousOutput = 0
    private var offset = 0
    private val end = Token(Location(file, tokens.lastOption.map(_.at.line + 1).getOrElse(1), 1), "end", "end of file")
    private def current =
        if(offset < tokens.length) tokens(offset) else end
    private def ahead =
        if(offset + 1 < tokens.length) tokens(offset + 1) else end
    private def skip(kind : String, value : Option[String] = None) : Token = {
        val c = current
        if(c.kind != kind) throw ParseException(c.at, "Expected " + kind + value.map(" " + _).getOrElse("") + ", got " + c.raw)
        if(value.exists(_ != c.raw)) throw ParseException(c.at, "Expected " + value.get + " got " + c.raw)
        offset += 1
        c
    }

    def parseTopLevel() : (List[TopImport], List[TopSymbol]) = {
        if(current.kind != "top" && current.kind != "end") {
            throw ParseException(current.at, "Expected an unindented top-level definition")
        }
        var topImports = List.empty[TopImport]
        var topSymbols = List.empty[TopSymbol]
        while(current.kind == "top") {
            skip("top")
            if(ahead.raw == "@") {
                val topImport = parseTopImport()
                if(topImports.exists(_.name == topImport.name)) {
                    topImports ::= topImport.copy(error = Some(
                        ParseException(current.at, "Duplicate import of " + topImport.name)
                    ))
                } else {
                    topImports ::= topImport
                }
            } else {
                val symbol = parseTopSymbol()
                if(current.kind != "top" && current.kind != "end") {
                    topSymbols ::= {
                        if(symbol.error.nonEmpty) symbol
                        else symbol.copy(error = Some(ParseException(current.at, "Unexpected " + current.raw)))
                    }
                    while(current.kind != "top" && current.kind != "end") offset += 1
                } else {
                    val name = symbol.binding.name
                    topSymbols ::= {
                        if(!topSymbols.exists(_.binding.name == name)) symbol
                        else symbol.copy(error = Some(ParseException(current.at, "Duplicate definition of " + name)))
                    }
                }
            }
        }
        if(current.kind != "end") throw ParseException(current.at, "Expected end of file, got " + current.raw)
        topImports.reverse -> topSymbols.reverse
    }

    private def parseTopImport() : TopImport = {
        val name = current.raw
        val at = current.at
        try {
            skip("definition")
            skip("operator", Some("@"))
            val url = JSON.parse(skip("string").raw).asInstanceOf[String]
            if(current.kind != "top" && current.kind != "end") {
                val unexpectedAt = current.at
                while (current.kind != "top" && current.kind != "end") offset += 1
                TopImport(at, name, url, Some(ParseException(unexpectedAt, "Unexpected " + current.raw)))
            } else {
                TopImport(at, name, url, None)
            }
        } catch { case e : ParseException =>
            TopImport(at, name, "???", Some(e))
        }
    }

    private def parseTopSymbol() : TopSymbol = {
        val isDefinition = current.kind == "definition"
        val variable = if(isDefinition) current.raw else { nextAnonymousOutput += 1; "out_" + nextAnonymousOutput }
        val bind = ahead.raw == "<-"
        val at = if(isDefinition) ahead.at else current.at
        try {
            if(!isDefinition) {
                TopSymbol(bind, Binding(at, variable, parseTerm()), List(), None)
            } else {
                skip("definition").raw
                if(bind) skip("separator", Some("<-")).at else skip("separator", Some("=")).at
                val binding = Binding(at, variable, parseTerm())
                TopSymbol(bind, binding, List(), None)
            }
        } catch { case e : ParseException =>
            val binding = Binding(at, variable, ERecord(at, List(), None))
            TopSymbol(bind, binding, List(), Some(e))
        }
    }

    private def parseTerm() : Term = parseIf()

    private def parseIf() : Term = {
        val condition = parsePipe()
        if(current.raw != "?") condition else {
            val at = skip("separator", Some("?")).at
            val thenBody = parseTerm()
            skip("separator", Some(","))
            val elseBody = parseTerm()
            EIf(at, condition, thenBody, elseBody)
        }
    }

    private def parseBinary(operators : Seq[String], parseOperand : () => Term) : Term = {
        var result = parseOperand()
        val operator = current.raw
        while(operators.contains(current.raw)) {
            val c = skip("operator", Some(operator))
            val argument = parseOperand()
            result = EBinary(c.at, operator, result, argument)
        }
        result
    }

    private val allBinary = Seq(
        Seq("|"),
        Seq("~>"),
        Seq("&&", "||"),
        Seq(">", "<", ">=", "<=", "==", "!="),
        Seq("+", "-"),
        Seq("*", "/"),
        Seq("^"),
    ).flatten

    private def parsePipe() : Term = parseBinary(Seq("|"), parsePair)
    private def parsePair() : Term = parseBinary(Seq("~>"), parseAndOr)
    private def parseAndOr() : Term = parseBinary(Seq("&&", "||"), parseCompare)
    private def parseCompare() : Term = parseBinary(Seq(">", "<", ">=", "<=", "==", "!="), parsePlus)
    private def parsePlus() : Term = parseBinary(Seq("+", "-"), parseMultiply)
    private def parseMultiply() : Term = parseBinary(Seq("*", "/"), parsePower)
    private def parsePower() : Term = parseBinary(Seq("^"), parseUnary)

    private def parseUnary() : Term = {
        if(current.raw == "!" || current.raw == "-") {
            val c = skip("operator")
            val argument = parseApply()
            EUnary(c.at, c.raw, argument)
        } else {
            parseApply()
        }
    }

    private def parseApply() : Term = {
        var result = parseDot()
        while(List("lower", "upper", "number", "string", "definition").contains(current.kind) || List("(", "[", "{").contains(current.raw)) {
            val argument = parseDot()
            result = EApply(argument.at, result, argument)
        }
        result
    }

    private def parseDot() : Term = {
        var result = parseAtom()
        while(current.raw == ".") {
            val c = skip("separator", Some("."))
            val label = skip("lower").raw
            result = EField(c.at, result, label)
        }
        result
    }

    private def parseAtom() : Term = (current.kind, current.raw) match {
        case (_, "(") =>
            skip("bracket", Some("("))
            val result = if(current.kind == "operator" && allBinary.contains(current.raw) && ahead.raw == ")") {
                val c = skip("operator")
                val at = c.at
                EFunction(at, "_1", EFunction(at, "_2",
                    EBinary(at, c.raw, EVariable(at, "_1"), EVariable(at, "_2"))
                ))
            } else if(current.raw == ".") {
                val at = skip("separator").at
                val field = skip("lower").raw
                EFunction(at, "_1", EField(at, EVariable(at, "_1"), field))
            } else {
                parseTerm()
            }
            skip("bracket", Some(")"))
            result
        case (_, "[") =>
            val at = skip("bracket", Some("[")).at
            var elements : List[Term] = List.empty
            var rest : Option[Term] = None
            while(current.raw != "]" && rest.isEmpty) {
                elements ::= parseTerm()
                if(current.raw != "]") skip("separator", Some(","))
                if(current.raw == "..") {
                    skip("separator", Some(".."))
                    rest = Some(parseTerm())
                }
            }
            skip("bracket", Some("]"))
            EList(at, elements.reverse, rest)
        case (_, "{") =>
            val at = skip("bracket", Some("{")).at
            var bindings : List[Binding] = List.empty
            var rest : Option[Term] = None
            while(current.raw != "}" && rest.isEmpty) {
                val c = skip("lower")
                val name = c.raw
                val value = if(current.raw == "," || current.raw == "}") EVariable(c.at, c.raw) else {
                    skip("separator", Some(":")).at
                    parseTerm()
                }
                bindings ::= Binding(at, name, value)
                if(current.raw != "}") skip("separator", Some(","))
                if(current.raw == "..") {
                    skip("separator", Some(".."))
                    rest = Some(parseTerm())
                }
            }
            skip("bracket", Some("}"))
            ERecord(at, bindings.reverse, rest)
        case ("lower", _) =>
            val c = skip("lower")
            EVariable(c.at, c.raw)
        case ("upper", _) =>
            val c = skip("upper")
            EVariable(c.at, c.raw)
        case ("number", _) =>
            val c = skip("number")
            ENumber(c.at, c.raw)
        case ("string", _) =>
            val c = skip("string")
            EString(c.at, c.raw)
        case ("definition", _) =>
            parseDefinition()
        case _ =>
            throw ParseException(current.at, "Expected an atom, got " + current.raw)
    }

    private def parseDefinition() : Term = {
        ahead.raw match {
            case "->" =>
                val variable = skip("definition").raw
                val at = skip("separator", Some("->")).at
                EFunction(at, variable, parseTerm())
            case "<-" =>
                val variable = skip("definition").raw
                val at = skip("separator", Some("<-")).at
                val binding = Binding(at, variable, parseTerm())
                skip("separator", Some(","))
                EBind(at, binding, parseTerm())
            case "=" =>
                var bindings = List.empty[Binding]
                while(ahead.raw == "=") {
                    val variable = skip("definition").raw
                    val at = skip("separator", Some("=")).at
                    bindings ::= Binding(at, variable, parseTerm())
                    skip("separator", Some(","))
                }
                ELet(bindings.last.at, bindings.reverse, parseTerm())
            case _ =>
                throw ParseException(current.at, "Expected =, -> or <-, got " + current.raw)
        }
    }

}
