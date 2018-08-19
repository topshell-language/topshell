exports.of_ = value => ({_task: (t, c) => {try { t(value) } catch(e) { c(e) }} });
exports.throw_ = error => ({_task: (t, c) => {try { c(error) } catch(e) { c(e) }} });
exports.catch_ = catch_ => task => ({_task: (t, c) =>
    task._task(t, error => {try { catch_(error)._task(t, c) } catch(e) { c(e) }})
});
exports.then_ = then_ => task => ({_task: (t, c) =>
    task._task(v => {try { then_(v)._task(t, c) } catch(e) { c(e) }}, c)
});
exports.map_ = f => exports.then_(v => exports.of_(f(v)));

exports.all_ = tasks => ({_task: (t, c) => {
    var failed = 0;
    var pending = tasks.length;
    var result = new Array(pending);
    tasks.forEach((task, i) => {
        task._task(v => {
            result[i] = v;
            if(--pending === 0 && !failed) {
                try { t(result) } catch(e) { c(e) }
            }
        }, e => {
            --pending;
            if(++failed === 1) c(e);
        });
    });
}});

exports.race_ = tasks => ({_task: (t, c) => {
    var pending = tasks.length;
    tasks.forEach((task, i) => {
        task._task(v => {
            if(--pending === tasks.length - 1) {
                try { t(v) } catch(e) { c(e) }
            }
        }, e => {
            if(--pending === tasks.length - 1) {
                c(e)
            }
        });
    });
}});

exports.now_ = ({_task: (t, c) => {
    try { t(Date.now() * 0.001) } catch(e) { c(e) }
}});

exports.sleep_ = s => ({_task: (t, c) =>
    void setTimeout(v => {try { t(void v) } catch(e) { c(e) }}, s * 1000)
});

exports.log_ = message => ({_task: (t, c) => {
    try { t(void console.log(message)) } catch(e) { c(e) }
}});

exports.dir_ = message => ({_task: (t, c) => {
    try { t(void console.dir(message)) } catch(e) { c(e) }
}});
