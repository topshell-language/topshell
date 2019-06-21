var fs = require('fs');
var path = require('path');
var child_process = require('child_process');


module.exports = {
    'File.readText': (json, context, callback) => {
        fs.readFile(json.path, 'utf8', callback);
    },
    'File.writeText': (json, context, callback) => {
        fs.writeFile(json.path, json.contents, 'utf8', callback);
    },
    'File.appendText': (json, context, callback) => {
        fs.appendFile(json.path, json.contents, 'utf8', callback);
    },
    'File.readBytes': (json, context, callback) => {
        fs.readFile(json.path, (err, data) => callback(err, data != null ? data.toString('hex') : data));
    },
    'File.writeBytes': (json, context, callback) => {
        fs.writeFile(json.path, Buffer.from(json.contents, 'hex'), callback);
    },
    'File.appendBytes': (json, context, callback) => {
        fs.appendFile(json.path, Buffer.from(json.contents, 'hex'), callback);
    },
    'File.list': (json, context, callback) => {
        fs.readdir(json.path, callback);
    },
    'File.listStatus': (json, context, callback) => {
        fs.readdir(json.path, (outerError, files) => {
            if(outerError) callback(outerError); else {
                var pending = files.length;
                var failed = false;
                var results = new Array(pending);
                files.forEach((file, i) => {
                    var filePath = path.join(json.path, file);
                    fs.stat(filePath, (error, stats) => {
                        if(error) {
                            failed = true;
                            callback(error);
                        } else if(!failed) {
                            pending--;
                            results[i] = {
                                name: file,
                                isFile: stats.isFile(),
                                isDirectory: stats.isDirectory()
                            };
                            if(pending === 0) callback(void 0, results);
                        }
                    });
                });
            }
        });
    },
    'File.status': (json, context, callback) => {
        fs.stat(json.path, (error, stats) => {
            if(error) callback(error); else {
                callback(void 0, {
                    name: path.basename(json.path),
                    isFile: stats.isFile(),
                    isDirectory: stats.isDirectory(),
                    status: stats
                });
            }
        });
    },
    'Process.run': (json, context, callback) => {
        let child = child_process.execFile(json.path, json.arguments, json.config, (error, stdout, stderr) => {
            if(json.config.check !== false) callback(error, {out: stdout, error: stderr});
            else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
        });
        child.stdin.end(json.config.in || "");
    },
    'Process.shell': (json, context, callback) => {
        let child = child_process.exec(json.command, json.config, (error, stdout, stderr) => {
            if(json.config.check !== false) callback(error, {out: stdout, error: stderr});
            else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
        });
        child.stdin.end(json.config.in || "");
    },
};
