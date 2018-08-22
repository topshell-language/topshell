exports.log_ = v => { console.dir(v); return v };

exports.logBy_ = f => v => { console.dir(f(v)); return v };

exports.throw_ = m => { throw m };

exports.runTaskNow_ = task => {
    if(self.runTaskNow_cancel instanceof Function) self.runTaskNow_cancel();
    self.runTaskNow_cancel = task._run({}, v => void v, e => console.error(e));
};
