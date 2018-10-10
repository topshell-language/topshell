//: a -> a
exports.log = v => { console.dir(v); return v };

//: (a -> b) -> a -> a
exports.logBy = f => v => { console.dir(f(v)); return v };

//: a -> b
exports.throw = m => { throw m };

//: a
exports.null = null;

//: a
exports.undefined = void 0;
