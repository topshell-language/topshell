//: [None, Some a]
exports.none = self.tsh.none;

//: a -> [None, Some a]
exports.some = self.tsh.some;

//: [None, Some a] -> Bool
exports.isNone = self.tsh.isNone;

//: [None, Some a] -> Bool
exports.isSome = self.tsh.isSome;

//: a -> [None, Some a] -> a
exports.default = d => v => exports.isNone(v) ? d : v._1;

//: [None, Some [None, Some a]] -> [None, Some a]
exports.flatten = v => exports.isNone(v) ? v : v._1;

//: (a -> b) -> [None, Some a] -> [None, Some b]
exports.map = f => v => exports.isNone(v) ? v : exports.some(f(v._1));

//: (a -> [None, Some b]) -> [None, Some a] -> [None, Some b]
exports.flatMap = f => v => exports.isNone(v) ? v : f(v._1);

//: [None, Some a] -> List a
exports.toList = v => exports.isNone(v) ? [] : [v._1];

//: String -> [None, Some a] -> a
exports.orFail = e => v => {if(exports.isNone(v)) throw e; else return v._1;};

//: List [None, Some a] -> List a
exports.flattenList = v => {
    var result = [];
    for(var i = 0; i < v.length; i++) {
        if(!exports.isNone(v[i])) result.push(v[i]._1);
    }
    return result;
};
