//: {user: String, host: String} -> Task a -> Task a
exports.do = config => task => new self.tsh.Task(async world => {
    let newWorld = Object.assign({}, world, {ssh: {config: config, user: config.user, host: config.host}});
    return await task.run(newWorld);
});
