//: List {key: k, value: v} -> Map k v | Order k
exports.of = a => XMap.of(a);

//: (v -> v -> v) -> List {key: k, value: v} -> Map k v | Order k
exports.fromList = f => l => XMap.fromList(f, l);

//: Map k v -> List {key: k, value: v} | Order k
exports.toList = m => XMap.toList(m);

//: k -> v -> Map k v -> Map k v | Order k
exports.add = k => v => m => XMap.add(k, v, m);

//: k -> Map k v -> Map k v | Order k
exports.remove = k => m => XMap.remove(k, m);

//: (v -> v -> v) -> Map k v -> Map k v -> Map k v | Order k
exports.union = f => m1 => m2 => XMap.union(f, m1, m2);

//: (v -> v -> w) -> Map k v -> Map k v -> Map k w | Order k
exports.intersect = f => m1 => m2 => XMap.intersect(f, m1, m2);

//: k -> Map k v -> [None, Some v] | Order k
exports.get = k => m => XMap.get(k, m);

//: k -> Map k v -> Bool | Order k
exports.has = k => m => XMap.has(k, m);

//: Map k v -> Int | Order k
exports.size = m => XMap.size(m);

//: Map k v -> Bool | Order k
exports.isEmpty = m => XMap.isEmpty(m);

//: Map k v | Order k
exports.empty = XMap.empty;
