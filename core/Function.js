//: (b -> c) -> (a -> b) -> a -> c
exports.compose = f => g => x => f(g(x));

//: (a -> b) -> (b -> c) -> a -> c
exports.chain = f => g => x => g(f(x));

//: ((a -> b) -> (a -> b)) -> a -> b
exports.fix = f => x => f(exports.fix(f))(x);
