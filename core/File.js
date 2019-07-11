//: String -> Task String
exports.readText = path => self.tsh.action("File.readText")({path: path});
//: String -> String -> Task {}
exports.writeText = path => contents => self.tsh.action("File.writeText")({path: path, contents: contents});
//: String -> String -> Task {}
exports.appendText = path => contents => self.tsh.action("File.appendText")({path: path, contents: contents});
//: String -> Task Bytes
exports.readBytes = path => self.tsh.action("File.readBytes")({path: path});
//: String -> Bytes -> Task {}
exports.writeBytes = path => contents => self.tsh.action("File.writeBytes")({path: path, contents: self.tsh.toHex(contents)});
//: String -> Bytes -> Task {}
exports.appendBytes = path => contents => self.tsh.action("File.appendBytes")({path: path, contents: self.tsh.toHex(contents)});
//: Int -> Int -> String -> Task Bytes
exports.readByteRange = from => size => path => self.tsh.action("File.readByteRange")({path: path, from: from, size: size});
//: String -> String -> Task {}
exports.copy = fromPath => toPath => self.tsh.action("File.copy")({path: fromPath, target: toPath});
//: String -> String -> Task {}
exports.rename = fromPath => toPath => self.tsh.action("File.rename")({path: fromPath, target: toPath});
//: String -> Task {}
exports.delete = path => self.tsh.action("File.delete")({path: path});
//: String -> Task {}
exports.createDirectory = path => self.tsh.action("File.createDirectory")({path: path});
//: String -> Task {}
exports.deleteDirectory = path => self.tsh.action("File.deleteDirectory")({path: path});
//: String -> Task (List String)
exports.list = path => self.tsh.action("File.list")({path: path});
//: String -> Task (List {name: String, isFile: Bool, isDirectory: Bool})
exports.listStatus = path => self.tsh.action("File.listStatus")({path: path});
//: String -> Task {name: String, isFile: Bool, isDirectory: Bool}
exports.status = path => self.tsh.action("File.status")({path: path});
//: Int -> String -> Stream Bytes
exports.streamBytes = from => path => {
    let buffer = 1024 * 1024;
    return self.tsh.Stream.forever({from: from, bytes: null}, r => {
        return exports.readByteRange(r.from)(buffer)(path).map(bytes =>
            ({from: r.from + bytes.byteLength, bytes: bytes})
        )
    }).map(r => r.bytes).takeWhile(r => r.byteLength !== 0)
};
