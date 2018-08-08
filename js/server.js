var http = require('http');
var url = require('url');
var utils = require('./utilities');
var handler = require('./handler');

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
