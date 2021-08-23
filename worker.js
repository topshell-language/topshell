self.tsh = {};

// echo "jsdom = require('jsdom')" > jsdom-export.js; browserify jsdom-export.js -o ../libraries/jsdom.js
importScripts("libraries/jsdom.js");
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
        this.run = run;
    }
    then(body) {
        return new self.tsh.Task(async world => {
            let v1 = (await this.run(world)).result;
            let v2 = (await body(v1).run(world)).result;
            return {result: v2};
        });
    }
    map(body) {
        return new self.tsh.Task(async world => {
            let v = (await this.run(world)).result;
            return {result: body(v)};
        });
    }
    catch(body) {
        return new self.tsh.Task(async world => {
            try {
                let v1 = (await this.run(world)).result;
                return {result: v1};
            } catch(e) {
                if(e === self.tsh.Task.abortedError) throw e;
                let v2 = (await body(e).run(world)).result;
                return {result: v2};
            }
        });
    }
    toHtml() {
        return {_tag: "span", children: ["task"]};
    }
};
self.tsh.Task.abortedError = new class {};
self.tsh.Task.simple = function(body) {
    return new self.tsh.Task(world => new Promise((resolvePromise, rejectPromise) =>
        body(x => resolvePromise({result: x}), rejectPromise, world)
    ));
};

