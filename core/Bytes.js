exports.fromList_ = bytes => new Uint8ClampedArray(bytes);
exports.toList_ = bytes => Array.from(bytes);
exports.size_ = bytes => bytes.byteLength;
exports.slice_ = start => stop => bytes => bytes.slice(start, stop);
