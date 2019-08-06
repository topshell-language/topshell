//: List Int -> String
exports.ofCodePoints = c => String.fromCodePoint.apply(String, c);
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
exports.ofFloat = i => "" + i;
//: String -> Int
exports.toInt = i => parseInt(i, 10);
//: Int -> String
exports.ofInt = i => "" + i;
//: Int -> String -> Int
exports.toIntBase = b => i => parseInt(i, b);
//: Int -> Int -> String
exports.ofIntBase = b => i => i.toString(b);
//: String -> List String
exports.lines = s => s.split("\n");

//: Bytes -> String
exports.ofBytes = bytes => new TextDecoder().decode(bytes);
//: String -> Bytes
exports.toBytes = text => new TextEncoder().encode(text);
//: Stream Bytes -> Stream String
exports.ofBytesStreaming = stream => new self.tsh.Stream(async function*(world) {
    let decoder = new TextDecoder();
    let o = stream.open(world);
    while(true) {
        let n = await o.next();
        if(n.done) {
            let s = decoder.decode();
            if(s.length !== 0) yield {result: s};
            return;
        }
        let s = decoder.decode(n.value.result, {stream: true});
        if(s.length !== 0) yield {result: s};
    }
});
//: Stream String -> Stream Bytes
exports.toBytesStreaming = stream => stream.filter(s => s.length !== 0).map(exports.toBytes);
