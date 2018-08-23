var fs = require('fs');
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
    'File.status': (json, callback) => {
        fs.stat(json.path, (error, stats) => {
            if(error) callback(error); else {
                callback(void 0, {
                    isFile: stats.isFile(),
                    isDirectory: stats.isDirectory(),
                    status: stats
                });
            }
        });
    },
};
