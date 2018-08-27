exports.read = text => JSON.parse(text);
exports.write = json => JSON.stringify(json);
exports.pretty = indentation => json => JSON.stringify(json, null, indentation);

