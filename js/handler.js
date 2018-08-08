var fs = require('fs');
var utils = require('./utilities');

var actions = {
  'File.readText': (json, callback) => {
    fs.readFile(json.fileName, 'utf8', callback);
  },
  'File.writeText': (json, callback) => {
    fs.writeFile(json.fileName, json.contents, 'utf8', callback);
  }
};

module.exports = (json, callback) => {
  var action = actions[json.action];
  if(action) {
    action(json.data, (err, data) => callback(err,
      JSON.stringify({data: data === undefined ? null : data})
    ));
  } else {
    callback("No such action");
  }
};
