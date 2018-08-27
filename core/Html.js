exports.of = self.tsh.toHtml;

exports.tag = function(tagName) { return function(children) {
    var c = Array.isArray(children) ? children : [children];
    return {_tag: tagName, children: c.map(exports.of)};
}};

exports.text = function(text) {
    return {_tag: ">text", text: "" + text};
};

exports.attribute = function(key) { return function(value) {
    return {_tag: ">attribute", key: "" + key, value: "" + value, };
}};

exports.style = function(key) { return function(value) {
    return {_tag: ">style", key: "" + key, value: "" + value, };
}};

exports.view = render => value => ({_tag: ">view", html: exports.of(render(value)), value: value});
