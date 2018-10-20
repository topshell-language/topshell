//: (b -> c) -> (a -> b) -> a -> c
exports.compose = f => g => x => f(g(x));

//: (a -> b) -> (b -> c) -> a -> c
exports.chain = f => g => x => g(f(x));

//: ((a -> b) -> (a -> b)) -> a -> b
exports.fix = f => x => f(exports.fix(f))(x);

//: (a -> b -> c) -> b -> a -> c
exports.flip = f => a => b => f(b)(a);

//: a -> a
exports.id = x => x;

//: a -> b -> a
exports.const = c => x => c;

//: (a -> Bool) -> (a -> Bool)
exports.non = f => x => !f(x);
