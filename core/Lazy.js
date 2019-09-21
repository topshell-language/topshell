//: ({} -> a) -> Lazy a
exports.of = f => new self.tsh.Lazy(f);

//: a -> Lazy a
exports.ofValue = x => {
    var lazy = new self.tsh.Lazy(null);
    lazy.value = x;
    return lazy;
};

//: Lazy a -> a
exports.force = l => {
    if(!Object.prototype.hasOwnProperty.call(l, "value")) l.value = l.compute({});
    return l.value;
};

//: ((a -> b) -> (a -> b)) -> a -> b | Order a
exports.memo = f => x => {
    var cache = XMap.empty;
    function h(k) {
        var c = XMap.get(k, cache);
        if(self.tsh.isNone(c)) {
            var v = f(h)(k);
            cache = XMap.add(k, v, cache);
            return v;
        } else {
            return c._1
        }
    }
    return h(x);
};

//: ((String -> a) -> (String -> a)) -> String -> a
exports.memoString = f => x => {
    var cache = {};
    function h(k) {
        if(!Object.prototype.hasOwnProperty.call(cache, k)) cache[k] = f(h)(k);
        return cache[k];
    }
    return h(x);
};

//: ((Int -> a) -> (Int -> a)) -> Int -> a
exports.memoArray = f => x => {
    var cache = [];
    function t(x) {
        var k = x;
        if(!Object.prototype.hasOwnProperty.call(cache, k)) cache[k] = f(t)(x);
        return cache[k];
    }
    return t(x);
};

//: ((Int -> Int -> a) -> (Int -> Int -> a)) -> Int -> Int -> a
exports.memoTable = f => x => y => {
    var cache = {};
    function t(x) { return function(y) {
        var k = x + "," + y;
        if(!Object.prototype.hasOwnProperty.call(cache, k)) cache[k] = f(t)(x)(y);
        return cache[k];
    }}
    return t(x)(y);
};

//: ((Int -> Int -> Int -> a) -> (Int -> Int -> Int -> a)) -> Int -> Int -> Int -> a
exports.memoCube = f => x => y => z => {
    var cache = {};
    function t(x) { return function(y) {
        var k = x + "," + y + "," + z;
        if(!Object.prototype.hasOwnProperty.call(cache, k)) cache[k] = f(t)(x)(y)(z);
        return cache[k];
    }}
    return t(x)(y)(z);
};
