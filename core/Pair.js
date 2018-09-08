exports.of = k => v => ({key: k, value: v});
exports.duplicate = v => ({key: v, value: v});
exports.swap = p => ({key: p.value, value: p.key});
exports.mapKey = f => p => ({key: f(p.key), value: p.value});
exports.mapValue = f => p => ({key: p.key, value: f(p.value)});
