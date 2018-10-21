//: ({} -> a) -> Lazy a
exports.of = f => new self.tsh.Lazy(f);

//: a -> Lazy a
exports.fromValue = x => {
    var lazy = new self.tsh.Lazy(null);
    lazy.value = x;
    return lazy;
};

//: Lazy a -> a
exports.force = l => {
    if(!Object.prototype.hasOwnProperty.call(l, "value")) l.value = l.compute({});
    return l.value;
};

//: (a -> String) -> (a -> b) -> a -> b
exports.memoBy = g => f => v => {
    var cache = {};
    function h(x) {
        var k = g(x);
        if(!Object.prototype.hasOwnProperty.call(cache, k)) cache[k] = f(h)(x);
        return cache[k];
    }
    return h(v);
};

//: (String -> a) -> String -> a
exports.memo = exports.dictionaryBy(k => k);
