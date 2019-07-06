//: a -> Task a
exports.of = value => self.tsh.Task.simple(resolve => resolve(value));

//: (a -> b) -> Task a -> Task b
exports.map = f => task => task.map(f);

//: (a -> Task b) -> Task a -> Task b
exports.flatMap = f => task => task.then(f);

//: Task (Task a) -> Task a
exports.flatten = task => task.then(task => task);

//: r1 -> Task r2 | {r1 : Task x} {r2 : x}
exports.concurrent = structure => new self.tsh.Task(world => {
    var tasks = [];
    for(var k in structure) if(Object.prototype.hasOwnProperty.call(structure, k)) {
        tasks.push(structure[k].run(world));
    }
    return Promise.all(tasks).map(results => {
        var result = Array.isArray(structure) ? [] : {};
        var i = 0;
        for(var k in structure) if(Object.prototype.hasOwnProperty.call(structure, k)) {
            result[k] = results[i].result;
            i++;
        }
        return result;
    });
});

//: Task a -> Task a -> Task a
exports.race = task1 => task2 => new self.tsh.Task(async world => {
    let controller = new AbortController();
    function propagateAbort() { controller.abort() }
    if(world.abortSignal) world.abortSignal.addEventListener("abort", propagateAbort);
    let newWorld = Object.assign({}, world, {abortSignal: controller.signal});
    try {
        return await Promise.race([task1.run(newWorld), task2.run(newWorld)]);
    } finally {
        if(world.abortSignal) world.abortSignal.removeEventListener("abort", propagateAbort);
        controller.abort();
    }
});

//: List (Task a) -> Task (List a)
exports.sequence = tasks => new self.tsh.Task(async world => {
    let results = [];
    for(var i = 0; i < tasks.length; i++) results.push((await tasks[i].run(world)).result);
    return {result: results};
});

//: String -> Task a
exports.throw = error => self.tsh.Task.simple((resolve, reject) => reject(error));

//: (String -> Task a) -> Task a -> Task a
exports.catch = f => task => task.catch(f);

//: Float -> Task {}
exports.sleep = d => self.tsh.Task.simple(resolve => setTimeout(_ => resolve({}), d * 1000));

//: Task Float
exports.now = self.tsh.Task.simple(resolve => resolve(Date.now() * 0.001));

//: Task Float
exports.random = new self.tsh.Task(world => {
    return Promise.resolve({result: Math.random()});
});

//: a -> Task {}
exports.log = message => new self.tsh.Task(world => {
    console.dir(message);
    return Promise.resolve({result: {}});
});
