exports._fetchThen = f => configuration => new self.tsh.Task((w, t, c) => {
    var options = {};
    for(var k in configuration) if(Object.prototype.hasOwnProperty.call(configuration, k)) {
        options[k] = configuration[k];
    }
    if(options.mode === "proxy") {
        options.credentials = "omit"; // Don't send cookies etc. to third parties
        configuration.url = "/proxy/" + encodeURI(configuration.url);
        delete options.mode;
    }
    var headers = configuration.headers;
    if(headers) {
        options.headers = {};
        for(var i = 0; i < headers.length; i++) {
            options.headers[headers[i].key] = headers[i].value;
        }
    }
    var canceled = false;
    var controller = new AbortController();
    options.signal = controller.signal;
    try {
        fetch(configuration.url, options).then(response => {
            if(!canceled) {
                if(response.ok || options.check === false) {
                    try { return f(response).then(v => Promise.resolve(t(v))); } catch(e) { c(e) }
                } else {
                    c(new Error("HTTP error " + response.status + " on " +
                        (options.method || "GET") + " " + configuration.url));
                }
            }
        }, e => {
            if(!canceled) c(e)
        });
    } catch(e) {
        c(e)
    }
    return () => {
        canceled = true;
        controller.abort();
    }
});

exports.fetch = exports._fetchThen(r => Promise.resolve(r));

exports.fetchText = exports._fetchThen(r => r.text());
exports.fetchJson = exports._fetchThen(r => r.json().then(j => Promise.resolve(j)));
exports.fetchBytes = exports._fetchThen(r => r.arrayBuffer().then(b => Promise.resolve(new Uint8ClampedArray(b))));

exports._processResponse = f => response => new self.tsh.Task((w, t, c) => {
    var canceled = false;
    try {
        f(response).then(v => {
            if(!canceled) t(v)
        }, e => {
            if(!canceled) c(e)
        })
    } catch(e) {
        c(e)
    }
    return () => canceled = true;
});

exports.text = exports._processResponse(r => r.text());
exports.json = exports._processResponse(r => r.json().then(j => Promise.resolve(j)));
exports.bytes = exports._processResponse(r => r.arrayBuffer().then(b => Promise.resolve(new Uint8ClampedArray(b))));

exports.header = header => response => response.headers.get(header);

exports.ok = response => response.ok;
exports.redirected = response => response.redirected;
exports.status = response => response.status;
exports.statusText = response => response.statusText;
exports.type = response => response.type;
exports.url = response => response.url;
