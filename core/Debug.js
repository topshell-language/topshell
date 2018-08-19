exports.log_ = v => { console.log(v); return v };
exports.dir_ = v => { console.dir(v); return v };

exports.logBy_ = f => v => { console.log(f(v)); return v };
exports.dirBy_ = f => v => { console.dir(f(v)); return v };

exports.throw_ = m => { throw m };

exports.runTaskNow_ = task => {task._task(v => void v, e => {throw e}); return task};

exports.runEventNow_ = event => {event._event(v => void v, e => {throw e}); return event};
