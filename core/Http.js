exports.fetch_ = configuration => url => ({_task: context => fetch(url, configuration)});
