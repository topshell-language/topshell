exports.isTag_ = function(tag) { return tag instanceof exports.Tag; };

exports.tag_ = function(tagName) { return function(attributes) { return function(children) {
    return new exports.Tag(tagName, attributes, children);
}}};

exports.Tag = function(tagName, attributes, children) {
    this.tagName = tagName;
    this.attributes = attributes;
    this.children = children;
};



exports.isVisual_ = function(v) { return v instanceof exports.Visual; };

exports.visual_ = function(options) { return new exports.Visual(options); };

exports.Visual = function(options) {
    this.state = options.state_;
    this.render = options.render_;
};

exports.Visual.prototype._tsh_visual_marker = true;

exports.Visual.prototype.setHtml = function(container) {
    if(container == null) return;
    function flatEach(array, body) {
        for(var i = 0; i < array.length; i++) {
            if(Array.isArray(array[i])) flatEach(array[i], body);
            else if(array[i] != null) body(array[i])
        }
    }
    function go(node) {
        if(node instanceof exports.Tag) {
            var e = document.createElement(node.tagName);
            for(var k in node.attributes) if(node.attributes.hasOwnProperty(k)) {
                e.setAttribute(k.replace("_", ""), node.attributes[k]);
            }
            if(Array.isArray(node.children)) {
                flatEach(node.children, function(n) {
                    var c = go(n);
                    if(c != null) e.appendChild(c);
                });
            } else {
                var child = go(node.children);
                if(child != null) e.appendChild(child);
            }
            return e;
        } else if(typeof node === "string" || typeof node === "number" || typeof node === "boolean") {
            return document.createTextNode(node);
        } else if(node == null) {
            return null;
        } else {
            throw "Can't convert to HTML: " + node;
        }
    }
    while(container.firstChild) container.removeChild(container.firstChild);
    var html;
    try { html = go(this.render(this.state)); } catch(e) { html = document.createTextNode(e + ""); }
    container.appendChild(html);
};

