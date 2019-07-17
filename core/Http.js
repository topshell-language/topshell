exports._fetchThen = f => configuration => new self.tsh.Task(async world => {
    var options = Object.assign({}, configuration);
    var url = configuration.url;
    if(options.mode === "proxy") {
        options.credentials = "omit"; // Don't send cookies etc. to third parties
        url = "/proxy/" + encodeURI(url);
        delete options.mode;
    }
    var headers = configuration.headers;
    if(headers) {
        options.headers = {};
        for(var i = 0; i < headers.length; i++) {
            options.headers[headers[i].key] = headers[i].value;
        }
    }
    if(world.abortSignal) options.signal = world.abortSignal;
    let response = await fetch(url, options);
    if(world.abortSignal && world.abortSignal.aborted) throw self.tsh.Task.abortedError;
    if(response.ok || options.check === false) {
        let result = await f(response);
        if(world.abortSignal && world.abortSignal.aborted) throw self.tsh.Task.abortedError;
        return {result: result};
    } else {
        throw new Error("HTTP error " + response.status + " on " + (options.method || "GET") + " " + url);
    }
});

//: c -> Task Http | c ~ {url : String, ?method : String, ?mode : String, ?body : String, ?check : Bool, ?headers : List {key: String, value: String}}
exports.fetch = exports._fetchThen(r => r);

//: c -> Task String | c ~ {url : String, ?method : String, ?mode : String, ?body : String, ?check : Bool, ?headers : List {key: String, value: String}}
exports.fetchText = exports._fetchThen(r => r.text());
//: c -> Task Json | c ~ {url : String, ?method : String, ?mode : String, ?body : String, ?check : Bool, ?headers : List {key: String, value: String}}
exports.fetchJson = exports._fetchThen(r => r.json());
//: c -> Task Bytes | c ~ {url : String, ?method : String, ?mode : String, ?body : String, ?check : Bool, ?headers : List {key: String, value: String}}
exports.fetchBytes = exports._fetchThen(r => r.arrayBuffer().then(b => new Uint8Array(b)));

exports._processResponse = f => response => new self.tsh.Task(async world => {
    let result = await f(response);
    if(world.abortSignal && world.abortSignal.aborted) throw self.tsh.Task.abortedError;
    return {result: result};
});

//: Http -> Task String
exports.text = exports._processResponse(r => r.text());
//: Http -> Task Json
exports.json = exports._processResponse(r => r.json().then(j => j));
//: Http -> Task Bytes
exports.bytes = exports._processResponse(r => r.arrayBuffer().then(b => new Uint8Array(b)));

//: String -> Http -> [None, Some String]
exports.header = header => response => response.headers.get(header);

//: Http -> Bool
exports.ok = response => response.ok;
//: Http -> Bool
exports.redirected = response => response.redirected;
//: Http -> Int
exports.status = response => response.status;
//: Http -> String
exports.statusText = response => response.statusText;
//: Http -> String
exports.type = response => response.type;
//: Http -> String
exports.url = response => response.url;
