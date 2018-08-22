exports.readText_ = path => ({_run: (w, t, c) => {
    var action = {action: "File.readText", data: {fileName: path}};
    var options = {method: "POST", body: JSON.stringify(action)};
    var canceled = false;
    var controller = new AbortController();
    options.signal = controller.signal;
    try {
        fetch("/execute", options)
            .then(r => {
                if(!r.ok) {
                    if(!canceled) c(new Error("Action error " + r.status + ": " + options.body));
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
}});
