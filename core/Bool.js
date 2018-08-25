exports.true_ = true;
exports.false_ = false;
exports.xor_ = a => b => (a || b) && !(a && b);
exports.implies_ = a => b => !a || b;