self.tsh.Stream = class extends self.tsh.AbstractView {
    constructor(open) {
        super();
        this.open = open;
    }
    then(body) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            let o = await outer.next();
            while(!o.done) {
                let inner = body(o.value.result).open(world);
                let i = await inner.next();
                while(!i.done) {
                    yield {result: i.value.result};
                    i = await inner.next();
                }
                o = await outer.next();
            }
        });
    }
    filter(body) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            let o = await outer.next();
            while(!o.done) {
                let v = o.value;
                if(body(v.result)) yield v;
                o = await outer.next();
            }
        });
    }
    map(body) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            let o = await outer.next();
            while(!o.done) {
                yield {result: body(o.value.result)};
                o = await outer.next();
            }
        });
    }
    zip(f, stream) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let s1 = open(world);
            let s2 = stream.open(world);
            let n1 = await s1.next();
            let n2 = await s2.next();
            while(!n1.done && !n2.done) {
                yield {result: f(n1.value.result)(n2.value.result)};
                n1 = await s1.next();
                n2 = await s2.next();
            }
        });
    }
    latest(f, stream) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let s1 = open(world);
            let s2 = stream.open(world);
            let n1 = null;
            let n2 = null;
            let p1 = s1.next().then(n => { p1 = null; n1 = n });
            let p2 = s2.next().then(n => { p2 = null; n2 = n });
            await p1;
            await p2;
            while(!n1.done && !n2.done) {
                yield {result: f(n1.value.result)(n2.value.result)};
                if(p1 === null) p1 = s1.next().then(n => { p1 = null; n1 = n });
                if(p2 === null) p2 = s2.next().then(n => { p2 = null; n2 = n });
                await Promise.race([p1, p2]);
            }
        });
    }
    scan(x, f) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            var r = {result: x};
            let o = await outer.next();
            while(!o.done) {
                r = {result: f(r.result)(o.value.result)};
                yield r;
                o = await outer.next();
            }
        });
    }
    fold(x, f) {
        let open = this.open;
        return new self.tsh.Task(async world => {
            let outer = open(world);
            var r = x;
            let o = await outer.next();
            while(!o.done) {
                r = f(r)(o.value.result);
                o = await outer.next();
            }
            return {result: r};
        });
    }
    switchMap(body) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let s1 = open(world);
            let s2 = null;
            let p1 = null;
            let p2 = null;
            let done = false;
            let newController = null;
            let newWorld = null;
            function propagateAbort() { newController.abort() }
            function stopInner() {
                if(newController && newWorld) {
                    if(world.abortSignal) world.abortSignal.removeEventListener("abort", propagateAbort);
                    newController.abort();
                    newController = null;
                    newWorld = null;
                }
            }
            try {
                while(!done) {
                    if(p1 == null) {
                        p1 = s1.next().then(n => {
                            p1 = null;
                            p2 = null;
                            stopInner();
                            if(n.done) {
                                done = true;
                            } else {
                                let stream2 = body(n.value.result);
                                newController = new AbortController();
                                if(world.abortSignal) world.abortSignal.addEventListener("abort", propagateAbort);
                                newWorld = Object.assign({}, world, {abortSignal: newController.signal});
                                s2 = stream2.open(newWorld);
                            }
                            return {outer: true};
                        });
                    }
                    if(p2 == null && s2 != null) {
                        p2 = s2.next();
                    }
                    let n = await Promise.race([p1, p2].filter(p => p != null));
                    if(n.done) {
                        s2 = null;
                        p2 = null;
                    } else if(!n.outer) {
                        p2 = null;
                        yield n.value;
                    }
                }
            } finally {
                stopInner();
            }
        });
    }
    merge(stream) {
        let open = this.open;
        return new self.tsh.Stream(function(world) {
            let s1 = open(world);
            let s2 = stream.open(world);
            let p1 = null;
            let p2 = null;
            return {next() {
                if(p1 === null && s1 !== null) p1 = s1.next().then(n => { if(n.done) s1 = null; return {p: true, n: n}; });
                if(p2 === null && s2 !== null) p2 = s2.next().then(n => { if(n.done) s2 = null; return {p: false, n: n}; });
                let promises = [p1, p2].filter(p => p !== null);
                return promises.length === 0 ? {done: true} : Promise.race(promises).then(r => {
                    if(r.p) p1 = null;
                    else p2 = null;
                    return r.n.done ? this.next() : r.n;
                });
            }};
        });
    }
    take(count) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            for(let i = 0; i < count; i++) {
                let o = await outer.next();
                if(o.done) return;
                yield o.value;
            }
        });
    }
    drop(count) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            for(let i = 0; i < count; i++) {
                let o = await outer.next();
                if(o.done) return;
            }
            while(true) {
                let o = await outer.next();
                if(o.done) return;
                yield o.value;
            }
        });
    }
    takeWhile(condition) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            while(true) {
                let o = await outer.next();
                if(o.done || !condition(o.value.result)) return;
                yield o.value;
            }
        });
    }
    dropWhile(condition) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            let triggered = false;
            while(true) {
                let o = await outer.next();
                if(o.done) return;
                if(triggered || !condition(o.value.result)) {
                    triggered = true;
                    yield o.value;
                }
            }
        });
    }
    batch(size) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            let outer = open(world);
            let list = [];
            while(true) {
                let o = await outer.next();
                if(o.done) {
                    yield {result: list};
                    return;
                }
                list.push(o.value.result);
                if(list.length >= size) {
                    yield {result: list};
                    list = [];
                }
            }
        });
    }
    buffer(initial, aggregate, condition) {
        let open = this.open;
        return new self.tsh.Stream(world => {
            let outer = open(world);
            let isInitial = true;
            let state = initial;
            let done = false;
            let going = false;
            let promise = null;
            function go() {
                going = true;
                if(!done && (isInitial || !condition(state))) {
                    promise = outer.next().then(x => {
                        if(x.done) done = true; else {
                            isInitial = false;
                            state = aggregate(state)(x.value.result);
                            go();
                        }
                        return x;
                    });
                } else {
                    going = false;
                }
            }
            go();
            function next() {
                if(!isInitial) {
                    let result = state;
                    isInitial = true;
                    state = initial;
                    if(!going) go();
                    return Promise.resolve({done: false, value: {result: result}})
                }
                if(done) return Promise.resolve({done: true});
                if(!going) go();
                return promise.then(next);
            }
            return {next: next};
        });
    }
    catch(f) {
        let open = this.open;
        return new self.tsh.Stream(async function*(world) {
            try {
                let outer = open(world);
                while(true) {
                    let o = await outer.next();
                    if(o.done) return;
                    yield o.value;
                }
            } catch(e) {
                if(e === self.tsh.Task.abortedError) throw e;
                let outer = f(e).open(world);
                while(true) {
                    let o = await outer.next();
                    if(o.done) return;
                    yield o.value;
                }
            }
        });
    }
    toHtml() {
        return {_tag: "span", children: ["stream"]};
    }
};
self.tsh.Stream.forever = (x, f) => new self.tsh.Stream(async function*(world) {
    var r = {result: x};
    while(true) {
        r = await f(r.result).run(world);
        yield r
    }
});
self.tsh.Stream.once = task => new self.tsh.Stream(async function*(world) {
    yield await task.run(world);
});
self.tsh.Stream.ofList = a => new self.tsh.Stream(async function*(world) {
    for(var i = 0; i < a.length; i++) yield {result: a[i]};
});
self.tsh.Stream.empty = new self.tsh.Stream(async function*(world) {});

