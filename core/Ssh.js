exports.do_ = config => user => host => task => ({_run: (w, t, c) => {
    w = {...w, ssh: {config: config, user: user, host: host}};
    return task._run(w, t, c);
}});
