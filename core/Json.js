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
//: Json -> List Json
exports.list = json => json;
//: String -> Maybe Json
exports.get = key => json =>
    Object.prototype.hasOwnProperty.call(json, key) ? self.tsh.some(json[key]) : self.tsh.none;
//: Json -> List {key: String, value: Json}
exports.pairs = json => {
    var result = [];
    for(var k in json) if(Object.prototype.hasOwnProperty.call(json, k)) {
        result.push({key: k, value: json[k]})
    }
    return result;
};
