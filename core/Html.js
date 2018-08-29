exports.of = v => new self.tsh.Tag(self.tsh.toHtml(v));

exports.tag = function(tagName) { return function(children) {
    var c = Array.isArray(children) ? children : [children];
    return new self.tsh.Tag({_tag: tagName, children: c.map(self.tsh.toHtml)});
}};

exports.text = function(text) {
    return new self.tsh.Tag({_tag: ">text", text: "" + text});
};

exports.attribute = function(key) { return function(value) {
    return new self.tsh.Tag({_tag: ">attribute", key: "" + key, value: "" + value, });
}};

exports.style = function(key) { return function(value) {
    return new self.tsh.Tag({_tag: ">style", key: "" + key, value: "" + value, });
}};
