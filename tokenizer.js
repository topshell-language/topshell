self.tsh.Token = function(
    file, code, kind, startLine, startLineOffset, startOffset, stopLine, stopLineOffset, stopOffset
) {
    this.file = file;
    this.code = code;
    this.kind = kind;
    this.startLine = startLine;
    this.startLineOffset = startLineOffset;
    this.startOffset = startOffset;
    this.stopLine = stopLine;
    this.stopLineOffset = stopLineOffset;
    this.stopOffset = stopOffset;
};

self.tsh.Token.end = (file, code) => new self.tsh.Token(file, code, "end", 0, 0, 0, 0, 0, 0);

self.tsh.tokenize = (file, code) => {
    var tokens = [];
    var line = 1;
    var lineOffset = 0;

    var operatorCharactersString = "!@#$%&/=?+|^~*<>.:-,;";
    var operatorCharacters = {};
    for(var j = 0; j < operatorCharactersString.length; j++) operatorCharacters[operatorCharactersString[j]] = true;

    function token(kind, startOffset, stopOffset) {
        return new self.tsh.Token(
            file, code, kind, startLine, startLineOffset, startOffset, line, lineOffset, stopOffset
        );
    }

    for(var i = 0; i < code.length;) {

        while(code[i] === ' ' || code[i] === '\t' || code[i] === '\r') i++;

        var start = i;
        var startLine = line;
        var startLineOffset = lineOffset;

        if(i === lineOffset && code[i] !== "\n" &&
            code[i] !== "}" && code[i] !== "]" && code[i] !== ")" && code[i] !== "|" && code[i] !== "/"
        ) {
            tokens.push(token("top", i, i));
        }

        if(code[i] === '\n') {

            i += 1;
            line += 1;
            lineOffset = i;

        } else if(code[i] === '/' && code[i + 1] === '/') {

            i += 2;
            while(code[i] !== '\n' && i < code.length) i += 1;

        } else if(code[i] === '/' && code[i + 1] === '*') {

            i += 2;
            while(code[i] !== '*' || code[i + 1] !== '/') {
                if(i >= code.length) {
                    throw 'Expected end of comment started on line ' + startLine + ', got end of file.';
                }
                if(code[i] === '\n') {
                    line += 1;
                    lineOffset = i + 1;
                }
                i += 1;
            }
            i += 2;

        } else if(code[i] === '"') {

            i += 1;
            while(code[i] !== '"') {
                if(code[i] === '\n') {
                    throw 'Unexpected end of line in string started on line ' + startLine + '.';
                }
                if(i >= code.length) {
                    throw 'Expected end of string started on line ' + startLine + ', got end of file.';
                }
                if(code[i] === '\\' && code[i + 1] !== '\n') i += 1;
                i += 1;
            }
            i += 1;
            tokens.push(token("string", start, i));

        } else if((code[i] >= 'a' && code[i] <= 'z') || (code[i] >= 'A' && code[i] <= 'Z')) {

            var kind = code[i] >= 'a' ? "lower" : "upper";
            i += 1;
            while(
                (code[i] >= 'a' && code[i] <= 'z') ||
                (code[i] >= 'A' && code[i] <= 'Z') ||
                (code[i] >= '0' && code[i] <= '9')
                ) i += 1;
            tokens.push(token(kind, start, i));

        } else if(code[i] >= '0' && code[i] <= '9') {

            var dot = false;
            var exponent = false;
            while(code[i] >= '0' && code[i] <= '9') {
                i += 1;
                if((code[i] === 'e' || code[i] === 'E') && !exponent) {
                    i += 1;
                    dot = true;
                    exponent = true;
                    if(code[i] === '+' || code[i] === '-') i += 1;
                }
                if(code[i] === '.' && !dot && !exponent) {
                    i += 1;
                    dot = true;
                }
            }
            tokens.push(token(dot || exponent ? "float" : "int", start, i));

        } else if(code[i] === '_') {

            i += 1;
            tokens.push(token("definition", start, i));

        } else if(operatorCharacters[code[i]] === true) {

            i += 1;
            while(operatorCharacters[code[i]] === true) i += 1;
            var op = code.slice(start, i);
            var operatorKind =
                op === "=" || op === "<-" || op === "->" ||
                op === "," || op === ";" ||
                op === "." || op === ".?" ||
                op === ".." || op === ":" || op === "::" || op === "=>" || op === "?" ?
                    "separator" : "operator";
            tokens.push(token(operatorKind, start, i));

        } else if(
            code[i] === '(' || code[i] === ')' ||
            code[i] === '[' || code[i] === ']' ||
            code[i] === '{' || code[i] === '}'
        ) {

            i += 1;
            tokens.push(token("bracket", start, i));

        } else if(i < code.length) {

            throw 'Unexpected character: ' + code[i];

        }

    }

    for(var k = 0; k + 1 < tokens.length; k++) {
        var previous = k === 0 ? "" : code.slice(tokens[k - 1].startOffset, tokens[k - 1].stopOffset);
        var next = code.slice(tokens[k + 1].startOffset, tokens[k + 1].stopOffset);
        var isField = previous === "{" || previous === "." || previous === ".?" || previous === ",";
        if(tokens[k].kind === "lower" && (next === "=" || next === "<-" || next === "->")) {
            tokens[k].kind = "definition";
        } else if(tokens[k].kind === "lower" && !isField && next === ":") {
            tokens[k].kind = "definition"
        } else if(tokens[k].kind === "upper" && next === "@") {
            tokens[k].kind = "definition"
        }
    }

    return tokens;

};
