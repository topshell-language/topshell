exports._action = actionName => parameter => ({_run: (w, t, c) => {
    var action = {action: actionName, data: parameter};
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
                        .then(j => {if(!canceled) return self.tsh.underscores(j.data)})
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


exports.readText_ = path => exports._action("File.readText")({path: path});
exports.list_ = path => exports._action("File.list")({path: path});
exports.listStatus_ = path => exports._action("File.listStatus")({path: path});
exports.status_ = path => exports._action("File.status")({path: path});
