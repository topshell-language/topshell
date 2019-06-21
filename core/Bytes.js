//: List Int -> Bytes
exports.fromList = bytes => new Uint8ClampedArray(bytes);
//: Bytes -> List Int
exports.toList = bytes => Array.from(bytes);
//: Bytes -> String
exports.toHex = self.tsh.toHex;
//: String -> Bytes
exports.fromHex = self.tsh.fromHex;
//: Bytes -> Int
exports.size = bytes => bytes.byteLength;
//: Int -> Int -> Bytes -> Bytes
exports.slice = start => stop => bytes => bytes.slice(start, stop);
