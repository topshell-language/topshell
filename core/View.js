exports.table = rows => {
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
    return {_tag: ">view", html: {_tag: "table", children: [
        {_tag: ">attribute", key: "border", value: "1"},
        {_tag: "thead", children: {_tag: "tr", children: columns.map(header)}},
        {_tag: "tbody", children: rows.map(row)},
    ]}, value: rows};
};
