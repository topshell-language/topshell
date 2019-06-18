//: List Int -> String
exports.fromCodePoints = c => String.fromCodePoint.apply(String, c);
//: String -> List Int
exports.toCodePoints = s => Array.from(s).map(c => c.codePointAt(0));
//: String -> List String -> String
exports.join = s => a => a.join(s);
//: Int -> String -> String -> String
exports.padStart = n => c => s => s.padStart(n, c);
//: Int -> String -> String -> String
exports.padEnd = n => c => s => s.padEnd(n, c);
//: Int -> String -> String
exports.repeat = n => s => s.repeat(n);
//: String -> String
exports.reverse = s => s.split("").reverse().join("");
//: String -> String -> String -> String
exports.replace = i => o => s => s.replace(i, o);
//: String -> String -> Bool
exports.startsWith = x => s => s.startsWith(x);
//: String -> String -> Bool
exports.endsWith = x => s => s.endsWith(x);
//: String -> String -> List String
exports.split = x => s => s.split(x);
//: Int -> String -> String
exports.at = i => s => s.charAt(i) || "";
//: String -> Int
exports.size = s => s.length;
//: String -> String -> Bool
exports.includes = x => s => s.includes(x);
//: Int -> Int -> String -> String
exports.slice = a => b => s => s.slice(a, b);
//: Int -> String -> String
exports.take = i => s => s.slice(0, i);
//: Int -> String -> String
exports.drop = i => s => s.slice(i);
//: String -> String
exports.trim = s => s.trim();
//: String -> String
exports.toUpper = s => s.toUpperCase();
//: String -> String
exports.toLower = s => s.toLowerCase();
//: String -> Float
exports.toFloat = i => parseFloat(i);
//: Float -> String
exports.fromFloat = i => "" + i;
//: String -> Int
exports.toInt = i => parseInt(i, 10);
//: Int -> String
exports.fromInt = i => "" + i;
//: Int -> String -> Int
exports.toIntBase = b => i => parseInt(i, b);
//: Int -> Int -> String
exports.fromIntBase = b => i => i.toString(b);
//: String -> List String
exports.lines = s => s.split("\n");
