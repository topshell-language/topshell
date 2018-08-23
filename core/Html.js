exports.of_ = self.tsh.toHtml;

exports.tag_ = function(tagName) { return function(children) {
    var c = Array.isArray(children) ? children : [children];
    return {_tag: tagName, children: c.map(exports.of_)};
}};

exports.text_ = function(text) {
    return {_tag: ">text", text: "" + text};
};

exports.attribute_ = function(key) { return function(value) {
    return {_tag: ">attribute", key_: "" + key, value_: "" + value, };
}};

exports.style_ = function(key) { return function(value) {
    return {_tag: ">style", key_: "" + key, value_: "" + value, };
}};

exports.view_ = render => value => ({_tag: ">view", html_: exports.of_(render(value)), value_: value});
