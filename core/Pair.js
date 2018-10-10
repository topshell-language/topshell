//: a -> b -> {key: a, value: b}
exports.of = k => v => ({key: k, value: v});
//: a -> {key: a, value: a}
exports.duplicate = v => ({key: v, value: v});
//: {key: a, value: b} -> {key: b, value: a}
exports.swap = p => ({key: p.value, value: p.key});
//: (a -> c) -> {key: a, value: b} -> {key: c, value: b}
exports.mapKey = f => p => ({key: f(p.key), value: p.value});
//: (b -> c) -> {key: a, value: b} -> {key: a, value: c}
exports.mapValue = f => p => ({key: p.key, value: f(p.value)});
