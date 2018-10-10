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

//: c -> Task Http | c.url : String | c.?method : String | c.?mode : String | c.?body : String | c.?check : Bool | c.?headers : List {key: String, value: String}
exports.fetch = exports._fetchThen(r => Promise.resolve(r));

//: c -> Task String | c.url : String | c.?method : String | c.?mode : String | c.?body : String | c.?check : Bool | c.?headers : List {key: String, value: String}
exports.fetchText = exports._fetchThen(r => r.text());
//: c -> Task Json | c.url : String | c.?method : String | c.?mode : String | c.?body : String | c.?check : Bool | c.?headers : List {key: String, value: String}
exports.fetchJson = exports._fetchThen(r => r.json().then(j => Promise.resolve(j)));
//: c -> Task Bytes | c.url : String | c.?method : String | c.?mode : String | c.?body : String | c.?check : Bool | c.?headers : List {key: String, value: String}
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

//: Http -> Task String
exports.text = exports._processResponse(r => r.text());
//: Http -> Task Json
exports.json = exports._processResponse(r => r.json().then(j => Promise.resolve(j)));
//: Http -> Task Bytes
exports.bytes = exports._processResponse(r => r.arrayBuffer().then(b => Promise.resolve(new Uint8ClampedArray(b))));

//: String -> Http -> Maybe String
exports.header = header => response => response.headers.get(header);

//: Http -> Bool
exports.ok = response => response.ok;
//: Http -> Bool
exports.redirected = response => response.redirected;
//: Http -> Number
exports.status = response => response.status;
//: Http -> String
exports.statusText = response => response.statusText;
//: Http -> String
exports.type = response => response.type;
//: Http -> String
exports.url = response => response.url;
