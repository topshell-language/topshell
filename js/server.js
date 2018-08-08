var http = require('http');
var url = require('url');
var utils = require('./utilities');
var actions = require('./actions');

var handler = (json, callback) => {
    var action = actions[json.action];
    if(action) {
        action(json.data, (err, data) => callback(err,
            JSON.stringify({data: data === undefined ? null : data})
        ));
    } else {
        callback("No such action");
    }
};

var server = http.createServer((request, response) => {
    var parts = url.parse(request.url);
 
    if(parts.pathname === '/execute' && request.method === 'POST') {

        utils.readJsonRequest(request, json => {
            handler(json, (err, result) => {
                if(err) utils.sendResponse(response, 'Error: ' + err, 500, {'Content-Type': 'text/plain'});
                else utils.sendResponse(response, result, 200, {'Content-Type': 'application/json'});
            });
        });
   
    } else {
        utils.sendResponse(response, "Not found", 404);
    }
});

server.listen(8080);
