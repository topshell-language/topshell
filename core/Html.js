exports.of_ = function(value) {
    if(value === undefined) return {_tag: "span", attributes: {}, children: ["Undefined"]};
    if(value === null) return {_tag: "span", attributes: {}, children: ["Null"]};
    if(value._tag !== undefined) return value;
    if(typeof value === 'string') return {_tag: "span", attributes: {}, children: [JSON.stringify(value)]};
    if(typeof value === 'number') return {_tag: "span", attributes: {}, children: [JSON.stringify(value)]};
    var result = [];
    if(Array.isArray(value)) {
        result.push("[");
        for(var i = 0; i < value.length; i++) {
            if(result.length > 1) result.push(", ");
            result.push(exports.of_(value[i]));
        }
        result.push("]");
    } else {
        result.push("{");
        for(var k in value) if(Object.prototype.hasOwnProperty.call(value, k)) {
            if(result.length > 1) result.push(", ");
            result.push(k.replace("_", "") + ": ");
            result.push(exports.of_(value[k]));
        }
        result.push("}");
    }
    return {_tag: "span", attributes: {}, children: result};
};

exports.tag_ = function(tagName) { return function(attributes) { return function(children) {
    var c = Array.isArray(children) ? children : [children];
    return {_tag: tagName, attributes: attributes, children: c.map(exports.of_)};
}}};

exports.text_ = function(text) {
    return {_tag: ">text", text: "" + text};
};
