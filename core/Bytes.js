//: List Int -> Bytes
exports.fromList = bytes => new Uint8Array(bytes);
//: Bytes -> List Int
exports.toList = bytes => Array.from(bytes);
//: String -> Bytes
exports.fromHex = self.tsh.fromHex;
//: Bytes -> String
exports.toHex = self.tsh.toHex;
//: String -> Bytes
exports.fromString = text => new TextEncoder("utf-8").encode(text);
//: Bytes -> String
exports.toString = bytes => new TextDecoder().decode(bytes);
//: Bytes -> Int
exports.size = bytes => bytes.byteLength;
//: Int -> Int -> Bytes -> Bytes
exports.slice = start => stop => bytes => bytes.slice(start, stop);
