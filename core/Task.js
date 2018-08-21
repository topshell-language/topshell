exports.of_ = value => ({_run: (t, c) => {
    try {
        t(value)
    } catch(e) {
        c(e)
    }
}});

exports.throw_ = error => ({_run: (t, c) => {
    try {
        c(error)
    } catch(e) {
        c(e)
    }
}});

exports.catch_ = f => task => ({_run: (t, c) => {
    var cancel1 = null;
    try {
        var cancel2 = task._run(t, error => {
            try {
                if(cancel1 instanceof Function) cancel1();
                cancel1 = f(error)._run(t, c)
            } catch(e) {
                c(e)
            }
        })
    } catch(e) {
        c(e)
    }
    return () => {
        if(cancel2 instanceof Function) cancel2();
        if(cancel1 instanceof Function) cancel1();
    };
}});

// Remember to copy this definition into Emitter if changed
exports.then_ = f => task => ({_run: (t, c) => {
    var cancel1 = null;
    try {
        var cancel2 = task._run(v => {
            try {
                if(cancel1 instanceof Function) cancel1();
                cancel1 = f(v)._run(t, c);
            } catch(e) {
                c(e)
            }
        }, c);
    } catch(e) {
        c(e);
    }
    return () => {
        if(cancel2 instanceof Function) cancel2();
        if(cancel1 instanceof Function) cancel1();
    };
}});

exports.filter_ = f => task => ({_run: (t, c) => {
    try {
        return task._run(v => {
            try {
                if(f(v)) t(v)
            } catch(e) {
                c(e)
            }
        }, c)
    } catch(e) {
        c(e)
    }
}});

exports.scan_ = f => z => task => ({_run: (t, c) => {
    var state = z;
    return task._run(v => {
        try {
            state = f(state)(v);
            t(state);
        } catch(e) {
            c(e)
        }
    }, c)
}});

exports.merge_ = task1 => task2 => ({_run: (t, c) => {
    try {
        var cancel1 = task1._run(t, c);
        var cancel2 = task2._run(t, c);
        return () => {
            if(cancel1 instanceof Function) cancel1();
            if(cancel2 instanceof Function) cancel2();
        }
    } catch(e) {
        c(e)
    }
}});

exports.zipWith_ = f => task1 => task2 => ({_run: (t, c) => {
    var result1, result2;
    var ok1, ok2;
    var cancel1 = task1._run(v => {
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
    var cancel2 = task2._run(v => {
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
    return () => {
        if(cancel1 instanceof Function) cancel1();
        if(cancel2 instanceof Function) cancel2();
    }
}});

exports.map_ = f => exports.then_(v => exports.of_(f(v)));

exports.sleep_ = s => ({_run: (t, c) => {
    var handle = setTimeout(_ => {
        try {
            t(void _)
        } catch(e) {
            c(e)
        }
    }, s * 1000);
    return () => clearInterval(handle);
}});

exports.interval_ = s => ({_run: (t, c) => {
    var handle = setInterval(_ => {
        try {
            t(void _)
        } catch(e) {
            c(e)
        }
    }, s * 1000);
    return () => clearTimeout(handle);
}});

exports.now_ = ({_run: (t, c) => {
    try { t(Date.now() * 0.001) } catch(e) { c(e) }
}});

exports.random_ = ({_run: (t, c) => {
    try { t(Math.random()) } catch(e) { c(e) }
}});

exports.log_ = message => ({_run: (t, c) => {
    try { t(void console.log(message)) } catch(e) { c(e) }
}});

exports.dir_ = message => ({_run: (t, c) => {
    try { t(void console.dir(message)) } catch(e) { c(e) }
}});
