self.tsh = {};

importScripts("../modules.js");

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

self.tsh.Lazy = class extends self.tsh.AbstractView {
    constructor(compute) {
        super();
        this.compute = compute;
    }
    toHtml() {
        return {_tag: "span", children: ["lazy"]};
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
        var isConstructor =
            Object.prototype.hasOwnProperty.call(value, "_") &&
            typeof value._ === 'string' &&
            value._.match(/^[A-Z][a-zA-Z0-9]*$/g);
        if(isConstructor) {
            result.push(value._);
            if(Object.prototype.hasOwnProperty.call(value, "value")) {
                result.push(" ");
                result.push(self.tsh.toHtml(value["value"]));
            }
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
    }
    return {_tag: "span", children: result};
};

self.tsh.record = (m, r) => {
    for(var k in r) {
        if(Object.prototype.hasOwnProperty.call(r, k) && !Object.prototype.hasOwnProperty.call(m, k)) m[k] = r[k];
    }
    return m;
};

self.tsh.lookup = (r, f) => {
    return r != null && Object.prototype.hasOwnProperty.call(r, f) ? self.tsh.some(r[f]) : self.tsh.none;
};

self.tsh.none = {_: "None"};
self.tsh.some = v => ({_: "Some", value: v});
self.tsh.isNone = v => v._ === "None";
self.tsh.isSome = v => v._ === "Some";

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


importScripts("./tokenizer.js");
importScripts("./target/scala-2.12/topshell-fastopt.js");
