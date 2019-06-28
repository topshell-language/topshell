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

//: List {key: String, value: String} -> Html
exports.attributes = function(pairs) {
    return new self.tsh.Tag({_tag: ">attributes", children: pairs});
};

//: List {key: String, value: String} -> Html
exports.styles = function(pairs) {
    return new self.tsh.Tag({_tag: ">styles", children: pairs});
};
