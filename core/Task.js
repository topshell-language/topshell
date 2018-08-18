exports.of_ = value => ({_task:
    context => Promise.resolve(value)
});

exports.then_ = callback => task => ({_task:
    context => task._task(context).then(result => callback(result)._task(context))
});

exports.map_ = callback => task => ({_task:
    context => task._task(context).then(result => Promise.resolve(callback(result)))
});

exports.runNow_ = task => task._task(null);
