//: a -> Html
exports.of = v => new self.tsh.Tag(self.tsh.toHtml(v));

//: String -> List Html -> Html
exports.tag = function(tagName) { return function(children) {
    var c = Array.isArray(children) ? children : [children];
    return new self.tsh.Tag({_tag: tagName, children: c.map(self.tsh.toHtml)});
}};

//: String -> Html
exports.text = function(text) {
    return new self.tsh.Tag({_tag: ">text", text: "" + text});
};

//: String -> String -> Html
exports.attribute = function(key) { return function(value) {
    return new self.tsh.Tag({_tag: ">attribute", key: "" + key, value: "" + value, });
}};

//: String -> String -> Html
exports.style = function(key) { return function(value) {
    return new self.tsh.Tag({_tag: ">style", key: "" + key, value: "" + value, });
}};
