exports.of_ = value => ({_task: (t, c) => {try { t(value) } catch(e) { c(e) }} });

exports.throw_ = error => ({_task: (t, c) => {try { c(error) } catch(e) { c(e) }} });

exports.catch_ = f => task => ({_task: (t, c) =>
    {try { task._task(t, error => {try { f(error)._task(t, c) } catch(e) { c(e) }}) } catch(e) { c(e) }}
});

exports.then_ = f => task => ({_task: (t, c) =>
    {try { task._task(v => {try { f(v)._task(t, c) } catch(e) { c(e) }}, c) } catch(e) { c(e) }}
});

exports.filter_ = f => task => ({_task: (t, c) =>
    {try { task._task(v => {try { if(f(v)) t(v) } catch(e) { c(e) }}, c) } catch(e) { c(e) }}
});

exports.scan_ = f => z => task => ({_task: (t, c) => {
    var state = z;
    task._task(v => {try {
        state = f(state)(v);
        t(state);
    } catch(e) { c(e) }}, c)
}});

exports.merge_ = task1 => task2 => ({_task: (t, c) => {
    try {
        task1._task(t, c);
        task2._task(t, c);
    } catch(e) { c(e) }
}});

exports.zipWith_ = f => task1 => task2 => ({_task: (t, c) => {
    var result1, result2;
    var ok1, ok2;
    task1._task(v => {
        ok1 = true;
        result1 = v;
        if(ok2 === true) {
            try {
                t(f(result1)(result2))
            } catch(e) {
                c(e)
            }
        }
    }, e => {
        ok1 = false;
        result1 = null;
        c(e);
    });
    task2._task(v => {
        ok2 = true;
        result2 = v;
        if(ok1 === true) {
            try {
                t(f(result1)(result2))
            } catch(e) {
                c(e)
            }
        }
    }, e => {
        ok2 = false;
        result2 = null;
        c(e);
    });
}});

exports.map_ = f => exports.then_(v => exports.of_(f(v)));

exports.sleep_ = s => ({_task: (t, c) =>
        void setTimeout(v => {try { t(void v) } catch(e) { c(e) }}, s * 1000)
});

exports.periodic_ = s => ({_task: (t, c) =>
    void setInterval(_ => {try { t(Date.now()) } catch(e) { c(e) }}, s * 1000)
});

exports.now_ = ({_task: (t, c) => {
    try { t(Date.now() * 0.001) } catch(e) { c(e) }
}});

exports.random_ = ({_task: (t, c) => {
    try { t(Math.random()) } catch(e) { c(e) }
}});

exports.log_ = message => ({_task: (t, c) => {
    try { t(void console.log(message)) } catch(e) { c(e) }
}});

exports.dir_ = message => ({_task: (t, c) => {
    try { t(void console.dir(message)) } catch(e) { c(e) }
}});
