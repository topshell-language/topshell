var http = require('http');
var url = require('url');
var fs = require('fs');
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

    var base = "/topshell/";
 
    if(parts.pathname === '/execute' && request.method === 'POST') {

        utils.readJsonRequest(request, json => {
            handler(json, (err, result) => {
                var problem = err ? (err.message ? err.message : "" + err) : "";
                if(err) utils.sendResponse(response, problem, 500, {'Content-Type': 'text/plain'});
                else utils.sendResponse(response, result, 200, {'Content-Type': 'application/json'});
            });
        });

    } else if(parts.pathname.startsWith(base) && request.method === 'GET') {
        var path = parts.pathname.slice(base.length);
        if(path.includes("..")) throw 'Illegal path: ' + path;
        var stream = fs.createReadStream("../" + path);
        stream.on('error', function() {
            response.writeHead(404);
            response.end();
        });
        stream.pipe(response);
    } else {
        utils.sendResponse(response, "Not found", 404);
    }
});

var port = 7070;

server.listen(port);

console.log("http://localhost:" + port + "/topshell/index.html");
