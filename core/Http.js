exports.fetch_ = configuration => url => ({_run: (w, t, c) => {
    var options = {};
    for(var k in configuration) if(Object.prototype.hasOwnProperty.call(configuration, k)) {
        options[k.replace("_", "")] = configuration[k];
    }
    var canceled = false;
    var controller = new AbortController();
    options.signal = controller.signal;
    console.dir(options);
    try {
        fetch(url, options).then(response => {
            if(!canceled) {
                if(response.ok || options.check === false) t(response);
                else c(new Error("HTTP error " + response.status + " on " + (options.method || "GET") + " " + url));
            }
        }, e => {
            if(!canceled) c(e)
        })
    } catch(e) {
        c(e)
    }
    return () => {
        canceled = true;
        controller.abort();
    }
}});

exports.text_ = response => ({_run: (w, t, c) => {
    var canceled = false;
    try {
        response.text().then(v => {
            if(!canceled) t(v)
        }, e => {
            if(!canceled) c(e)
        })
    } catch(e) {
        c(e)
    }
    return () => canceled = true;
}});

exports.header_ = header => response => response.headers.get(header);

exports.ok_ = response => response.ok;
exports.redirected_ = response => response.redirected;
exports.status_ = response => response.status;
exports.statusText_ = response => response.statusText;
exports.type_ = response => response.type;
