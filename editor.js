(function(mod) {
    if (typeof exports == "object" && typeof module == "object") // CommonJS
        mod(require("../../lib/codemirror"));
    else if (typeof define == "function" && define.amd) // AMD
        define(["../../lib/codemirror"], mod);
    else // Plain browser env
        mod(CodeMirror);
})(function(CodeMirror) {
    "use strict";

    CodeMirror.defineMode("topshell", function() {

        function switchState(source, setState, f) {
            setState(f);
            return f(source, setState);
        }

        var fieldRE = /[a-z][a-zA-Z0-9]*|["]([^"\\]|[\\][.])*["]/;
        var smallRE = /[a-z_]/;
        var largeRE = /[A-Z]/;
        var digitRE = /[0-9]/;
        var hexitRE = /[0-9A-Fa-f]/;
        var octitRE = /[0-7]/;
        var idRE = /[a-z_A-Z0-9\']/;
        var symbolRE = /[-!#$%&*+.\/<=>?@\\^|~:\u03BB\u2192]/;
        var specialRE = /[(),;[\]`{}]/;
        var whiteCharRE = /[ \t\v\f]/; // newlines are handled in tokenizer

        function normal() {
            return function (source, setState) {
                if (source.eatWhile(whiteCharRE)) {
                    return null;
                }

                var ch = source.next();
                if (specialRE.test(ch)) {
                    if (ch == '/' && source.eat('*')) {
                        var t = "comment";
                        return switchState(source, setState, ncomment(t, 1));
                    }
                    return null;
                }

                if (ch == '\'') {
                    if (source.eat('\\'))
                        source.next();  // should handle other escapes here
                    else
                        source.next();

                    if (source.eat('\''))
                        return "string";
                    return "error";
                }

                if (ch == '"') {
                    return switchState(source, setState, stringLiteral);
                }

                if (ch == '.') {
                    source.match(fieldRE);
                    return "attribute";
                }

                if (largeRE.test(ch)) {
                    source.eatWhile(idRE);
                    return "keyword";
                }

                if (smallRE.test(ch)) {
                    var isDef = source.pos === 1;
                    source.eatWhile(idRE);
                    return source.eat(':') ? "attribute" : "variable";
                }

                if (digitRE.test(ch)) {
                    if (ch == '0') {
                        if (source.eat(/[xX]/)) {
                            source.eatWhile(hexitRE); // should require at least 1
                            return "integer";
                        }
                        if (source.eat(/[oO]/)) {
                            source.eatWhile(octitRE); // should require at least 1
                            return "number";
                        }
                    }
                    source.eatWhile(digitRE);
                    var t = "number";
                    if (source.eat('.')) {
                        t = "number";
                        source.eatWhile(digitRE); // should require at least 1
                    }
                    if (source.eat(/[eE]/)) {
                        t = "number";
                        source.eat(/[-+]/);
                        source.eatWhile(digitRE); // should require at least 1
                    }
                    return t;
                }

                if (symbolRE.test(ch)) {
                    if (ch == '/' && source.eat(/\//)) {
                        source.eatWhile(/\//);
                        if (!source.eat(symbolRE)) {
                            source.skipToEnd();
                            return "comment";
                        }
                    }
                    source.eatWhile(symbolRE);
                    return "builtin";
                }

                return "error";
            }
        }

        function ncomment(type, nest) {
            if (nest == 0) {
                return normal();
            }
            return function(source, setState) {
                var currNest = nest;
                while (!source.eol()) {
                    var ch = source.next();
                    if (ch == '/' && source.eat('*')) {
                        ++currNest;
                    } else if (ch == '*' && source.eat('/')) {
                        --currNest;
                        if (currNest == 0) {
                            setState(normal());
                            return type;
                        }
                    }
                }
                setState(ncomment(type, currNest));
                return type;
            }
        }

        function stringLiteral(source, setState) {
            while (!source.eol()) {
                var ch = source.next();
                if (ch == '"') {
                    setState(normal());
                    return source.eat(":") ? "attribute" : "string";
                }
                if (ch == '\\') {
                    if (source.eol() || source.eat(whiteCharRE)) {
                        setState(stringGap);
                        return source.eat(":") ? "attribute" : "string";
                    }
                    if (!source.eat('&')) source.next(); // should handle other escapes here
                }
            }
            setState(normal());
            return "error";
        }

        function stringGap(source, setState) {
            if (source.eat('\\')) {
                return switchState(source, setState, stringLiteral);
            }
            source.next();
            setState(normal());
            return "error";
        }


        var wellKnownWords = (function() {
            var wkw = {};

            var keywords = [
                "(", ")", "[", "]", "{", "}",
                ":", ".", ";", ",", "|", "=", "<-", "->", "=>", "~>",
                "_", "..", "@", "?",
                "+", "-", "*", "/", "^", "!", "&&", "||",
                "<", ">", "<=", ">=", "==", "!="
            ];

            for (var i = keywords.length; i--;)
                wkw[keywords[i]] = "operator";

            return wkw;
        })();



        return {
            startState: function ()  { return { f: normal() }; },
            copyState:  function (s) { return { f: s.f }; },

            token: function(stream, state) {
                var t = state.f(stream, function(s) { state.f = s; });
                var w = stream.current();
                return (wellKnownWords.hasOwnProperty(w)) ? wellKnownWords[w] : t;
            }
        };

    });

    CodeMirror.defineMIME("text/x-topshell", "topshell");
});


CodeMirror.hint.topshell = function (editor) {
    var list = ["Base64.encode", "Base64.decode", "Bool.true", "Bool.false", "Bool.xor", "Bool.implies", "Bytes.fromArray", "Bytes.toArray", "Bytes.size", "Bytes.slice", "Debug.log", "Debug.logBy", "Debug.throw", "Debug.null", "Debug.undefined", "File.readText", "File.writeText", "File.list", "File.listStatus", "File.status", "Html.of", "Html.tag", "Html.text", "Html.attribute", "Html.style", "Http.fetch", "Http.fetchText", "Http.fetchJson", "Http.fetchBytes", "Http.text", "Http.json", "Http.bytes", "Http.header", "Http.ok", "Http.redirected", "Http.status", "Http.statusText", "Http.type", "Http.url", "Json.read", "Json.write", "Json.pretty", "List.map", "List.then", "List.range", "List.size", "List.isEmpty", "List.at", "List.take", "List.drop", "List.takeLast", "List.dropLast", "List.filter", "List.reverse", "List.find", "List.all", "List.any", "List.head", "List.tail", "List.append", "List.foldLeft", "List.foldRight", "List.sort", "List.repeat", "List.flatten", "List.zip", "List.zipWith", "List.takeWhile", "List.dropWhile", "List.unzip", "List.withKeys", "List.keys", "List.scanLeft", "List.scanRight", "Math.pi", "Math.e", "Math.remainder", "Math.isFinite", "Math.isNaN", "Math.abs", "Math.acos", "Math.acosh", "Math.asin", "Math.asinh", "Math.atan", "Math.atan2", "Math.atanh", "Math.cbrt", "Math.ceil", "Math.clz32", "Math.cos", "Math.cosh", "Math.exp", "Math.expm1", "Math.floor", "Math.fround", "Math.hypot", "Math.imul", "Math.log", "Math.log10", "Math.log1p", "Math.log2", "Math.max", "Math.min", "Math.round", "Math.sign", "Math.sin", "Math.sinh", "Math.sqrt", "Math.tan", "Math.tanh", "Math.trunc", "Memo.dictionaryBy", "Memo.dictionary", "Pair.of", "Pair.duplicate", "Pair.swap", "Pair.mapKey", "Pair.mapValue", "Process.run", "Process.shell", "Ssh.do", "String.fromCodePoints", "String.toCodePoints", "String.join", "String.padStart", "String.padEnd", "String.repeat", "String.replace", "String.startsWith", "String.endsWith", "String.split", "String.at", "String.size", "String.includes", "String.slice", "String.take", "String.drop", "String.trim", "String.toUpper", "String.toLower", "String.toInt", "String.fromInt", "String.toIntBase", "String.fromIntBase", "String.split", "String.lines", "Task.of", "Task.throw", "Task.catch", "Task.then", "Task.filter", "Task.scan", "Task.merge", "Task.race", "Task.zipWith", "Task.all", "Task.map", "Task.sleep", "Task.interval", "Task.now", "Task.random", "Task.log", "View.by", "View.tableBy", "View.table", "View.bars", "View.text", "View.tree"];
    list = list.map(c => c + " ");
    var cursor = editor.getCursor();
    var currentLine = editor.getLine(cursor.line);
    var start = cursor.ch;
    var end = start;
    while (end < currentLine.length && /[\w$]+/.test(currentLine.charAt(end))) ++end;
    while (start && /[\w$.]+/.test(currentLine.charAt(start - 1))) --start;
    var curWord = start != end && currentLine.slice(start, end);
    var regex = new RegExp('^' + curWord, 'i');

    var items = (!curWord ? list : list.filter(function (item) {
        return item.match(regex);
    })).sort();

    var result = {
        list: items.map(i => ({text: i, render: CodeMirror.renderTopshellHint})),
        from: CodeMirror.Pos(cursor.line, start),
        to: CodeMirror.Pos(cursor.line, end)
    };

    return result;
};

CodeMirror.renderTopshellHint = (element, self, data) => {
    let parts = data.text.split(".", 2);
    let module = parts[0];
    let field = parts[1];
    let moduleNode = document.createElement("span");
    moduleNode.setAttribute("class", "topshell-hint-module");
    moduleNode.appendChild(document.createTextNode(module));
    let fieldNode = document.createTextNode(field);
    element.appendChild(moduleNode);
    element.appendChild(document.createTextNode("."));
    element.appendChild(fieldNode);
};
