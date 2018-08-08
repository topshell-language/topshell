var fs = require('fs');
var utils = require('./utilities');

module.exports = {
    'File.readText': (json, callback) => {
        fs.readFile(json.fileName, 'utf8', callback);
    },
    'File.writeText': (json, callback) => {
        fs.writeFile(json.fileName, json.contents, 'utf8', callback);
    }
};