self.tsh.Lazy = class extends self.tsh.AbstractView {
    constructor(compute) {
        super();
        this.compute = compute;
    }
    toHtml() {
        return {_tag: "span", children: ["lazy"]};
    }
};

self.tsh.Dom = class extends self.tsh.AbstractView {
    constructor(list) {
        super();
        if(!Array.isArray(list)) throw "Expected a List Dom, but got " + list;
        this.list = list;
    }
    toHtml() {
        return {_tag: "span", children: ["dom"]};
    }
};

self.tsh.Regex = class extends self.tsh.AbstractView {
    constructor(pattern, flags) {
        super();
        this.pattern = pattern;
        this.flags = flags;
        this.global = null;
        this.nonGlobal = null;
    }
    cacheNonGlobal() {
        if(this.nonGlobal === null) this.nonGlobal = new RegExp(this.pattern, this.flags);
        return this.nonGlobal;
    }
    cacheGlobal(offset) {
        if(this.global === null || this.global.lastIndex !== offset) {
            this.global = new RegExp(this.pattern, this.flags + "g");
            this.global.lastIndex = offset;
        }
        return this.global;
    }
    toHtml() {
        return {_tag: "span", children: [
            "[regex: " + JSON.stringify(this.pattern) + ", flags: " + this.flags + "]"
        ]};
    }
};

