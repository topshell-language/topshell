//: Maybe a
exports.none = self.tsh.none;

//: a -> Maybe a
exports.some = self.tsh.some;

//: Maybe a -> Bool
exports.isNone = self.tsh.isNone;

//: Maybe a -> Bool
exports.isSome = self.tsh.isSome;

//: a -> Maybe a -> a
exports.default = d => v => exports.isNone(v) ? d : v.value;

//: Maybe (Maybe a) -> Maybe a
exports.flatten = v => exports.isNone(v) ? v : v.value;

//: (a -> b) -> Maybe a -> Maybe b
exports.map = f => v => exports.isNone(v) ? v : exports.some(f(v.value));

//: (a -> Maybe b) -> Maybe a -> Maybe b
exports.flatMap = f => v => exports.isNone(v) ? v : f(v.value);

//: Maybe a -> List a
exports.toList = v => exports.isNone(v) ? [] : [v.value];

//: String -> Maybe a -> a
exports.orFail = e => v => {if(exports.isNone(v)) throw e; else return v.value;};

//: List (Maybe a) -> List a
exports.flattenList = v => {
    var result = [];
    for(var i = 0; i < v.length; i++) {
        if(!exports.isNone(v[i])) result.push(v[i].value);
    }
    return result;
};
