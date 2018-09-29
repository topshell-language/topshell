exports._fixConfig = (config, environment) => {
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
            config.env[e.key] = e.value;
        });
    }
    return config;
};

exports.run = config => {
    config = exports._fixConfig(config);
    return self.tsh.action("Process.run")({config: config, path: config.command, arguments : config.arguments});
};

exports.shell = config => {
    config = exports._fixConfig(config);
    return self.tsh.action("Process.shell")({config: config, command: config.command});
};