self.tsh.toHtml = value => {
    if(value instanceof self.tsh.AbstractView) return value.toHtml();
    if(value instanceof Function) return {_tag: "span", children: ["function"]};
    if(value instanceof Uint8Array) return {_tag: "span", children: ["bytes"]};
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
    } else if(XMap.isInstance(value)) {
        result.push("[map: ");
        var pairs = XMap.toList(value);
        for(var i = 0; i < pairs.length; i++) {
            if(result.length > 1) result.push(", ");
            result.push(self.tsh.toHtml(pairs[i].key));
            result.push(" ~> ");
            result.push(self.tsh.toHtml(pairs[i].value));
        }
        result.push("]");
    } else if(XSet.isInstance(value)) {
        result.push("[set: ");
        var members = XSet.toList(value);
        for(var i = 0; i < members.length; i++) {
            if(result.length > 1) result.push(", ");
            result.push(self.tsh.toHtml(members[i]));
        }
        result.push("]");
    } else {
        var isConstructor =
            Object.prototype.hasOwnProperty.call(value, "_") &&
            typeof value._ === 'string' &&
            value._.match(/^[A-Z][a-zA-Z0-9]*$/g);
        if(isConstructor) {
            result.push(value._);
            for(var j = 1; Object.prototype.hasOwnProperty.call(value, "_" + j); j++) {
                var v = value["_" + j];
                var simple = !v || !Object.prototype.hasOwnProperty.call(v, "_1") || !v._;
                result.push(" ");
                if(!simple) result.push("(");
                result.push(self.tsh.toHtml(v));
                if(!simple) result.push(")");
            }
        } else {
            var isPair = false;
            if(Object.prototype.hasOwnProperty.call(value, "key") && Object.prototype.hasOwnProperty.call(value, "value")) {
                isPair = true;
                for(var p in value) if(Object.prototype.hasOwnProperty.call(value, p)) {
                    if(p !== "key" && p !== "value") isPair = false;
                }
            }
            if(isPair) {
                result.push(self.tsh.toHtml(value.key));
                result.push(" ~> ");
                if(value.value instanceof Object) result.push("(");
                result.push(self.tsh.toHtml(value.value));
                if(value.value instanceof Object) result.push(")");
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
self.tsh.some = v => ({_: "Some", _1: v});
self.tsh.maybe = v => v == null ? self.tsh.none : self.tsh.some(v);
self.tsh.isNone = v => v._ === "None";
self.tsh.isSome = v => v._ === "Some";

self.tsh.hexForBytes = (function() {
    var result = new Array(256);
    for(var i = 0; i < 256; i++) {
        result[i] = (i < 16 ? "0" : "") + i.toString(16);
    }
    return result;
})();
self.tsh.bytesForHex = (function() {
    var result = {};
    for(var i = 0; i < 256; i++) {
        result[self.tsh.hexForBytes[i]] = i;
        result[self.tsh.hexForBytes[i].toUpperCase()] = i;
    }
    return result;
})();

self.tsh.toHex = a => a.reduce((s, b) => s + self.tsh.hexForBytes[b], "");
self.tsh.ofHex = s => {
    var l = s.length / 2;
    var result = new Uint8Array(l);
    for(var i = 0; i < l; i += 1) {
        result[i] = self.tsh.bytesForHex[s[i * 2] + s[i * 2 + 1]];
    }
    return result;
};

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
        return m.then(f);
    } else if(m instanceof self.tsh.Stream) {
        return m.then(f);
    } else {
        console.error("Operator <- not supported for: " + m);
        throw "Operator <- not supported for: " + m;
    }
};

self.tsh.action = actionName => parameter => new self.tsh.Task(async world => {
    var action = {action: actionName, data: parameter, context: world};
    var options = {method: "POST", body: JSON.stringify(action)};
    function checkAborted() {
        if(world.abortSignal && world.abortSignal.aborted) throw self.tsh.Task.abortedError;
    }
    checkAborted();
    if(world.abortSignal) options.signal = world.abortSignal;
    console.log(actionName +
        (parameter.from != null ? " " + parameter.from : "") +
        (parameter.size != null ? " " + parameter.size : "") +
        (parameter.path != null ? " " + parameter.path : "") +
        (parameter.target != null ? " " + parameter.target : "") +
        (parameter.command != null ? " " + parameter.command : "") +
        (world.ssh != null ? " (" + world.ssh.user + "@" + world.ssh.host + ")" : "")
    );
    let result = await fetch("/execute", options);
    checkAborted();
    if(!result.ok) {
        let problem;
        try {
            problem = await result.text();
        } catch (_) {
            problem = "Action error " + result.status + ": " + options.body;
        }
        checkAborted();
        throw new Error(problem);
    } else if(actionName === "File.streamBytes") {
        checkAborted();
        return {result: result.body.getReader()};
    } else if(result.headers.get('Content-Type') === "application/octet-stream") {
        let bytes = new Uint8Array(await result.arrayBuffer());
        checkAborted();
        return {result: bytes};
    } else {
        let json = await result.json();
        checkAborted();
        return {result: json.data};
    }
});

self.tsh.loadImport = url => new self.tsh.Task.simple((t, e, w) => {
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
importScripts("./target/scala-2.12/topshell-opt.js");
