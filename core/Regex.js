//: String -> Regex
exports.of = s => new self.tsh.Regex(s, "u");
//: String -> Regex
exports.ignoringCase = s => new self.tsh.Regex(s, "iu");
//: String -> Regex
exports.multiLine = s => new self.tsh.Regex(s, "um");
//: String -> Regex
exports.multiLineIgnoringCase = s => new self.tsh.Regex(s, "ium");
//: Regex -> String
exports.pattern = r => r.pattern;
//: Regex -> String
exports.flags = r => r.flags;
//: Regex -> String -> String -> String
exports.replaceFirst = r => t => s => s.replace(r.cacheNonGlobal(), t);
//: Regex -> String -> String -> String
exports.replaceAll = r => t => s => s.replace(r.cacheGlobal(), t);
//: Regex -> ({groups: List String, matched: String, input: String, from: Int, to: Int} -> String) -> String -> String
exports.replaceFirstWith = r => f => s => s.replace(r.cacheNonGlobal(), function() {
    var groups = Array.prototype.slice.call(arguments, 0, arguments.length - 2);
    var from = arguments[arguments.length - 2];
    var to = from + arguments[0].length;
    return f({groups: groups, matched: groups[0], input: s, from: from, to: to});
});
//: Regex -> ({groups: List String, matched: String, input: String, from: Int, to: Int} -> String) -> String -> String
exports.replaceAllWith = r => f => s => s.replace(r.cacheGlobal(), function() {
    var groups = Array.prototype.slice.call(arguments, 0, arguments.length - 2);
    var from = arguments[arguments.length - 2];
    var to = from + arguments[0].length;
    return f({groups: groups, matched: groups[0], input: s, from: from, to: to});
});
//: Regex -> String -> List String
exports.split = r => s => s.split(r.cacheNonGlobal());
//: Regex -> String -> List String
exports.findFirst = r => s => s.match(r.cacheNonGlobal()) || [];
//: Regex -> String -> List [List String]
exports.findAll = r => s => exports.matchAll(r)(s).map(m => m.groups);
//: Regex -> String -> Maybe {groups: List String, matched: String, input: String, from: Int, to: Int}
exports.matchFirst = r => s => exports.matchFirstFrom(0)(r)(s);
//: Int -> Regex -> String -> Maybe {groups: List String, matched: String, input: String, from: Int, to: Int}
exports.matchFirstFrom = o => r => s => {
    var result = [];
    var groups = null;
    var cached = r.cacheGlobal(o);
    if((groups = cached.exec(s)) != null) {
        return self.tsh.some({groups: groups, matched: groups[0], input: s, from: groups.index, to: cached.lastIndex});
    }
    return self.tsh.none;
};
//: Regex -> String -> List {groups: List String, matched: String, input: String, from: Int, to: Int}
exports.matchAll = r => s => exports.matchAllFrom(0)(r)(s);
//: Int -> Regex -> String -> List {groups: List String, matched: String, input: String, from: Int, to: Int}
exports.matchAllFrom = o => r => s => {
    var result = [];
    var groups = null;
    var cached = r.cacheGlobal(o);
    while((groups = cached.exec(s)) != null) {
        result.push({groups: groups, matched: groups[0], input: s, from: groups.index, to: cached.lastIndex});
    }
    return result;
};
