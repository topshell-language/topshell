//: List Number -> String
exports.fromCodePoints = c => String.fromCodePoint.apply(String, c);
//: String -> List Number
exports.toCodePoints = s => Array.from(s).map(c => c.codePointAt(0));
//: String -> List String -> String
exports.join = s => a => a.join(s);
//: Number -> String -> String -> String
exports.padStart = n => c => s => s.padStart(n, c);
//: Number -> String -> String -> String
exports.padEnd = n => c => s => s.padEnd(n, c);
//: Number -> String -> String
exports.repeat = n => s => s.repeat(n);
//: String -> String -> String -> String
exports.replace = i => o => s => s.replace(i, o);
//: String -> String -> Bool
exports.startsWith = x => s => s.startsWith(x);
//: String -> String -> Bool
exports.endsWith = x => s => s.endsWith(x);
//: String -> String -> List String
exports.split = x => s => s.split(x);
//: String -> String -> String
exports.at = i => s => s.charAt(i) || "";
//: String -> Number
exports.size = s => s.length;
//: String -> String -> Bool
exports.includes = x => s => s.includes(x);
//: Number -> Number -> String -> String
exports.slice = a => b => s => s.slice(a, b);
//: Number -> String -> String
exports.take = i => s => s.slice(0, i);
//: Number -> String -> String
exports.drop = i => s => s.slice(i);
//: String -> String
exports.trim = s => s.trim();
//: String -> String
exports.toUpper = s => s.toUpperCase();
//: String -> String
exports.toLower = s => s.toLowerCase();
//: String -> Number
exports.toNumber = i => parseFloat(i);
//: Number -> String
exports.fromNumber = i => "" + i;
//: String -> Number
exports.toInt = i => parseInt(i, 10);
//: Number -> String
exports.fromInt = i => "" + i;
//: Number -> String -> Number
exports.toIntBase = b => i => parseInt(i, b);
//: Number -> Number -> String
exports.fromIntBase = b => i => i.toString(b);
//: String -> List String
exports.lines = s => s.split("\n");
