exports.dictionaryBy_ = g => f => v => {
    var cache = {};
    function h(x) {
        var k = g(x);
        if(!Object.prototype.hasOwnProperty.call(cache, k)) cache[k] = f(h)(x);
        return cache[k];
    }
    return h(v);
};

exports.dictionary_ = exports.dictionaryBy_(k => "" + k);

