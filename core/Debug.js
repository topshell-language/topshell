exports.log_ = v => { console.dir(v); return v };

exports.logBy_ = f => v => { console.dir(f(v)); return v };

exports.throw_ = m => { throw m };

exports.null_ = null;

exports.undefined_ = void 0;
