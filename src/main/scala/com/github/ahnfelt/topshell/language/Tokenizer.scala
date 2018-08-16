package com.github.ahnfelt.topshell.language

import com.github.ahnfelt.topshell.language.Syntax.Location

object Tokenizer {

    private val patterns = """
        comment:    [/][/][^\r\n]*
        number:     [0-9]+(?:[.][0-9]+(?:[eE][+-]?[0-9]+)?)?\b
        string:     ["](?:[^"\\\r\n]+|[\\][\\"trn])*["]
        lower:      [a-z][a-zA-Z0-9]*\b
        upper:      [A-Z][a-zA-Z0-9]*\b
        wildcard:   [_]\b
        operator:   [!@#$%&/=?+|^~*<>.:-]+
        separator:  [,;]|[.][.]?|[:][:]?
        bracket:    [(){}\[\]]
        space:      [ ]+
        newline:    [\r]?[\n]
        unknown:    [^\n ]+
    """

    private val (keys, parts) = patterns.split("\n").filter(_.trim.nonEmpty).map(_.split(":", 2).map(_.trim)).map {
        case Array(k, v) => k -> v
        case _ => throw new RuntimeException("The patterns variable is not correctly formatted.")
    }.unzip

    private val pattern = parts.map("(" + _ + ")").mkString("|").r

    private val groups = keys.zipWithIndex.map { case (k, i) => (k, i + 1) }.toMap

    case class Token(at : Location, kind : String, raw : String)

    case class ParseException(at : Location, message : String) extends RuntimeException(message + " " + at)

    def tokenize(file : String, code : String) : Array[Token] = {
        val tokens = {
            var line = 1
            var lineStart = 0
            var wildcard = 0
            pattern.findAllMatchIn(code).map { m =>
                def location = Location(file, line, 1 + m.start - lineStart)
                val token = if(m.group(groups("space")) != null) {
                    List.empty
                } else if(m.group(groups("newline")) != null) {
                    line += 1
                    lineStart = m.end
                    List.empty
                } else if(m.group(groups("comment")) != null) {
                    List.empty
                } else if(m.group(groups("lower")) != null) {
                    List(Token(location, "lower", m.group(groups("lower"))))
                } else if(m.group(groups("upper")) != null) {
                    List(Token(location, "upper", m.group(groups("upper"))))
                } else if(m.group(groups("operator")) != null) {
                    val raw = m.group(groups("operator"))
                    val kind =
                        if(List("=", "<-", "->", ".", "..", ":", "::", "?").contains(raw)) "separator" else "operator"
                    List(Token(location, kind, raw))
                } else if(m.group(groups("wildcard")) != null) {
                    wildcard += 1
                    List(Token(location, "definition", "_" + wildcard))
                } else {
                    val iterator = for(k <- keys) yield Option(m.group(groups(k))).map(g => Token(location, k, g))
                    List(iterator.collectFirst { case Some(t) => t }.get)
                }
                val all = m.group(0)
                if(m.start == lineStart && all != "}" && all != "]" && all != ")" && all.trim.nonEmpty) {
                    Token(location, "top", "top level symbol") :: token
                } else token
            }
        }.flatten.toArray
        tokens.zip(tokens.drop(1) :+ Token(null, "internal", "internal")).map { case (token, next) =>
            if(token.kind == "lower" && Seq("=", "<-", "->", "::").contains(next.raw)) {
                token.copy(kind = "definition")
            } else if(token.kind == "upper" && Seq("@").contains(next.raw)) {
                token.copy(kind = "definition")
            } else {
                token
            }
        }
    }

}
