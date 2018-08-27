exports.readText = path => self.tsh.action("File.readText")({path: path});
exports.writeText = path => contents => self.tsh.action("File.writeText")({path: path, contents: contents});
exports.list = path => self.tsh.action("File.list")({path: path});
exports.listStatus = path => self.tsh.action("File.listStatus")({path: path});
exports.status = path => self.tsh.action("File.status")({path: path});
