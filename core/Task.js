//: a -> Task a
exports.of = value => new self.tsh.Task((w, t, c) => {
    try {
        t(value)
    } catch(e) {
        c(e)
    }
});

//: String -> Task a
exports.throw = error => new self.tsh.Task((w, t, c) => {
    try {
        c(error)
    } catch(e) {
        c(e)
    }
});

//: (String -> Task a) -> Task a -> Task a
exports.catch = f => task => new self.tsh.Task((w, t, c) => {
    var cancel1 = null;
    try {
        var cancel2 = task._run(w, t, error => {
            try {
                if(cancel1 instanceof Function) cancel1();
                cancel1 = f(error)._run(w, t, c)
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
});

//: (a -> Task b) -> Task a -> Task b
exports.then = self.tsh.taskThen;

//: (a -> Bool) -> Task a -> Task a
exports.filter = f => task => new self.tsh.Task((w, t, c) => {
    try {
        return task._run(w, v => {
            try {
                if(f(v)) t(v)
            } catch(e) {
                c(e)
            }
        }, c)
    } catch(e) {
        c(e)
    }
});

//: (a -> b -> a) -> a -> Task b -> Task a
exports.scan = f => z => task => new self.tsh.Task((w, t, c) => {
    var state = z;
    return task._run(w, v => {
        try {
            state = f(state)(v);
            t(state);
        } catch(e) {
            c(e)
        }
    }, c)
});

//: Task a -> Task a -> Task a
exports.merge = task1 => task2 => new self.tsh.Task((w, t, c) => {
    try {
        var cancel1 = task1._run(w, t, c);
        var cancel2 = task2._run(w, t, c);
        return () => {
            if(cancel1 instanceof Function) cancel1();
            if(cancel2 instanceof Function) cancel2();
        }
    } catch(e) {
        c(e)
    }
});

//: Task a -> Task a -> Task a
exports.race = task1 => task2 => new self.tsh.Task((w, t, c) => {
    try {
        var cancel1 = task1._run(w, v => {
            if(cancel2 instanceof Function) cancel2();
            t(v)
        }, c);
        var cancel2 = task2._run(w, v => {
            if(cancel1 instanceof Function) cancel1();
            t(v)
        }, c);
        return () => {
            if(cancel1 instanceof Function) cancel1();
            if(cancel2 instanceof Function) cancel2();
        }
    } catch(e) {
        c(e)
    }
});

//: (a -> b -> c) -> Task a -> Task b -> Task c
exports.zipWith = f => task1 => task2 => new self.tsh.Task((w, t, c) => {
    var result1, result2;
    var ok1, ok2;
    var cancel1 = task1._run(w, v => {
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
    var cancel2 = task2._run(w, v => {
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
});

//: List (Task a) -> Task (List a)
exports.all = tasks => new self.tsh.Task((w, t, c) => {
    var results = new Array(tasks.length);
    var status = new Array(tasks.length);
    var cancels = new Array(tasks.length);
    var pending = tasks.length;
    tasks.forEach((_, i) => {
        cancels[i] = tasks[i]._run(w, v => {
            if(pending === 0) results = results.slice();
            results[i] = v;
            if(!status[i]) pending--;
            status[i] = true;
            if(pending === 0) {
                try {
                    t(results)
                } catch(e) {
                    c(e)
                }
            }
        }, e => {
            c(e);
        });
    });
    if(tasks.length === 0) {
        try {
            t([])
        } catch(e) {
            c(e)
        }
    }
    return () => {
        cancels.forEach(cancel => { if(cancel instanceof Function) cancel() });
    }
});

//: (a -> b) -> Task a -> Task b
exports.map = f => task => new self.tsh.Task((w, t, c) => {
    return task._run(w, v => {
        try {
            t(f(v))
        } catch(e) {
            c(e)
        }
    }, c);
});

//: Number -> Task {}
exports.sleep = s => new self.tsh.Task((w, t, c) => {
    var handle = setTimeout(_ => {
        try {
            t(void _)
        } catch(e) {
            c(e)
        }
    }, s * 1000);
    return () => clearTimeout(handle);
});

//: Number -> Task {}
exports.interval = s => new self.tsh.Task((w, t, c) => {
    try {
        t(void 0)
    } catch(e) {
        c(e)
    }
    var handle = setInterval(_ => {
        try {
            t(void 0)
        } catch(e) {
            c(e)
        }
    }, s * 1000);
    return () => clearInterval(handle);
});

//: Task Number
exports.now = new self.tsh.Task((w, t, c) => {
    try { t(Date.now() * 0.001) } catch(e) { c(e) }
});

//: Task Number
exports.random = new self.tsh.Task((w, t, c) => {
    try { t(Math.random()) } catch(e) { c(e) }
});

//: a -> Task {}
exports.log = message => new self.tsh.Task((w, t, c) => {
    try { t(void console.dir(message)) } catch(e) { c(e) }
});

//: (a -> Task b) -> Task a -> Task b
exports.flatMap = exports.then;
//: Task (Task a) -> Task a
exports.flatten = exports.flatMap(v => v);

// Proposal:
// Task.retry [2, 5, 10] task
// Retries a task after 2, 5, 10 seconds if it throws.
// Add jitter 50%-150% to each delay to decouple retries, eg. 10 means between 5 and 15 seconds.
