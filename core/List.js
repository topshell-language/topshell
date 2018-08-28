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


exports.takeWhile = function(r) { return r.takeWhile; };
exports.dropWhile = function(r) { return r.dropWhile; };
exports.takeLastWhile = function(r) { return r.takeLastWhile; };
exports.dropLastWhile = function(r) { return r.dropLastWhile; };
exports.unzip = function(r) { return r.unzip; };
exports.indexes = function(r) { return r.indexes; };
exports.startsWith = function(r) { return r.startsWith; };
exports.endsWith = function(r) { return r.endsWith; };

exports.scanLeft = function(r) { return r.scanLeft; };
exports.scanRight = function(r) { return r.scanRight; };
