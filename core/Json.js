exports.read_ = text => JSON.parse(text);
exports.write_ = text => JSON.stringify(text);
exports.pretty_ = indentation => text => JSON.stringify(text, null, indentation);

