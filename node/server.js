var http = require('http');
var url = require('url');
var fs = require('fs');
var path = require('path');
var process = require('process');
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
    var action = isSsh ? ssh_actions[json.action] : actions[json.action];
    if(action) {
        try {
            action(json.data, json.context, (err, data) => callback(err, data));
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
                if(err) {
                    utils.sendResponse(response, problem, 500, {'Content-Type': 'text/plain'});
                } else if(Buffer.isBuffer(result)) {
                    utils.sendResponse(response, result, 200, {'Content-Type': 'application/octet-stream'});
                } else if(result != null && typeof result.pipe === "function") {
                    response.setHeader("Content-Type", 'application/octet-stream');
                    result.pipe(response);
                } else {
                    let json = JSON.stringify({data: result === undefined ? {} : result});
                    utils.sendResponse(response, json, 200, {'Content-Type': 'application/json'});
                }
            });
        });

    } else if(parts.pathname === '/modules.js' && request.method === 'GET') {

        core_modules.findCoreModules((e, m) => {
            var result = core_modules.toJs(m);
            utils.sendResponse(response, result, 200, {'Content-Type': 'application/javascript'});
        });

    } else if(parts.pathname.startsWith(base) && request.method === 'GET') {
        var file = parts.pathname.slice(base.length);
        if(file.includes("..")) throw 'Illegal path: ' + file;
        file = "../" + file;
        var fastOptRelativePath = '../target/scala-2.12/topshell-fastopt.js';
        var fastOptPath = path.join(__dirname, fastOptRelativePath);
        if(file === '../target/scala-2.12/topshell-opt.js' && fs.existsSync(fastOptPath)) file = fastOptRelativePath;
        file = path.join(__dirname, file);
        var stream = fs.createReadStream(file);
        if(file.endsWith(".js")) response.setHeader("Content-Type", "application/javascript");
        stream.on('error', function() {
            response.writeHead(404);
            response.end();
        });
        stream.pipe(response);
    } else {
        utils.sendResponse(response, "Not found", 404);
    }
});

if(process.argv.length > 3) throw 'Expected one or zero command line arguments';
if(process.argv.length === 3 && !process.argv[2].includes(":")) throw 'Expected a host:port command line argument';
var host = process.argv.length === 3 ? process.argv[2].split(":")[0] : 'localhost';
var port = process.argv.length === 3 ? process.argv[2].split(":")[1] : 7070;

server.listen(port, host);

console.log("http://" + host + ":" + port + "/topshell/index.html");
