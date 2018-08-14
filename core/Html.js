exports.tag_ = function(tagName) { return function(attributes) { return function(children) {
    return {_tag: tagName, attributes: attributes, children: children};
}}};
