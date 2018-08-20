exports.readText_ = path => ({_task: (t, c) => {
    var action = {action: "File.readText", data: {fileName: path}};
    var options = {method: "POST", body: JSON.stringify(action)};
    try { fetch("/execute", options).then(r => r.json()).then(j => j.data).then(t, c) } catch(e) { c(e) }
}});
