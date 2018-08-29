exports.do = config => user => host => task => new self.tsh.Task((w, t, c) => {
    w = {...w, ssh: {config: config, user: user, host: host}};
    return task._run(w, t, c);
});
