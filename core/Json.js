//: String -> Json
exports.read = text => JSON.parse(text);
//: Json -> String
exports.write = json => JSON.stringify(json);
//: String -> Json -> String
exports.pretty = indentation => json => JSON.stringify(json, null, indentation);
//: Json -> a
exports.toAny = json => json;
//: a -> Json
exports.fromAny = json => json;
