exports.log = v => { console.dir(v); return v };

exports.logBy = f => v => { console.dir(f(v)); return v };

exports.throw = m => { throw m };

exports.null = null;

exports.undefined = void 0;
