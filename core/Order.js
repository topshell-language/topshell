//: a -> a -> a | Order a
exports.max = x => y => XOrder.max(x, y);
//: a -> a -> a | Order a
exports.min = x => y => XOrder.min(x, y);
//: a -> a -> Bool | Order a
exports.equal = x => y => XOrder.equal(x, y);
//: a -> a -> Bool | Order a
exports.notEqual = x => y => XOrder.notEqual(x, y);
//: a -> a -> Bool | Order a
exports.less = x => y => XOrder.less(x, y);
//: a -> a -> Bool | Order a
exports.lessEqual = x => y => XOrder.lessEqual(x, y);
//: a -> a -> Bool | Order a
exports.greater = x => y => XOrder.greater(x, y);
//: a -> a -> Bool | Order a
exports.greaterEqual = x => y => XOrder.greaterEqual(x, y);
