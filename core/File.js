exports.readText_ = path => self.tsh.action("File.readText")({path: path});
exports.list_ = path => self.tsh.action("File.list")({path: path});
exports.listStatus_ = path => self.tsh.action("File.listStatus")({path: path});
exports.status_ = path => self.tsh.action("File.status")({path: path});
