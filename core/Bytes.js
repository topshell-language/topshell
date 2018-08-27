exports.fromList = bytes => new Uint8ClampedArray(bytes);
exports.toList = bytes => Array.from(bytes);
exports.size = bytes => bytes.byteLength;
exports.slice = start => stop => bytes => bytes.slice(start, stop);
