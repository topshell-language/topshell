exports.true = true;
exports.false = false;
exports.xor = a => b => (a || b) && !(a && b);
exports.implies = a => b => !a || b;
