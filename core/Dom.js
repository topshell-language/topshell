//: String -> Dom
exports.fromHtmlDocument = html => {
    return new self.tsh.Dom([new jsdom.JSDOM(html).window.document.documentElement]);
};
//: String -> Dom
exports.fromHtml = html => {
    var nodes = jsdom.JSDOM.fragment(html);
    var result = Array.from(nodes.childNodes);
    return new self.tsh.Dom(result);
};
//: Dom -> String
exports.toHtml = dom => {
    var result = "";
    for(var i = 0; i < dom.list.length; i++) {
        result += dom.list[i].outerHTML || "";
    }
    return result;
};
//: List Dom -> Dom
exports.fromList = list => new self.tsh.Dom(list.flatMap(d => d.list));
//: Dom -> List Dom
exports.toList = dom => dom.list.map(d => new self.tsh.Dom([d]));
//: String -> Dom -> Dom
exports.select = selector => dom => new self.tsh.Dom(dom.list.flatMap(d => Array.from(d.querySelectorAll(selector))));
//: Dom -> String
exports.text = dom => dom.list.map(d => d.nodeType !== 8 ? d.textContent : "").join("");
//: Dom -> String
exports.comment = dom => dom.list.map(d => d.nodeType === 8 ? d.textContent : "").join("");
//: Dom -> String
exports.tagName = dom => {
    for(var i = 0; i < dom.list.length; i++) {
        var a = dom.list[i].tagName;
        if(a != null) return a.toLowerCase();
    }
    return "";
};
//: String -> Dom -> [None, Some String]
exports.attribute = name => dom => {
    for(var i = 0; i < dom.list.length; i++) {
        var a = dom.list[i].getAttribute != null ? dom.list[i].getAttribute(name) : null;
        if(a != null) return self.tsh.some(a);
    }
    return self.tsh.none;
};
//: Dom -> List {key: String, value: String}
exports.attributes = dom => {
    return dom.list.flatMap(d =>
        d.attributes != null ? Array.from(d.attributes).map(a => ({key: a.name, value: a.value})) : []
    );
};
//: Dom -> Dom
exports.children = dom => new self.tsh.Dom(dom.list.flatMap(d => d.childNodes != null ? Array.from(d.childNodes) : []));
//: (Dom -> Bool) -> Dom -> Dom
exports.filter = f => dom => new self.tsh.Dom(dom.list.filter(d => f(new self.tsh.Dom([d]))));
//: (Dom -> Dom) -> Dom -> Dom
exports.map = f => dom => new self.tsh.Dom(dom.list.flatMap(d => f(new self.tsh.Dom([d])).list));
//: Int -> Dom -> Dom
exports.take = i => dom => new self.tsh.Dom(dom.list.slice(0, i));
//: Int -> Dom -> Dom
exports.drop = i => dom => new self.tsh.Dom(dom.list.slice(i));
//: Int -> Dom -> Dom
exports.takeLast = i => dom => new self.tsh.Dom(dom.list.slice(-i));
//: Int -> Dom -> Dom
exports.dropLast = i => dom => new self.tsh.Dom(dom.list.slice(0, -i));
//: Dom -> Boolean
exports.isText = dom => {
    return dom.list.length === 1 && dom.list[0].nodeType === 3;
};
//: Dom -> Boolean
exports.isElement = dom => {
    return dom.list.length === 1 && dom.list[0].nodeType === 1;
};
//: Dom -> Boolean
exports.isCData = dom => {
    return dom.list.length === 1 && dom.list[0].nodeType === 4;
};
//: Dom -> Boolean
exports.isComment = dom => {
    return dom.list.length === 1 && dom.list[0].nodeType === 8;
};
//: Dom -> Boolean
exports.isList = dom => {
    return dom.list.length !== 1;
};
//: Dom -> Boolean
exports.isEmpty = dom => {
    return dom.list.length === 0;
};
//: Dom -> Int
exports.size = dom => {
    return dom.list.length;
};
