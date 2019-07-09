//: List Int -> Bytes
exports.ofList = bytes => new Uint8Array(bytes);
//: Bytes -> List Int
exports.toList = bytes => Array.from(bytes);
//: String -> Bytes
exports.ofHex = self.tsh.ofHex;
//: Bytes -> String
exports.toHex = self.tsh.toHex;
//: String -> Bytes
exports.ofString = text => new TextEncoder("utf-8").encode(text);
//: Bytes -> String
exports.toString = bytes => new TextDecoder().decode(bytes);
//: Bytes -> Int
exports.size = bytes => bytes.byteLength;
//: Int -> Int -> Bytes -> Bytes
exports.slice = start => stop => bytes => bytes.slice(start, stop);
