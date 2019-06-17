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
//: Json -> Map String Json
exports.toMap = json => {
    var result = XMap.empty;
    for(var k in json) if(Object.prototype.hasOwnProperty.call(json, k)) {
        result = XMap.add(k, json[k], result);
    }
    return result;
};
//: Map String Json -> Json
exports.fromMap = map => { var r = {}; XMap.toList(map).forEach(p => r[p.key] = p.value); return r; };
//: Int -> Json -> [None, Some Json]
exports.at = index => json =>
    index >= 0 && index < json.length ? self.tsh.some(json[index]) : self.tsh.none;
//: String -> Json -> [None, Some Json]
exports.get = key => json =>
    Object.prototype.hasOwnProperty.call(json, key) ? self.tsh.some(json[key]) : self.tsh.none;
//: Int -> Json -> Json
exports.atOrFail = index => json => {
    if(index >= 0 && index < json.length) return json[index]; else throw "Index out of bounds: " + index;
};
//: String -> Json -> Json
exports.getOrFail = key => json =>{
    if(Object.prototype.hasOwnProperty.call(json, key)) return json[key]; else throw "No such field: " + key;
};
