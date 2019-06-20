package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax._

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSGlobal

@js.native @JSGlobal("tsh.Token")
class Token extends js.Object {
    val file : String = js.native
    val code : String = js.native
    val kind : String = js.native
    val startLine : Int = js.native
    val startLineOffset : Int = js.native
    val startOffset : Int = js.native
    val stopLine : Int = js.native
    val stopLineOffset : Int = js.native
    val stopOffset : Int = js.native
}

case class ParseException(at : Location, message : String) extends RuntimeException(message + " " + at)

class Parser(file : String, tokens : Array[Token]) {

    private implicit class RichToken(token : Token) {
        def at : Location = Location(token.file, token.startLine, (token.startLineOffset - token.startOffset) + 1)
        def raw : String = {
            val result = token.code.slice(token.startOffset, token.stopOffset)
            if(result != "_") result else {
                nextWildcard += 1
                "_" + nextWildcard
            }
        }
    }

    private var nextWildcard = 0
    private var nextAnonymousOutput = 0
    private var offset = 0
    private val end = js.Dynamic.global.tsh.Token.end(file, "").asInstanceOf[Token]
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
        val name = if(isDefinition) current.raw else { nextAnonymousOutput += 1; "out_" + nextAnonymousOutput }
        val variable = if(name != "_") name else { nextAnonymousOutput += 1; "out_" + nextAnonymousOutput }
        val at = if(isDefinition) ahead.at else current.at
        var bind = ahead.raw == "<-"
        try {
            if(!isDefinition) {
                bind = false
                TopSymbol(bind, Binding(at, variable, None, parseTerm()), List(), None)
            } else {
                skip("definition").raw
                val scheme = if(current.raw != ":") None else Some {
                    skip("separator", Some(":"))
                    parseScheme(false)
                }
                bind = current.raw == "<-"
                if(bind) skip("separator", Some("<-")).at else skip("separator", Some("=")).at
                val binding = Binding(at, variable, scheme, parseTerm())
                TopSymbol(bind, binding, List(), None)
            }
        } catch { case e : ParseException =>
            val binding = Binding(at, variable, None, ERecord(at, List(), None))
            TopSymbol(bind, binding, List(), Some(e))
        }
    }

    private def parseTerm() : Term = parseSemicolon()

    private def parseSemicolon() = {
        val term = parseNonSemicolonTerm()
        if(current.raw != ";") term else {
            val at = skip("separator", Some(";")).at
            nextWildcard += 1
            val binding = Binding(at, "_" + nextWildcard, None, term)
            EBind(at, binding, parseTerm())
        }
    }

    private def parseNonSemicolonTerm() : Term = parseIf()

    private def parseIf() : Term = {
        val condition = parseLeftPipe()
        if(current.raw != "?") condition else {
            val at = skip("separator", Some("?")).at
            val thenBody = parseNonSemicolonTerm()
            val elseBody = if(current.raw == ";") {
                skip("separator", Some(";"))
                Some(parseTerm())
            } else None
            EIf(at, condition, thenBody, elseBody)
        }
    }

    private def parseLeftPipe() : Term = {
        val result = parseRightPipe()
        if(current.raw != "<|") result else {
            val c = skip("operator", Some("<|"))
            EBinary(c.at, "<|", result, parseLeftPipe())
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

    private def parseRightPipe() : Term = parseBinary(Seq("|>"), () => parsePair())
    private def parsePair() : Term = parseBinary(Seq("~>"), () => parseAndOr())
    private def parseAndOr() : Term = parseBinary(Seq("&&", "||"), () => parseCompare())
    private def parseCompare() : Term = parseBinary(Seq(">", "<", ">=", "<=", "==", "!="), () => parseAppend())
    private def parseAppend() : Term = parseBinary(Seq("++"), () => parsePlus())
    private def parsePlus() : Term = parseBinary(Seq("+", "-"), () => parseMultiply())
    private def parseMultiply() : Term = parseBinary(Seq("*", "/"), () => parsePower())
    private def parsePower() : Term = parseBinary(Seq("^"), () => parseUnary())

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
        while(isAtomStartToken(current)) {
            val argument = parseDot()
            result = EApply(argument.at, result, argument)
        }
        result
    }

    private def isAtomStartToken(token : Token) : Boolean = {
        List("lower", "upper", "int", "float", "string", "definition").contains(token.kind) ||
        List("(", "[", "{").contains(token.raw)
    }

    private def parseDot() : Term = {
        var result = parseAtom()
        while(current.raw == "." || current.raw == ".?") {
            val optional = current.raw == ".?"
            val c = skip("separator")
            val label =
                if(current.kind == "string") JSON.parse(skip("string").raw).asInstanceOf[String]
                else skip("lower").raw
            result = EField(c.at, result, label, optional)
        }
        result
    }

    private def parseAtom() : Term = (current.kind, current.raw) match {
        case (_, "(") =>
            skip("bracket", Some("("))
            val result =
                if(current.kind == "operator" && binaryOperatorSymbols.contains(current.raw) && ahead.raw == ")") {
                    val c = skip("operator")
                    val at = c.at
                    EFunction(at, "_1", EFunction(at, "_2",
                        EBinary(at, c.raw, EVariable(at, "_1"), EVariable(at, "_2"))
                    ))
                } else if(current.raw == ";" && ahead.raw == ")") {
                    val at = skip("separator", Some(";")).at
                    nextWildcard += 1
                    EFunction(at, "_x1", EFunction(at, "_x2", {
                        val binding = Binding(at, "_" + nextWildcard, None, EVariable(at, "_x1"))
                        EBind(at, binding, EVariable(at, "_x2"))
                    }))
                } else if(current.raw == "." || current.raw == ".?") {
                    val optional = current.raw == ".?"
                    val at = skip("separator").at
                    val field =
                        if(current.kind == "string") JSON.parse(skip("string").raw).asInstanceOf[String]
                        else skip("lower").raw
                    EFunction(at, "_1", EField(at, EVariable(at, "_1"), field, optional))
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
        case (_, "{") if ahead.raw == "|" =>
            val at = skip("bracket", Some("{")).at
            var cases : List[VariantCase] = List.empty
            var defaultCase : Option[DefaultCase] = None
            while(current.raw == "|" && defaultCase.isEmpty) {
                val c = skip("operator", Some("|"))
                if(current.raw.startsWith("_")) {
                    skip(if(current.kind == "definition") "definition" else "lower")
                    skip("separator", Some("->"))
                    val body = parseTerm()
                    defaultCase = Some(DefaultCase(c.at, None, body))
                } else if(current.kind == "lower" || current.kind == "definition") {
                    val name = skip(if(current.kind == "definition") "definition" else "lower").raw
                    skip("separator", Some("->"))
                    val body = parseTerm()
                    defaultCase = Some(DefaultCase(c.at, Some(name), body))
                } else {
                    val name = skip("upper").raw
                    var arguments : List[Option[String]] = List.empty
                    while(current.raw.startsWith("_") || current.kind == "lower" || current.kind == "definition") {
                        val v = skip(if(current.kind == "definition") "definition" else "lower").raw
                        if(v.startsWith("_")) arguments ::= None
                        else arguments ::= Some(v)
                    }
                    skip("separator", Some("->"))
                    val body = parseTerm()
                    cases ::= VariantCase(c.at, name, arguments.reverse, body)
                }
            }
            skip("bracket", Some("}"))
            EMatch(at, cases.reverse, defaultCase)
        case (_, "{") =>
            val at = skip("bracket", Some("{")).at
            var bindings : List[Binding] = List.empty
            var rest : Option[Term] = None
            while(current.raw != "}" && rest.isEmpty) {
                val c = current
                val name =
                    if(current.kind == "string") JSON.parse(skip("string").raw).asInstanceOf[String]
                    else skip("lower").raw
                val value = if((current.raw == "," || current.raw == "}") && c.kind == "lower") {
                    EVariable(c.at, name)
                } else {
                    skip("separator", Some(":")).at
                    parseTerm()
                }
                bindings ::= Binding(at, name, None, value)
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
            if(current.raw == ".") EVariable(c.at, c.raw)
            else if(current.raw == "..") { skip("separator"); EVariable(c.at, c.raw) }
            else {
                var arguments = List[Term]()
                while(isAtomStartToken(current)) arguments ::= parseDot()
                EVariant(c.at, c.raw, arguments.reverse)
            }
        case ("int", _) =>
            val c = skip("int")
            EInt(c.at, c.raw)
        case ("float", _) =>
            val c = skip("float")
            EFloat(c.at, c.raw)
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
                val binding = Binding(at, variable, None, parseTerm())
                skip("separator", Some(","))
                EBind(at, binding, parseTerm())
            case "=" =>
                var bindings = List.empty[Binding]
                while(ahead.raw == "=") {
                    val variable = skip("definition").raw
                    val at = skip("separator", Some("=")).at
                    bindings ::= Binding(at, variable, None, parseTerm())
                    skip("separator", Some(","))
                }
                ELet(bindings.last.at, bindings.reverse, parseTerm())
            case _ =>
                throw ParseException(current.at, "Expected =, -> or <-, got " + current.raw)
        }
    }

    def parseScheme(explicit : Boolean) : Scheme = {
        var explicitParameters = List.empty[TypeParameter]
        while(explicit && ahead.raw == "=>") explicitParameters ::= parseTypeParameter()
        val generalized = parseType()
        val constraints = parseConstraints()
        val parameters =
            if(explicit) explicitParameters.reverse
            else Pretty.freeParameterNames(generalized, _ => None).toList.sorted.map(TypeParameter(_, KStar()))
        Scheme(parameters, constraints, generalized)
    }

    def parseTypeParameter() : TypeParameter = {
        val name = skip("lower").raw
        skip("separator", Some("=>"))
        TypeParameter(name, KStar())
    }

    def parseType() : Type = parseFunctionType()

    def parseFunctionType() : Type = {
        val left = parseTypeApplication()
        if(current.raw == "->") {
            skip("separator", Some("->"))
            val right = parseFunctionType()
            TApply(TApply(TConstructor("->"), left), right)
        } else {
            left
        }
    }

    def parseTypeApplication() : Type = {
        val left = parseTypeAtom()
        var applies = List.empty[Type]
        val kinds = List("definition", "lower", "upper", "string")
        while(kinds.contains(current.kind) || current.raw == "(" || current.raw == "{" || current.raw == "[") {
            applies ::= parseTypeAtom()
        }
        applies.reverse.foldLeft(left)(TApply)
    }

    def parseTypeAtom() : Type = {
        if(current.raw == "(") {
            skip("bracket", Some("("))
            val result = parseType()
            skip("bracket", Some(")"))
            result
        } else if(current.raw == "[") {
            parseVariantType()
        } else if(current.raw == "{") {
            parseRecordType()
        } else if(current.kind == "upper") {
            val c = skip("upper")
            TConstructor(c.raw)
        } else if(current.kind == "lower") {
            val c = skip("lower")
            TParameter(c.raw)
        } else if(current.kind == "definition") {
            val c = skip("definition")
            TParameter(c.raw)
        } else if(current.kind == "string") {
            val c = JSON.parse(skip("string").raw).asInstanceOf[String]
            TSymbol(c)
        } else {
            throw ParseException(current.at, "Expected type, got: " + current.raw)
        }
    }

    def parseVariantType() : Type = {
        skip("bracket", Some("["))
        var variants : List[(String, List[Type])] = List.empty
        while(current.raw != "]") {
            val name = skip("upper").raw
            var arguments = List[Type]()
            while(isAtomStartToken(current)) arguments ::= parseTypeAtom()
            variants ::= (name -> arguments.reverse)
            if(current.raw != "]") skip("separator", Some(","))
        }
        skip("bracket", Some("]"))
        TVariant(variants.reverse)
    }

    def parseRecordType() : Type = {
        skip("bracket", Some("{"))
        var bindings : List[TypeBinding] = List.empty
        while(current.raw != "}") {
            val name =
                if(current.kind == "string") JSON.parse(skip("string").raw).asInstanceOf[String]
                else skip("lower").raw
            skip("separator", Some(":"))
            val s = parseScheme(true)
            bindings ::= TypeBinding(name, s)
            if(current.raw != "}") skip("separator", Some(","))
        }
        skip("bracket", Some("}"))
        TRecord(bindings.reverse)
    }

    def parseConstraints() : List[Type] = {
        var constraints = List.empty[Type]
        while(current.raw == "|") constraints = parseConstraint().reverse ++ constraints
        constraints.reverse
    }

    def parseConstraint() : List[Type] = {
        skip("operator", Some("|"))
        if(current.raw == "[") {
            skip("bracket")
            val variant = skip("lower").raw
            skip("separator", Some(","))
            var result = List[Type]()
            while(current.raw != "]") {
                val name = skip("upper").raw
                var arguments = List[Type]()
                while(isAtomStartToken(current)) arguments ::= parseTypeAtom()
                result ::= VariantConstraint(TParameter(variant), name, arguments.reverse)
                if(current.raw == ",") skip("separator", Some(","))
            }
            skip("bracket", Some("]"))
            result.reverse
        } else if(current.raw == "{") {
            val at = skip("bracket").at
            val s1 = skip("lower").raw
            skip("separator", Some(":"))
            val t1 = parseType()
            val (s2, t2) = if(current.raw != ",") s1 -> t1 else {
                skip("separator", Some(","))
                val s2 = skip("lower").raw
                skip("separator", Some(":"))
                s2 -> parseType()
            }
            val constraints = parseConstraints()
            skip("bracket", Some("}"))
            def splitStructure(t : Type) = t match {
                case TApply(c, TParameter(s)) => Some(c) -> s
                case TParameter(s) => None -> s
                case _ => throw ParseException(at, "Expected structure, got: " + t)
            }
            val (c1, p1) = splitStructure(t1)
            val (c2, p2) = splitStructure(t2)
            if(p1 != p2) throw ParseException(at, "Structure parameter mismatch: " + p1 + " vs. " + p2)
            List(StructureConstraint(p1, TParameter(s1), c1, TParameter(s2), c2, constraints))
        } else if(ahead.raw == "." || ahead.raw == ".?") {
            val record = skip("lower").raw
            val o = skip("separator").raw
            val label =
                if (current.kind == "lower") skip("lower").raw
                else JSON.parse(skip("string").raw).asInstanceOf[String]
            skip("separator", Some(":"))
            val t = parseType()
            List(FieldConstraint(TParameter(record), label, t, o == ".?"))
        } else {
            val left = parseType()
            if(current.raw == "==") {
                val o = skip("operator", Some("==")).raw
                val right = parseType()
                List(TApply(TApply(TConstructor(o), left), right))
            } else {
                List(left)
            }
        }
    }

}

object Parser {

    def easy[T](file : String, code : String, parse : Parser => T) : T = {
        val tokens = js.Dynamic.global.tsh.tokenize(file, code).asInstanceOf[js.Array[Token]]
        val parser = new Parser(file, tokens.toArray[Token].drop(1))
        parse(parser)
    }

}