exports.Lazy = function(compute) {
    this.compute = compute;
};

//: ({} -> a) -> Lazy a
exports.of = f => new exports.Lazy(f);

//: a -> Lazy a
exports.fromValue = x => {
    var lazy = new exports.Lazy(null);
    lazy.value = x;
    return lazy;
};

//: Lazy a -> a
exports.force = l => {
    if(!Object.prototype.hasOwnProperty.call(l, "value")) l.value = l.compute({});
    return l.value;
};
