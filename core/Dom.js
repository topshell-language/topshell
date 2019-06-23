//: String -> Dom
exports.fromHtmlDocument = html => {
    return new self.tsh.Dom(new jsdom.JSDOM(html).window.document.documentElement);
};
//: String -> Dom
exports.fromHtml = html => {
    var nodes = jsdom.JSDOM.fragment(html);
    var result = Array.from(nodes.childNodes);
    return new self.tsh.Dom(result.length === 1 ? result[0] : result);
};
//: Dom -> String
exports.toHtml = dom => {
    var result = "";
    if(Array.isArray(dom.dom)) {
        for(var i = 0; i < dom.dom.length; i++) {
            result += dom.dom[i].outerHTML || "";
        }
    } else {
        result = dom.dom.outerHTML || "";
    }
    return result;
};
//: List Dom -> Dom
exports.fromList = list => new self.tsh.Dom(list.flatMap(d => Array.isArray(d.dom) ? d.dom : [d.dom]));
//: Dom -> List Dom
exports.toList = dom => {
    if(Array.isArray(dom.dom)) return dom.dom.map(d => new self.tsh.Dom(d));
    else return [dom];
};
//: String -> Dom -> Dom
exports.select = selector => dom => {
    var result;
    if(Array.isArray(dom.dom)) {
        result = dom.dom.flatMap(d => Array.from(d.querySelectorAll(selector)));
    } else {
        result = Array.from(dom.dom.querySelectorAll(selector));
    }
    return new self.tsh.Dom(result.length === 1 ? result[0] : result);
};
//: Dom -> String
exports.text = dom => {
    if(Array.isArray(dom.dom)) {
        return dom.dom.map(d => d.nodeType !== 8 ? d.textContent : "").join("");
    } else {
        return dom.dom.nodeType !== 8 ? dom.dom.textContent : "";
    }
};
//: Dom -> String
exports.comment = dom => {
    if(Array.isArray(dom.dom)) {
        return dom.dom.map(d => d.nodeType === 8 ? d.textContent : "").join("");
    } else {
        return dom.dom.nodeType === 8 ? dom.dom.textContent : "";
    }
};
//: String -> Dom -> [None, Some String]
exports.attribute = name => dom => {
    if(Array.isArray(dom.dom)) {
        for(var i = 0; i < dom.dom.length; i++) {
            var a = exports.attribute(name)(dom.dom[i].getAttribute != null ? dom.dom[i].getAttribute(name) : null);
            if(a != null) return self.tsh.some(a);
        }
        return self.tsh.none();
    } else {
        return self.tsh.maybe(dom.dom.getAttribute != null ? dom.dom.getAttribute(name) : null);
    }
};
//: Dom -> List {key: String, value: String}
exports.attributes = dom => {
    if(Array.isArray(dom.dom)) {
        return dom.dom.flatMap(d => d.attributes != null ?
            Array.from(d.attributes).map(a => ({key: a.name, value: a.value})) : []);
    } else {
        return dom.dom.attributes != null ?
            Array.from(dom.dom.attributes).map(a => ({key: a.name, value: a.value})) : [];
    }
};
//: Dom -> Dom
exports.children = dom => {
    var result;
    if(Array.isArray(dom.dom)) {
        result = dom.dom.flatMap(d => d.childNodes != null ? Array.from(d.childNodes) : []);
    } else {
        result = dom.dom.childNodes != null ? Array.from(dom.dom.childNodes) : [];
    }
    return new self.tsh.Dom(result.length === 1 ? result[0] : result);
};
//: (Dom -> Bool) -> Dom -> Dom
exports.filter = f => dom => {
    var result;
    if(Array.isArray(dom.dom)) {
        result = dom.dom.filter(d => f(new self.tsh.Dom(d)));
    } else {
        result = f(dom) ? dom : [];
    }
    return new self.tsh.Dom(result.length === 1 ? result[0] : result);
};
//: (Dom -> Dom) -> Dom -> Dom
exports.map = f => dom => {
    var result;
    if(Array.isArray(dom.dom)) {
        result = dom.dom.flatMap(d => { var r = f(new self.tsh.Dom(d)); return Array.isArray(r.dom) ? r.dom : [r.dom]});
    } else {
        result = f(dom).dom;
    }
    return new self.tsh.Dom(result.length === 1 ? result[0] : result);
};
//: Dom -> Boolean
exports.isText = dom => {
    return dom.dom.nodeType === 3;
};
//: Dom -> Boolean
exports.isElement = dom => {
    return dom.dom.nodeType === 1;
};
//: Dom -> Boolean
exports.isCData = dom => {
    return dom.dom.nodeType === 4;
};
//: Dom -> Boolean
exports.isComment = dom => {
    return dom.dom.nodeType === 8;
};
//: Dom -> Boolean
exports.isList = dom => {
    return Array.isArray(dom.dom);
};
//: Dom -> Int
exports.size = dom => {
    return Array.isArray(dom.dom) ? dom.dom.length : 1;
};
