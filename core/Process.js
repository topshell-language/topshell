exports._fixConfig = (config, environment) => {
    config = self.tsh.removeUnderscores(config);
    if(config.environment) {
        config.env = config.env || {};
        config.environment.forEach(e => {
            config.env[e.key] = e.value;
        });
        delete config.environment;
    }
    if(environment) {
        config.env = config.env || {};
        environment.forEach(e => {
            config.env[e.key_] = e.value_;
        });
    }
    return config;
};

exports.run_ = config => path => arguments => {
    config = exports._fixConfig(config);
    return self.tsh.action("Process.run")({config: config, path: path, arguments : arguments});
};

exports.shell_ = config => command => {
    config = exports._fixConfig(config);
    return self.tsh.action("Process.shell")({config: config, command: command});
};
