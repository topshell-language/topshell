exports.fromArray = bytes => new Uint8ClampedArray(bytes);
exports.toArray = bytes => Array.from(bytes);
exports.size = bytes => bytes.byteLength;
exports.slice = start => stop => bytes => bytes.slice(start, stop);
