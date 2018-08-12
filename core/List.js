exports.map_ = function(f) {
    return function(r) {
        var result = [];
        for(var i = 0; i < r.length; i++) {
            var a = f(r[i]);
            result.push(a);
        }
        return result;
    };
};

exports.then_ = function(f) {
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

exports.range_ = function(start) {
    return function(stop) {
        var result = [];
        for(var i = start; i <= stop; i++) {
            result.push(i);
        }
        return result;
    };
};

exports.size_ = function(r) { return r.length; };
exports.isEmpty_ = function(r) { return r.length === 0; };
exports.at_ = function(r) { return function(i) { return r[i]; }; };
exports.take_ = function(r) { return function(i) { return r.slice(0, i); }; };
exports.drop_ = function(r) { return function(i) { return r.slice(i); }; };
exports.takeLast_ = function(r) { return function(i) { return r.slice(-i); }; };
exports.dropLast_ = function(r) { return function(i) { return r.slice(0, -i); }; };

exports.filter_ = function(f) { return function(r) { return r.filter(f); }; };
exports.reverse_ = function(r) { return r.slice().reverse(); };
exports.find_ = function(f) { return function(r) { return r.find(f); }; };
exports.all_ = function(f) { return function(r) { return r.every(f); }; };
exports.any_ = function(f) { return function(r) { return r.some(f); }; };

exports.head_ = function(r) { return r[0]; };
exports.tail_ = function(r) { return r.slice(1); };
exports.append_ = function(r) { return function(a) { return r.concat(a); }; };

exports.foldLeft_ = f => z => a => a.reduce((x, y) => f(x)(y), z);
exports.foldRight_ = f => z => a => a.reduceRight((x, y) => f(x)(y), z);

exports.sort_ = f => a => a.slice().sort((a, b) => f(a)(b) ? -1 : f(b)(a) ? 1 : 0);


exports.takeWhile_ = function(r) { return r.takeWhile_; };
exports.dropWhile_ = function(r) { return r.dropWhile_; };
exports.takeLastWhile_ = function(r) { return r.takeLastWhile_; };
exports.dropLastWhile_ = function(r) { return r.dropLastWhile_; };
exports.zip_ = function(r) { return r.zip_; };
exports.unzip_ = function(r) { return r.unzip_; };
exports.indexes_ = function(r) { return r.indexes_; };
exports.startsWith_ = function(r) { return r.startsWith_; };
exports.endsWith_ = function(r) { return r.endsWith_; };

exports.scanLeft_ = function(r) { return r.scanLeft_; };
exports.scanRight_ = function(r) { return r.scanRight_; };
