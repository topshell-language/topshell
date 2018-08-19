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
