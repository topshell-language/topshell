//: Int -> Bool
exports.isEven = x => x % 2 === 0;
//: Int -> Bool
exports.isOdd = x => (x + 1) % 2 === 0;
//: Int -> Float
exports.toFloat = x => x;
//: Float -> Int
exports.floor = x => {
    if(!isFinite(x)) throw 'Int.floor ' + x;
    return Math.floor(x);
};
//: Float -> Int
exports.ceil = x => {
    if(!isFinite(x)) throw 'Int.ceil ' + x;
    return Math.ceil(x);
};
//: Float -> Int
exports.round = x => {
    if(!isFinite(x)) throw 'Int.round ' + x;
    return Math.round(x);
};
//: Int -> Int -> Int
exports.remainder = top => bottom => top % bottom;
