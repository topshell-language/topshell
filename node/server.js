var http = require('http');
var url = require('url');
var fs = require('fs');
var utils = require('./utilities');
var actions = require('./actions');
var ssh_actions = require('./ssh_actions');
var core_modules = require('./core_modules');

var httpProxy;
try {
    httpProxy = require('http-proxy');
} catch(e) {
    console.log("HTTP proxying disabled. " + (e.code === 'MODULE_NOT_FOUND' ? "To enable, npm install http-proxy" : e.message))
}

var proxy = httpProxy ? httpProxy.createProxyServer({
    followRedirects: true,
    ignorePath: true,
    changeOrigin: true
}) : null;

if(proxy) {
    proxy.on('error', (error, request, response) => {
        response.writeHead(500, {
            'Content-Type': 'text/plain'
        });
        response.end('' + error);
    });
}

var handler = (json, callback) => {
    var isSsh = json.context.ssh && ssh_actions[json.action];
    var action = isSsh || json.action === 'File.readByteRange' ? ssh_actions[json.action] : actions[json.action];
    if(action) {
        try {
            action(json.data, json.context, (err, data) => callback(err,
                Buffer.isBuffer(data) ? data : JSON.stringify({data: data === undefined ? null : data})
            ));
        } catch(e) {
            callback(e);
        }
    } else {
        callback("No such action");
    }
};

var server = http.createServer((request, response) => {

    var parts = url.parse(request.url);

    if(parts.pathname && parts.pathname.startsWith('/proxy/')) {
        var target = parts.pathname.slice('/proxy/'.length) + (parts.search || "");
        if(proxy) proxy.web(request, response, {target: decodeURI(target)});
        else utils.sendResponse(response, 'Proxying is disabled', 500, {'Content-Type': 'text/plain'});
        return;
    }

    var base = "/topshell/";
 
    if(parts.pathname === '/execute' && request.method === 'POST') {

        utils.readJsonRequest(request, json => {
            handler(json, (err, result) => {
                var problem = err ? (err.message ? err.message : "" + err) : "";
                let bin = Buffer.isBuffer(result);
                if(err) utils.sendResponse(response, problem, 500, {'Content-Type': 'text/plain'});
                else if(bin) utils.sendResponse(response, result, 200, {'Content-Type': 'application/octet-stream'});
                else utils.sendResponse(response, result, 200, {'Content-Type': 'application/json'});
            });
        });

    } else if(parts.pathname === '/modules.js' && request.method === 'GET') {

        core_modules.findCoreModules((e, m) => {
            var result = core_modules.toJs(m);
            utils.sendResponse(response, result, 200, {'Content-Type': 'application/javascript'});
        });

    } else if(parts.pathname.startsWith(base) && request.method === 'GET') {
        var path = parts.pathname.slice(base.length);
        if(path.includes("..")) throw 'Illegal path: ' + path;
        path = "../" + path;
        var fastOptPath = '../target/scala-2.12/topshell-fastopt.js';
        if(path === '../target/scala-2.12/topshell-opt.js' && fs.existsSync(fastOptPath)) path = fastOptPath;
        var stream = fs.createReadStream(path);
        if(path.endsWith(".js")) response.setHeader("Content-Type", "application/javascript");
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
