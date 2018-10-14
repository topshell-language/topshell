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
//: Json -> List {key: String, value: Json}
exports.pairs = json => {
    var result = [];
    for(var k in json) if(Object.prototype.hasOwnProperty.call(json, k)) {
        result.push({key: k, value: json[k]})
    }
    return result;
};
//: Number -> Json -> Maybe Json
exports.at = index => json =>
    index >= 0 && index < json.length ? self.tsh.some(json[index]) : self.tsh.none;
//: String -> Json -> Maybe Json
exports.get = key => json =>
    Object.prototype.hasOwnProperty.call(json, key) ? self.tsh.some(json[key]) : self.tsh.none;
//: Number -> Json -> Json
exports.atOrFail = index => json => {
    if(index >= 0 && index < json.length) return json[index]; else throw "Index out of bounds: " + index;
};
//: String -> Json -> Json
exports.getOrFail = key => json =>{
    if(Object.prototype.hasOwnProperty.call(json, key)) return json[key]; else throw "No such field: " + key;
};
