//: (b -> a) -> Lazy a
exports.of = f => new self.tsh.Lazy(f);

//: a -> Lazy a
exports.fromValue = x => {
    var lazy = new exports.Lazy(null);
    lazy.value = x;
    return lazy;
};

//: Lazy a -> a
exports.force = l => {
    if(!Object.prototype.hasOwnProperty.call(l, "value")) l.value = l.compute();
    return l.value;
};
