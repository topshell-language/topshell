//: (a -> b) -> List a -> List b
exports.map = function(f) {
    return function(r) {
        var result = [];
        for(var i = 0; i < r.length; i++) {
            var a = f(r[i]);
            result.push(a);
        }
        return result;
    };
};

//: (a -> List b) -> List a -> List b
exports.then = function(f) {
    return function(r) {
        var result = [];
        for(var i = 0; i < r.length; i++) {
            var a = f(r[i]);
            for(var j = 0; j < a.length; j++) {
                result.push(a[j]);
            }
        }
        return result;
    };
};

//: Number -> Number -> List Number
exports.range = function(start) {
    return function(stop) {
        var result = [];
        for(var i = start; i <= stop; i++) {
            result.push(i);
        }
        return result;
    };
};

//: (a -> Maybe {key: a, value: b}) -> a -> List b
exports.unfold = f => z => {
    var result = [];
    var e = null;
    while(self.tsh.isSome(e = f(z))) {
        z = e.value.key;
        result.push(e.value.value);
    }
    return result;
};

//: List a -> Number
exports.size = function(r) { return r.length; };
//: List a -> Number
exports.isEmpty = function(r) { return r.length === 0; };
//: Number -> List a -> Maybe a
exports.at = function(i) { return function(r) { return i >= 0 && i < r.length ? self.tsh.some(r[i]) : self.tsh.none; }; };
//: Number -> List a -> List a
exports.take = function(i) { return function(r) { return r.slice(0, i); }; };
//: Number -> List a -> List a
exports.drop = function(i) { return function(r) { return r.slice(i); }; };
//: Number -> List a -> List a
exports.takeLast = function(i) { return function(r) { return r.slice(-i); }; };
//: Number -> List a -> List a
exports.dropLast = function(i) { return function(r) { return r.slice(0, -i); }; };

//: (a -> Bool) -> List a -> List a
exports.filter = function(f) { return function(r) { return r.filter(f); }; };
//: List a -> List a
exports.reverse = function(r) { return r.slice().reverse(); };
//: (a -> Bool) -> List a -> Maybe a
exports.find = function(f) { return function(r) { return r.find(f); }; };
//: (a -> Bool) -> List a -> Bool
exports.all = function(f) { return function(r) { return r.every(f); }; };
//: (a -> Bool) -> List a -> Bool
exports.any = function(f) { return function(r) { return r.some(f); }; };

//: List a -> Maybe a
exports.head = function(r) { return r.length > 0 ? self.tsh.some(r[0]) : self.tsh.none; };
//: List a -> List a
exports.tail = function(r) { return r.slice(1); };
//: List a -> List a -> List a
exports.append = function(r) { return function(a) { return r.concat(a); }; };

//: (a -> b -> a) -> a -> List b -> a
exports.foldLeft = f => z => a => a.reduce((x, y) => f(x)(y), z);
//: (a -> b -> b) -> b -> List a -> b
exports.foldRight = f => z => a => a.reduceRight((x, y) => f(x)(y), z);

//: (a -> a -> Bool) -> List a -> List a
exports.sort = f => a => a.slice().sort((a, b) => f(a)(b) ? -1 : f(b)(a) ? 1 : 0);

//: Number -> a -> List a
exports.repeat = n => v => new Array(n).fill(v);

//: List (List a) -> List a
exports.flatten = l => Array.prototype.concat.apply(...l);
//: (a -> List b) -> List a -> List b
exports.flatMap = exports.then;

//: List a -> List b -> List {key: a, value: b}
exports.zip = a => b => {
    let result = [];
    for(var i = 0; i < a.length && i < b.length; i++) {
        result.push({key: a[i], value: b[i]});
    }
    return result;
};

//: (a -> b -> c) -> List a -> List b -> List c
exports.zipWith = f => a => b => {
    let result = [];
    for(var i = 0; i < a.length && i < b.length; i++) {
        result.push(f(a[i])(b[i]));
    }
    return result;
};

//: (a -> Bool) -> List a -> List a
exports.takeWhile = f => a => {
    let result = [];
    for(var i = 0; i < a.length; i++) {
        if(!f(a[i])) return result;
        result.push(a[i]);
    }
    return result;
};

//: (a -> Bool) -> List a -> List a
exports.dropWhile = f => a => {
    let result = [];
    for(var j = 0; j < a.length && f(a[j]); j++) {}
    for(var i = j; i < a.length; i++) {
        result.push(a[i]);
    }
    return result;
};

//: List {key: a, value: b} -> {key: List a, value: List b}
exports.unzip = a => {
    let keys = [];
    let values = [];
    for(var i = 0; i < a.length; i++) {
        keys.push(a[i].key);
        values.push(a[i].key);
    }
    return {key: keys, value: values};
};

//: List a -> List {key: Number, value: a}
exports.withKeys = a => a.map((e, i) => ({key: i, value: e}));

//: List a -> List Number
exports.keys = a => a.map((e, i) => i);

//: (a -> b -> a) -> a -> List b -> List a
exports.scanLeft = f => z => a => {
    let result = new Array(a.length);
    for(var i = 0; i < a.length; i++) {
        z = f(z)(a[i]);
        result[i] = z;
    }
    return result;
};

//: (a -> b -> b) -> b -> List a -> List b
exports.scanRight = f => z => a => {
    let result = new Array(a.length);
    for(var i = a.length - 1; i >= 0; i--) {
        z = f(a[i])(z);
        result[i] = z;
    }
    return result;
};
