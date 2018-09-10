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

exports.range = function(start) {
    return function(stop) {
        var result = [];
        for(var i = start; i <= stop; i++) {
            result.push(i);
        }
        return result;
    };
};

exports.size = function(r) { return r.length; };
exports.isEmpty = function(r) { return r.length === 0; };
exports.at = function(i) { return function(r) { return r[i]; }; };
exports.take = function(i) { return function(r) { return r.slice(0, i); }; };
exports.drop = function(i) { return function(r) { return r.slice(i); }; };
exports.takeLast = function(i) { return function(r) { return r.slice(-i); }; };
exports.dropLast = function(i) { return function(r) { return r.slice(0, -i); }; };

exports.filter = function(f) { return function(r) { return r.filter(f); }; };
exports.reverse = function(r) { return r.slice().reverse(); };
exports.find = function(f) { return function(r) { return r.find(f); }; };
exports.all = function(f) { return function(r) { return r.every(f); }; };
exports.any = function(f) { return function(r) { return r.some(f); }; };

exports.head = function(r) { return r[0]; };
exports.tail = function(r) { return r.slice(1); };
exports.append = function(r) { return function(a) { return r.concat(a); }; };

exports.foldLeft = f => z => a => a.reduce((x, y) => f(x)(y), z);
exports.foldRight = f => z => a => a.reduceRight((x, y) => f(x)(y), z);

exports.sort = f => a => a.slice().sort((a, b) => f(a)(b) ? -1 : f(b)(a) ? 1 : 0);

exports.repeat = n => v => new Array(n).fill(v);

exports.flatten = l => Array.prototype.concat.apply(...l);

exports.zip = a => b => {
    let result = [];
    for(var i = 0; i < a.length && i < b.length; i++) {
        result.push({key: a[i], value: b[i]});
    }
    return result;
};

exports.zipWith = f => a => b => {
    let result = [];
    for(var i = 0; i < a.length && i < b.length; i++) {
        result.push(f(a[i])(b[i]));
    }
    return result;
};

exports.takeWhile = f => a => {
    let result = [];
    for(var i = 0; i < a.length; i++) {
        if(!f(a[i])) return result;
        result.push(a[i]);
    }
    return result;
};

exports.dropWhile = f => a => {
    let result = [];
    for(var j = 0; j < a.length && f(a[j]); j++) {}
    for(var i = j; i < a.length; i++) {
        result.push(a[i]);
    }
    return result;
};

exports.unzip = a => {
    let keys = [];
    let values = [];
    for(var i = 0; i < a.length; i++) {
        keys.push(a[i].key);
        values.push(a[i].key);
    }
    return {key: keys, value: values};
};

exports.withKeys = a => a.map((e, i) => ({key: i, value: e}));

exports.keys = a => a.map((e, i) => i);

exports.scanLeft = f => z => a => {
    let result = new Array(a.length);
    for(var i = 0; i < a.length; i++) {
        z = f(z)(a[i]);
        result[i] = z;
    }
    return result;
};

exports.scanRight = f => z => a => {
    let result = new Array(a.length);
    for(var i = a.length - 1; i >= 0; i--) {
        z = f(a[i])(z);
        result[i] = z;
    }
    return result;
};
