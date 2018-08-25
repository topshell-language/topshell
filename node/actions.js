var fs = require('fs');
var path = require('path');
var child_process = require('child_process');
var utils = require('./utilities');

module.exports = {
    'File.readText': (json, callback) => {
        fs.readFile(json.path, 'utf8', callback);
    },
    'File.writeText': (json, callback) => {
        fs.writeFile(json.path, json.contents, 'utf8', callback);
    },
    'File.list': (json, callback) => {
        fs.readdir(json.path, callback);
    },
    'File.listStatus': (json, callback) => {
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
                                path: filePath,
                                isFile: stats.isFile(),
                                isDirectory: stats.isDirectory(),
                                status: stats
                            };
                            if(pending === 0) callback(void 0, results);
                        }
                    });
                });
            }
        });
    },
    'File.status': (json, callback) => {
        fs.stat(json.path, (error, stats) => {
            if(error) callback(error); else {
                callback(void 0, {
                    path: json.path,
                    isFile: stats.isFile(),
                    isDirectory: stats.isDirectory(),
                    status: stats
                });
            }
        });
    },
    'Process.run': (json, callback) => {
        child_process.execFile(json.path, json.arguments, json.config, (error, stdout, stderr) => {
            callback(error, {out: stdout, error: stderr})
        });
    },
    'Process.shell': (json, callback) => {
        child_process.exec(json.command, json.config, (error, stdout, stderr) => {
            callback(error, {out: stdout, error: stderr})
        });
    },
};
