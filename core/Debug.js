exports.log_ = v => { console.log(v); return v };
exports.dir_ = v => { console.dir(v); return v };

exports.logThen_ = v => r => { console.log(v); return r };
exports.dirThen_ = v => r => { console.dir(v); return r };

exports.crash_ = m => { throw m };
