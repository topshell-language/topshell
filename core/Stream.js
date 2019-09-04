//: a -> Stream a
exports.of = x => self.tsh.Stream.ofList([x]);
//: List a -> Stream a
exports.ofList = self.tsh.Stream.ofList;

//: Int -> Stream a
exports.counter = a => new self.tsh.Stream(async function*(world) {
    var x = a;
    while(true) {
        yield {result: x};
        x += 1;
    }
});

//: Task a -> Stream a
exports.once = task => new self.tsh.Stream(async function*(world) {yield (await task.run(world))});
//: a -> (a -> Task a) -> Stream a
exports.forever = x => f => self.tsh.Stream.forever(x, f);
//: Stream a
exports.never = (function() {
    let p = Promise.race([]);
    return new self.tsh.Stream(async function*(world) { await p; });
})();
//: Stream a
exports.empty = self.tsh.Stream.empty;

//: (a -> b) -> Stream a -> Stream b
exports.map = f => s => s.map(f);
//: (a -> Bool) -> Stream a -> Stream a
exports.filter = f => s => s.filter(f);

//: Stream (Stream a) -> Stream a
exports.flatten = s => s.then(x => x);
//: (a -> Stream b) -> Stream a -> Stream b
exports.flatMap = f => s => s.then(f);

//: Stream (Stream a) -> Stream a
exports.switch = s => s.switchMap(x => x);
//: (a -> Stream b) -> Stream a -> Stream b
exports.switchMap = f => s => s.switchMap(f);

//: b -> (b -> a -> b) -> Stream a -> Task b
exports.fold = x => f => s => s.fold(x, f);
//: b -> (b -> a -> b) -> Stream a -> Stream b
exports.scan = x => f => s => s.scan(x, f);

//: Int -> Stream a -> Stream a
exports.take = n => s => s.take(n);
//: Int -> Stream a -> Stream a
exports.drop = n => s => s.drop(n);
//: (a -> Bool) -> Stream a -> Stream a
exports.takeWhile = f => s => s.takeWhile(f);
//: (a -> Bool) -> Stream a -> Stream a
exports.dropWhile = f => s => s.dropWhile(f);


//: List (Stream a) -> Stream a
exports.merge = l => l.length === 0 ? exports.empty : l.length === 1 ? l[0] : l.reduce((s1, s2) => s1.merge(s2));

//: (a -> b -> c) -> Stream a -> Stream b -> Stream c
exports.latest = f => s1 => s2 => s1.latest(f, s2);
//: (a -> b -> c) -> Stream a -> Stream b -> Stream c
exports.zip = f => s1 => s2 => s1.zip(f, s2);

//: Int -> Stream a -> Stream a
exports.buffer = n => s => exports.untilEmpty(s.buffer([], l => x => l.concat([x]), l => l.length >= n));
//: b -> (b -> a -> b) -> (b -> Bool) -> Stream a -> Stream b
exports.bufferBy = x => f => g => s => s.buffer(x, f, g);

//: Int -> Stream a -> Stream (List a)
exports.batch = n => s => s.batch(n);
//: Int -> Stream a -> Stream (List a)
exports.window = n => s => s.scan([], l => x => { l = l.slice(l.length < n ? 0 : 1); l.push(x); return l });

//: String -> Stream a
exports.throw = e => new self.tsh.Stream(async function*(_) {throw e});
//: (String -> Stream a) -> Stream a -> Stream a
exports.catch = f => s => s.catch(f);

//: Stream a -> Task (List a)
exports.drain = exports.fold([])(l => x => l.concat([x]));
//: Stream a -> Task [None, Some a]
exports.last = exports.fold(self.tsh.none)(_ => x => self.tsh.some(x));

//: Stream [None, Some a] -> Stream a
exports.untilNone = s => s.takeWhile(o => self.tsh.isSome(o)).map(o => o._1);
//: Stream (List a) -> Stream a
exports.untilEmpty = s => s.takeWhile(l => l.length !== 0).then(self.tsh.Stream.ofList);

//: Float -> Stream Float
exports.interval = duration => new self.tsh.Stream(async function*(world) {
    let t1 = 0;
    while(true) {
        let t2 = Date.now() * 0.001;
        let delta = t2 - t1;
        if(delta >= duration) {
            t1 = t2;
            yield {result: t1};
        } else {
            await new Promise(resolve => setTimeout(resolve, (duration - delta) * 1000));
            t1 = Date.now() * 0.001;
            yield {result: t1};
        }
    }
});
