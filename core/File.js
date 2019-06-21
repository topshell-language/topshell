//: String -> Task String
exports.readText = path => self.tsh.action("File.readText")({path: path});
//: String -> String -> Task {}
exports.writeText = path => contents => self.tsh.action("File.writeText")({path: path, contents: contents});
//: String -> String -> Task {}
exports.appendText = path => contents => self.tsh.action("File.appendText")({path: path, contents: contents});
//: String -> Task (List String)
exports.list = path => self.tsh.action("File.list")({path: path});
//: String -> Task (List {name: String, isFile: Bool, isDirectory: Bool})
exports.listStatus = path => self.tsh.action("File.listStatus")({path: path});
//: String -> Task {name: String, isFile: Bool, isDirectory: Bool}
exports.status = path => self.tsh.action("File.status")({path: path});
