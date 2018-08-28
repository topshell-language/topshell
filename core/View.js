exports.by = render => value => ({_tag: ">view", html: self.tsh.toHtml(render(value)), value: value});

exports.tableBy = columns => exports.by(rows => {
    let header = c => ({_tag: "th", children: {_tag: ">text", text: "" + c.key}});
    let cell = r => c => ({_tag: "td", children: [self.tsh.toHtml(c.value(r))]});
    let row = r => ({_tag: "tr", children: columns.map(cell(r))});
    return {_tag: "table", children: [
        {_tag: ">attribute", key: "border", value: "1"},
        {_tag: "thead", children: {_tag: "tr", children: columns.map(header)}},
        {_tag: "tbody", children: rows.map(row)},
    ]};
});

exports.table = exports.by(rows => {
    let seen = {};
    for(let i = 0; i < rows.length; i++) {
        for(let k of Object.keys(rows[i])) {
            seen[k] = true;
        }
    }
    let columns = Array.from(Object.keys(seen)).sort();
    let header = c => ({_tag: "th", children: {_tag: ">text", text: "" + c}});
    let cell = r => c => ({_tag: "td", children: [
        Object.prototype.hasOwnProperty.call(r, c) ? self.tsh.toHtml(r[c]) : {_tag: ">text", text: ""}
    ]});
    let row = r => ({_tag: "tr", children: columns.map(cell(r))});
    return {_tag: "table", children: [
        {_tag: ">attribute", key: "border", value: "1"},
        {_tag: "thead", children: {_tag: "tr", children: columns.map(header)}},
        {_tag: "tbody", children: rows.map(row)},
    ]};
});

exports.bars = data => {
    let attribute = (k, v) => ({_tag: ">attribute", key: k, value: v});
    let style = (k, v) => ({_tag: ">style", key: k, value: v});
    let max = Math.max(...data);
    return {_tag: "div", children: data.map(d => ({
        _tag: "div", children: [
            style("background-color", "pink"),
            style("width", Math.max(0, d / max * 100) + "%"),
            style("height", "10px"),
            style("margin-bottom", "5px"),
            style("transition", "width 0.5s"),
            attribute("title", "" + d),
        ]
    }))};
};

exports.text = data => {
    let style = (k, v) => ({_tag: ">style", key: k, value: v});
    return {_tag: "div", children: [
        style("white-space", "pre-wrap"),
        {_tag: ">text", text: "" + data}
    ]};
};

exports.tree = data => {
    let style = (k, v) => ({_tag: ">style", key: k, value: v});
    if(Array.isArray(data) && data.length > 0) {
        return {_tag: "div", children: [
            {_tag: ">text", text: "["},
            {_tag: "div", children: [
                style("margin-left", "10px"),
                {_tag: "div", children: data.map(exports.tree).map((e, i) => (
                    {_tag: "div", children: i < data.length - 1 ? [e, {_tag: ">text", text: ", "}] : [e]}
                ))},
            ]},
            {_tag: ">text", text: "]"},
        ]};
    } else if(data != null && typeof data === "object" && Object.keys(data).length > 0 && Object.keys(data).every(s => !s.startsWith("_tag") && !s.startsWith("_view"))) {
        return {_tag: "div", children: [
            {_tag: ">text", text: "{"},
            {_tag: "div", children: [
                style("margin-left", "10px"),
                {_tag: "div", children: Object.keys(data).map((l, i) => {
                    var k = {_tag: ">text", text: l};
                    var e = exports.tree(data[l]);
                    return {_tag: "div", children: i < data.length - 1 ?
                        [k, {_tag: ">text", text: ": "}, e, {_tag: ">text", text: ", "}] :
                        [k, {_tag: ">text", text: ": "}, e]
                    }
                })},
            ]},
            {_tag: ">text", text: "}"},
        ]};
    } else {
        return self.tsh.toHtml(data);
    }
};
