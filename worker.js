self.tsh = {};

self.tsh.symbols = {};


self.tsh.AbstractView = class {};

self.tsh.View = class extends self.tsh.AbstractView {
    constructor(render, value) {
        super();
        this.render = render;
        this.value = value;
    }
    toHtml() {
        return this.render(this.value).toHtml();
    }
};

self.tsh.Tag = class extends self.tsh.AbstractView {
    constructor(tag) {
        super();
        this.tag = tag;
    }
    toHtml() {
        return this.tag;
    }
};

self.tsh.Task = class extends self.tsh.AbstractView {
    constructor(run) {
        super();
        this._run = run;
    }
    toHtml() {
        return {_tag: "span", children: ["task"]};
    }
};

self.tsh.toHtml = value => {
    if(value instanceof self.tsh.AbstractView) return value.toHtml();
    if(value instanceof Function) return {_tag: "span", children: ["function"]};
    if(value instanceof Uint8ClampedArray) return {_tag: "span", children: ["bytes"]};
    if(value instanceof Response) return {_tag: "span", children: ["response"]};
    if(typeof value === 'string') return {_tag: "span", children: [JSON.stringify(value)]};
    if(typeof value === 'number') return {_tag: "span", children: [JSON.stringify(value)]};
    if(value === void 0) return {_tag: "span", children: ["undefined"]};
    if(value === null) return {_tag: "span", children: ["null"]};
    if(value === true) return {_tag: "span", children: ["true"]};
    if(value === false) return {_tag: "span", children: ["false"]};
    var result = [];
    if(Array.isArray(value)) {
        result.push("[");
        for(var i = 0; i < value.length; i++) {
            if(result.length > 1) result.push(", ");
            result.push(self.tsh.toHtml(value[i]));
        }
        result.push("]");
    } else {
        result.push("{");
        for(var k in value) if(Object.prototype.hasOwnProperty.call(value, k)) {
            if(result.length > 1) result.push(", ");
            var l = k.match(/^[a-z][a-zA-Z0-9]*$/g) ? k : JSON.stringify(k);
            result.push(l + ": ");
            result.push(self.tsh.toHtml(value[k]));
        }
        result.push("}");
    }
    return {_tag: "span", children: result};
};

self.tsh.record = (m, r) => {
    for(var k in r) {
        if(Object.prototype.hasOwnProperty.call(r, k) && !Object.prototype.hasOwnProperty.call(m, k)) m[k] = r[k];
    }
    return m;
};

