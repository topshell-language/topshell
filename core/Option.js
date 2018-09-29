exports.none = self.tsh.none;
exports.some = self.tsh.some;
exports.isNone = self.tsh.isNone;
exports.isSome = self.tsh.isSome;
exports.default = d => v => exports.isNone(v) ? d : v.value;
exports.flatten = v => exports.isNone(v) ? v : v.value;
exports.map = f => v => exports.isNone(v) ? v : exports.some(f(v.value));
exports.flatMap = f => v => exports.isNone(v) ? v : f(v.value);
exports.toList = v => exports.isNone(v) ? [] : [v.value];
exports.orFail = e => v => {if(exports.isNone(v)) throw e; else return v;};

exports.flattenList = v => {
    var result = [];
    for(var i = 0; i < v.length; i++) {
        if(!exports.isNone(v[i])) result.push(v[i].value);
    }
    return result;
};
