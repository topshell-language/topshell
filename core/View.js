//: (a -> Html) -> a -> View
exports.by = render => value => new self.tsh.View(render, value);

//: List String -> List a -> View
exports.tableBy = columns => exports.by(rows => {
    let header = c => ({_tag: "th", children: {_tag: ">text", text: "" + c.key}});
    let cell = r => c => ({_tag: "td", children: [self.tsh.toHtml(c.value(r))]});
    let row = r => ({_tag: "tr", children: columns.map(cell(r))});
    return new self.tsh.Tag({_tag: "table", children: [
        {_tag: ">attributes", children: [{key: "border", value: "1"}]},
        {_tag: "thead", children: {_tag: "tr", children: columns.map(header)}},
        {_tag: "tbody", children: rows.map(row)},
    ]});
});

//: List a -> View
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
    return new self.tsh.Tag({_tag: "table", children: [
        {_tag: ">attributes", children: [{key: "border", value: "1"}]},
        {_tag: "thead", children: {_tag: "tr", children: columns.map(header)}},
        {_tag: "tbody", children: rows.map(row)},
    ]});
});

//: List Float -> View
exports.bars = exports.by(data => {
    let attribute = (k, v) => ({_tag: ">attributes", children: [{key: k, value: v}]});
    let style = (k, v) => ({_tag: ">styles", children: [{key: k, value: v}]});
    let max = Math.max(...data);
    return new self.tsh.Tag({_tag: "div", children: data.map(d => ({
        _tag: "div", children: [
            style("background-color", "pink"),
            style("width", Math.max(0, d / max * 100) + "%"),
            style("height", "10px"),
            style("margin-bottom", "5px"),
            style("transition", "width 0.5s"),
            attribute("title", "" + d),
        ]
    }))});
});

//: String -> View
exports.text = exports.by(data => {
    let style = (k, v) => ({_tag: ">styles", children: [{key: k, value: v}]});
    return new self.tsh.Tag({_tag: "div", children: [
        style("white-space", "pre-wrap"),
        {_tag: ">text", text: "" + data}
    ]});
});

//: a -> View
exports.tree = exports.by(data => {
    let style = (k, v) => ({_tag: ">styles", children: [{key: k, value: v}]});
    if(Array.isArray(data) && data.length > 0) {
        return new self.tsh.Tag({_tag: "span", children: [
            {_tag: ">text", text: "["},
            {_tag: "div", children: [
                style("margin-left", "20px"),
                {_tag: "div", children: data.map(exports.tree).map((e, i) => (
                    {_tag: "div", children: i < data.length - 1 ? [e.toHtml(), {_tag: ">text", text: ", "}] : [e.toHtml()]}
                ))},
            ]},
            {_tag: ">text", text: "]"},
        ]});
    } else if(data && data.constructor === Object && Object.keys(data).length > 0) {
        let keys = Object.keys(data);
        return new self.tsh.Tag({_tag: "span", children: [
            {_tag: ">text", text: "{"},
            {_tag: "div", children: [
                style("margin-left", "20px"),
                {_tag: "div", children: keys.map((l, i) => {
                    var k = {_tag: ">text", text: /^[a-z][a-zA-Z0-9]*$/g.test(l) ? l : JSON.stringify(l)};
                    var e = exports.tree(data[l]);
                    return {_tag: "div", children: i < keys.length - 1 ?
                        [k, {_tag: ">text", text: ": "}, e.toHtml(), {_tag: ">text", text: ", "}] :
                        [k, {_tag: ">text", text: ": "}, e.toHtml()]
                    }
                })},
            ]},
            {_tag: ">text", text: "}"},
        ]});
    } else {
        return new self.tsh.Tag(self.tsh.toHtml(data));
    }
});

//: List (List a) -> View
exports.matrix = exports.by(data => {
    let rows = [];
    for(let i = 0; i < data.length; i++) {
        var row = [];
        for(let j = 0; j < data[i].length; j++) {
            row.push(data[i][j]);
        }
        rows.push(row);
    }
    let cell = c => ({_tag: "td", children: [
        {_tag: ">styles", children: [{key: "padding", value: "5px 10px"}, {key: "border", value: "1px solid #333"}]},
        self.tsh.toHtml(c)
    ]});
    let toRow = r => ({_tag: "tr", children: r.map(cell)});
    return new self.tsh.Tag({_tag: "table", children: [
        {_tag: ">attributes", children: [
            {key: "border", value: "0"}, {key: "cellspacing", value: "0"}, {key: "cellpadding", value: "0"}
        ]},
        {_tag: ">styles", children: [
            {key: "text-align", value: "center"}, {key: "border-collapse", value: "collapse"}
        ]},
        {_tag: "tbody", children: rows.map(toRow)},
    ]});
});
