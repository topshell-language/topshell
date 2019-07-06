//: List {key: k, value: v} -> Map k v | Order k
exports.of = a => XMap.of(a);

//: (v -> v -> v) -> List {key: k, value: v} -> Map k v | Order k
exports.ofList = f => l => XMap.ofList(f, l);

//: Map k v -> List {key: k, value: v} | Order k
exports.toList = m => XMap.toList(m);

//: Map k v -> Set k | Order k
exports.keys = m => XMap.keys(m);

//: k -> v -> Map k v -> Map k v | Order k
exports.add = k => v => m => XMap.add(k, v, m);

//: k -> Map k v -> Map k v | Order k
exports.remove = k => m => XMap.remove(k, m);

//: (v -> v -> v) -> Map k v -> Map k v -> Map k v | Order k
exports.union = f => m1 => m2 => XMap.union(f, m1, m2);

//: (v -> v -> w) -> Map k v -> Map k v -> Map k w | Order k
exports.intersect = f => m1 => m2 => XMap.intersect(f, m1, m2);

//: v -> k -> Map k v -> v | Order k
exports.fallback = v => k => m => XMap.getOrElse(v, k, m);

//: k -> Map k v -> [None, Some v] | Order k
exports.get = k => m => XMap.get(k, m);

//: k -> Map k v -> Bool | Order k
exports.has = k => m => XMap.has(k, m);

//: k -> Map k v -> Map k v | Order k
exports.from = k => m => XMap.from(k, m);

//: k -> Map k v -> Map k v | Order k
exports.until = k => m => XMap.until(k, m);

//: (k -> v -> a -> a) -> a -> Map k v -> a
exports.foldLeft = f => z => m => XMap.foldLeft(f, z, m);

//: (k -> v -> a -> a) -> a -> Map k v -> a
exports.foldRight = f => z => m => XMap.foldRight(f, z, m);

//: Map k v -> Int | Order k
exports.size = m => XMap.size(m);

//: Map k v -> Bool | Order k
exports.isEmpty = m => XMap.isEmpty(m);

//: Map k v | Order k
exports.empty = XMap.empty;
