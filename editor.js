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

        var fieldRE = /[?]?[a-z][a-zA-Z0-9]*|["]([^"\\]|[\\][.])*["]/;
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
                ":", ".", ";", ",", "|", "<|", "|>", "=", "<-", "->", "=>", "~>",
                "_", "..", "@", "?",
                "+", "-", "*", "/", "^", "!", "&&", "||",
                "<", ">", "<=", ">=", "==", "!=",
                "&", "$", "%"
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
    var list = Array.prototype.concat.apply([],
        Object.entries(tsh.coreModules)
            .sort((a, b) => a[0].localeCompare(b[0]))
            .map((e) => e[1].map(s => {
                var text = e[0] + "." + s.name + " ";
                return {text: text, displayText: text + ": " + s.type, render: CodeMirror.renderTopshellHint};
            }))
    );
    var cursor = editor.getCursor();
    var currentLine = editor.getLine(cursor.line);
    var start = cursor.ch;
    var end = start;
    while (end < currentLine.length && /[\w$]+/.test(currentLine.charAt(end))) ++end;
    while (start && /[\w$.]+/.test(currentLine.charAt(start - 1))) --start;
    var curWord = start !== end && currentLine.slice(start, end);
    var regex = new RegExp('^' + curWord, 'i');

    var items = !curWord ? list : list.filter(item => item.text.match(regex));

    return {
        list: items,
        from: CodeMirror.Pos(cursor.line, start),
        to: CodeMirror.Pos(cursor.line, end)
    };
};

CodeMirror.renderTopshellHint = (element, self, data) => {
    let dot = data.displayText.indexOf(".");
    let colon = data.displayText.indexOf(":");
    let module = data.displayText.slice(0, dot);
    let field = data.displayText.slice(dot, colon);
    let t = data.displayText.slice(colon);
    let moduleNode = document.createElement("span");
    moduleNode.setAttribute("class", "topshell-hint-module");
    moduleNode.appendChild(document.createTextNode(module));
    let fieldNode = document.createTextNode(field);
    let typeNode = document.createElement("span");
    typeNode.setAttribute("class", "topshell-hint-type");
    typeNode.appendChild(document.createTextNode(t));
    element.appendChild(moduleNode);
    element.appendChild(fieldNode);
    element.appendChild(typeNode);
    element.style.maxWidth = "600px";
};
