exports.simple = data => {
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
