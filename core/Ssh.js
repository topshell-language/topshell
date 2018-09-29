exports.do = config => task => new self.tsh.Task((w, t, c) => {
    w = {...w, ssh: {config: config, user: config.user, host: config.host}};
    return task._run(w, t, c);
});
