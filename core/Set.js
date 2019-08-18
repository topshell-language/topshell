//: v -> Set v | Order v
exports.of = a => XSet.of([a]);

//: List v -> Set v | Order v
exports.ofList = a => XSet.of(a);

//: Set v -> List v | Order v
exports.toList = m => XSet.toList(m);

//: (k -> v) -> Set k -> Map k v | Order k
exports.toMap = f => s => XSet.toMap(f, s);

//: v -> Set v -> Set v | Order v
exports.add = v => m => XSet.add(k, v, m);

//: v -> Set v -> Set v | Order v
exports.remove = k => m => XSet.remove(k, m);

//: List (Set v) -> Set v | Order v
exports.unions = a => a.reduce((x, y) => XSet.union(x, y), XSet.empty);

//: Set v -> Set v -> Set v | Order v
exports.union = m1 => m2 => XSet.union(m1, m2);

//: Set v -> Set v -> Set v | Order v
exports.intersect = m1 => m2 => XSet.intersect(m1, m2);

//: Set v -> Set v -> Set v | Order v
exports.exclude = m1 => m2 => XSet.exclude(m1, m2);

//: v -> Set v -> Bool | Order v
exports.has = k => m => XSet.has(k, m);

//: v -> Set v -> Set v | Order v
exports.from = k => m => XSet.from(k, m);

//: v -> Set v -> Set v | Order v
exports.until = k => m => XSet.until(k, m);

//: (v -> a -> a) -> a -> Set v -> a
exports.foldLeft = f => z => m => XSet.foldLeft(f, z, m);

//: (v -> a -> a) -> a -> Set v -> a
exports.foldRight = f => z => m => XSet.foldRight(f, z, m);

//: Set v -> Int | Order v
exports.size = m => XSet.size(m);

//: Set v -> Bool | Order v
exports.isEmpty = m => XSet.isEmpty(m);

//: Set v | Order v
exports.empty = XSet.empty;
