exports.read_ = text => self.tsh.underscores(JSON.parse(text));
exports.write_ = json => JSON.stringify(self.tsh.removeUnderscores(json));
exports.pretty_ = indentation => json => JSON.stringify(self.tsh.removeUnderscores(json), null, indentation);

