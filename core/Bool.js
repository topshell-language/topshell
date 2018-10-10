//: Bool
exports.true = true;
//: Bool
exports.false = false;
//: Bool -> Bool -> Bool
exports.xor = a => b => (a || b) && !(a && b);
//: Bool -> Bool -> Bool
exports.implies = a => b => !a || b;
