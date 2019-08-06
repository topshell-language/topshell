//: Bytes
exports.empty = new Uint8Array(0);
//: List Int -> Bytes
exports.ofList = bytes => new Uint8Array(bytes);
//: Bytes -> List Int
exports.toList = bytes => Array.from(bytes);
//: String -> Bytes
exports.ofHex = self.tsh.ofHex;
//: Bytes -> String
exports.toHex = self.tsh.toHex;
//: Bytes -> Int
exports.size = bytes => bytes.byteLength;
//: Int -> Bytes -> Bytes
exports.take = size => bytes => bytes.slice(0, size);
//: Int -> Bytes -> Bytes
exports.drop = size => bytes => bytes.slice(size);
//: (Int -> Bool) -> Bytes -> Bytes
exports.takeWhile = f => bytes => { let i = bytes.findIndex(x => !f(x)); return i < 0 ? bytes : bytes.slice(i); };
//: (Int -> Bool) -> Bytes -> Bytes
exports.dropWhile = f => bytes => { let i = bytes.findIndex(x => !f(x)); return i < 0 ? exports.empty : bytes.slice(0, i); };
//: Int -> Int -> Bytes -> Bytes
exports.slice = start => stop => bytes => bytes.slice(start, stop);
//: Int -> Int -> Bytes -> Bytes
exports.slice = start => stop => bytes => bytes.slice(start, stop);
//: Bytes -> Bytes
exports.reverse = a => a.reverse();
//: (Int -> Bool) -> Bytes -> Bytes
exports.filter = f => a => a.filter(f);
//: (Int -> Int) -> Bytes -> Bytes
exports.map = f => a => a.map(f);
//: a -> (a -> Int -> a) -> Bytes -> a
exports.foldLeft = z => f => a => a.reduce((x, y) => f(x)(y), z);
//: a -> (a -> Int -> a) -> Bytes -> a
exports.foldRight = z => f => a => a.reduceRight((x, y) => f(x)(y), z);
