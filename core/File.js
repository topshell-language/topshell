exports.readText_ = path => ({_run: (w, t, c) => {
    var action = {action: "File.readText", data: {fileName: path}};
    var options = {method: "POST", body: JSON.stringify(action)};
    try {
        fetch("/execute", options)
            .then(r => {
                if(!r.ok) {
                    c(new Error("Action error " + r.status + ": " + options.body));
                } else {
                    return Promise.resolve(r)
                        .then(r => r.json())
                        .then(j => j.data)
                        .then(t, c)
                }
            })
    } catch(e) {
        c(e)
    }
}});
