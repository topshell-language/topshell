exports.of_ = value => ({_event: (t, c) => {try { t(value) } catch(e) { c(e) }} });
exports.throw_ = error => ({_event: (t, c) => {try { c(error) } catch(e) { c(e) }} });
exports.catch_ = catch_ => event => ({_event: (t, c) =>
    event._event(t, error => {try { catch_(error)._event(t, c) } catch(e) { c(e) }})
});
exports.then_ = then_ => event => ({_event: (t, c) =>
    event._event(v => {try { then_(v)._event(t, c) } catch(e) { c(e) }}, c)
});
exports.map_ = f => exports.then_(v => exports.of_(f(v)));

exports.scan_ = f => z => event => ({_event: (t, c) => {
    var state = z;
    event._event(v => {try {
        state = f(state)(v);
        t(state);
    } catch(e) { c(e) }}, c)
}});

exports.all_ = events => ({_event: (t, c) => {
    var failed = 0;
    var pending = events.length;
    var result = new Array(pending);
    events.forEach((event, i) => {
        event._event(v => {
            if(result[i] === void 0) pending--;
            else if(pending <= 0) result = result.slice();
            result[i] = v;
            if(pending <= 0 && !failed) {
                try { t(result) } catch(e) { c(e) }
            }
        }, e => {
            --pending;
            if(++failed === 1) c(e);
        });
    });
}});

exports.any_ = events => ({_event: (t, c) => {
    var failed = false;
    events.forEach((events, i) => {
        events._event(v => {
            if(!failed) {
                try { t(v) } catch(e) { c(e) }
            }
        }, e => {
            if(!failed) {
                failed = true;
                c(e);
            }
        });
    });
}});

exports.interval_ = s => ({_event: (t, c) =>
    void setInterval(_ => {try { t(Date.now()) } catch(e) { c(e) }}, s * 1000)
});

exports.log_ = message => ({_event: (t, c) => {
    try { t(void console.log(message)) } catch(e) { c(e) }
}});

exports.dir_ = message => ({_event: (t, c) => {
    try { t(void console.dir(message)) } catch(e) { c(e) }
}});
