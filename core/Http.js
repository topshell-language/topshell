exports.fetch_ = configuration => url => ({_task: (t, c) => {
    var options = {};
    for(var k in configuration) if(Object.prototype.hasOwnProperty.call(configuration, k)) {
        options[k.replace("_", "")] = configuration[k];
    }
    try { fetch(url, options).then(t, c) } catch(e) { c(e) }
}});

exports.text_ = response => ({_task: (t, c) => {
    try { response.text().then(t, c) } catch(e) { c(e) }
}});

exports.headers_ = response => response.headers;
exports.ok_ = response => response.ok;
exports.redirected_ = response => response.redirected;
exports.status_ = response => response.status;
exports.statusText_ = response => response.statusText;
exports.type_ = response => response.type;