self.tsh.taskThen = f => task => new self.tsh.Task((w, t, c) => {
    var cancel1 = null;
    try {
        var cancel2 = task._run(w, v => {
            try {
                if(cancel1 instanceof Function) cancel1();
                cancel1 = f(v)._run(w, t, c);
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
});

self.tsh.then = (m, f) => {
    if(Array.isArray(m)) {
        var result = [];
        for(var i = 0; i < m.length; i++) {
            var a = f(m[i]);
            for(var j = 0; j < a.length; j++) {
                result.push(a[j]);
            }
        }
        return result;
    } else if(m instanceof self.tsh.Task) {
        return self.tsh.taskThen(f)(m);
    } else {
        console.error("Operator <- not supported for: " + m);
        throw "Operator <- not supported for: " + m;
    }
};

self.tsh.action = actionName => parameter => new self.tsh.Task((w, t, c) => {
    var action = {action: actionName, data: parameter, context: w};
    var options = {method: "POST", body: JSON.stringify(action)};
    var canceled = false;
    var controller = new AbortController();
    options.signal = controller.signal;
    try {
        fetch("/execute", options)
            .then(r => {
                if(!r.ok) {
                    if(!canceled) return r.text().then(
                        problem => {if(!canceled) c(new Error(problem))},
                        _ => {if(!canceled) c(new Error("Action error " + r.status + ": " + options.body))}
                    );
                } else {
                    return Promise.resolve(r)
                        .then(r => {if(!canceled) return r.json()})
                        .then(j => {if(!canceled) return j.data})
                        .then(v => {if(!canceled) t(v)}, e => {if(!canceled) c(e)})
                }
            })
    } catch(e) {
        c(e)
    }
    return () => {
        canceled = true;
        controller.abort();
    }
});

self.tsh.loadImport = url => new self.tsh.Task((w, t, e) => {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.onload = function() {
        if(xhr.status === 200) {
            var f = new Function('exports', xhr.responseText);
            var exported = {};
            f(exported);
            t(exported);
        } else {
            e('Could not load module');
        }
    };
    xhr.send();
});

self.tsh.runLines = (emit, fromLine, toLine) => {

    let symbols = self.tsh.symbols;

    for(let name of Object.keys(symbols)) {
        if(symbols[name].fromLine <= toLine && symbols[name].toLine >= fromLine && symbols[name].effect) {
            self.tsh.startSymbol(emit, name);
        }
    }

};

self.tsh.startSymbol = (emit, name) => {

    let symbols = self.tsh.symbols;

    if(symbols[name].cancel instanceof Function) symbols[name].cancel();

    self.tsh.clearDependants(emit, name);

    if(symbols[name].run) {
        symbols[name].start = false;
        symbols[name].cancel = symbols[name].run({}, v => {
            symbols[name].done = true;
            symbols[name].result = v;
            if(symbols[name].kind === "import") v = new self.tsh.Tag({_tag: "span", children: []});
            emit(name, v, void 0);
            self.tsh.proceed(emit, name);
        }, e => {
            emit(name, void 0, e);
        });
    } else {
        symbols[name].start = true;
    }

};

self.tsh.emitPending = (emit, name) => {

    let symbols = self.tsh.symbols;

    let pending = symbols[name].dependencies.filter(d => {
        return !symbols[d].done;
    });

    emit(name, new self.tsh.Tag({_tag: "span", children: [
        {_tag: ">style", key: "color", value: "#c0d0e0"},
        {_tag: ">style", key: "font-family", value: "'Montserrat', sans-serif"},
        "Pending: " + pending.map(s => s.slice(0, -1)).join(", ")
    ]}), void 0);

};

self.tsh.clearDependants = (emit, name) => {

    let symbols = self.tsh.symbols;

    for(let k of Object.keys(symbols)) if(symbols[k].dependencies.includes(name)) {
        if(symbols[k].fromLine > symbols[name].toLine) {
            if(symbols[k].cancel instanceof Function) symbols[k].cancel();
            symbols[k].cancel = null;
            symbols[k].computed = false;
            symbols[k].started = false;
            symbols[k].done = false;
            self.tsh.emitPending(emit, k);
            self.tsh.clearDependants(emit, k);
        }
    }

};

self.tsh.proceed = (emit, previousName) => {

    let symbols = self.tsh.symbols;

    if(previousName) self.tsh.clearDependants(emit, previousName);

    for(let name of Object.keys(symbols)) {
        if(!symbols[name].started && symbols[name].error == null) {
            if(symbols[name].dependencies.every(d => symbols[d].done || symbols[d].fromLine > symbols[name].toLine)) {
                symbols[name].started = true;
                try {
                    let result = symbols[name].compute(symbols);
                    symbols[name].computed = true;
                    if(symbols[name].effect) {
                        if(result._run instanceof Function) {
                            symbols[name].run = result._run;
                        } else {
                            emit(name, void 0, "Not a task: " + result);
                        }
                    }
                    if(symbols[name].run) {
                        if(symbols[name].start) {
                            self.tsh.startSymbol(emit, name);
                        } else {
                            emit(name, new self.tsh.Tag({_tag: "span", children: [
                                {_tag: ">style", key: "color", value: "#c0d0e0"},
                                {_tag: ">style", key: "font-family", value: "'Montserrat', sans-serif"},
                                "Ready to run ",
                                {_tag: "span", children: [
                                    {_tag: ">style", key: "font-style", value: "italic"},
                                    "(Ctrl + Enter)"
                                ]},
                            ]}), void 0);
                        }
                    } else {
                        symbols[name].done = true;
                        symbols[name].result = result;
                        emit(name, symbols[name].result, void 0);
                        self.tsh.proceed(emit, name);
                    }
                } catch(e) {
                    emit(name, void 0, e);
                }
            } else {
                self.tsh.emitPending(emit, name);
            }
        }
    }

};

self.tsh.setSymbols = (emit, newSymbols) => {

    let symbols = self.tsh.symbols;

    for(let name of Object.keys(symbols)) if(!newSymbols[name]) {
        if(symbols[name].cancel instanceof Function) symbols[name].cancel();
        delete symbols[name];
    }

    for(let name of Object.keys(newSymbols)) {
        if(symbols[name] && symbols[name].cancel instanceof Function) symbols[name].cancel();
        symbols[name] = newSymbols[name];
        if(symbols[name].error) emit(name, void 0, symbols[name].error);
    }

    self.tsh.proceed(emit, null);

};

importScripts("./target/scala-2.12/topshell-fastopt.js");
