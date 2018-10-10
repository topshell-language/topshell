//: List Number -> Bytes
exports.fromList = bytes => new Uint8ClampedArray(bytes);
//: Bytes -> List Number
exports.toList = bytes => Array.from(bytes);
//: Bytes -> Number
exports.size = bytes => bytes.byteLength;
//: Number -> Number -> Bytes -> Bytes
exports.slice = start => stop => bytes => bytes.slice(start, stop);
