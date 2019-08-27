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

//: [None, Some a] -> a
exports.get = v => { if(v._ !== "Some") throw "Debug.get None"; else return v._1; };

//: Int -> List a -> a
exports.at = i => v => {
    if(!(i < v.length)) throw "Debug.at " + i + " [... " + v.length + " elements]";
    return v[i];
};

//: a -> b
exports.cast = x => x;
