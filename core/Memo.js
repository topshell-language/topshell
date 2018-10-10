//: (a -> String) -> (a -> b) -> a -> b
exports.dictionaryBy = g => f => v => {
    var cache = {};
    function h(x) {
        var k = g(x);
        if(!Object.prototype.hasOwnProperty.call(cache, k)) cache[k] = f(h)(x);
        return cache[k];
    }
    return h(v);
};

//: (String -> a) -> String -> a
exports.dictionary = exports.dictionaryBy(k => k);
