var fs = require('fs');
var path = require('path');
var child_process = require('child_process');
try { var Client = require('ssh2').Client } catch(e) {}

// TODO: Find out how to avoid ssh Process.run to be parsed as a shell script, and decide on ssh executable vs ssh2 lib

module.exports = {
    'Process.run': (json, sshContext, callback) => {
        let arguments = ["-o", "BatchMode yes", sshContext.user + "@" + sshContext.host, json.path];
        child_process.execFile("ssh", arguments.concat(json.arguments), json.config, (error, stdout, stderr) => {
            if(json.config.check !== false) callback(error, {out: stdout, error: stderr});
            else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
        });
    },
    'Process.shell': (json, sshContext, callback) => {
        try {
            var privateKey =
                sshContext.config.key ? sshContext.config.key :
                sshContext.config.keyPath ? fs.readFileSync(sshContext.config.keyPath) : null;
        } catch(e) {
            return callback(e, void 0);
        }
        var connectOptions = {
            host: sshContext.host,
            port: sshContext.config.port,
            username: sshContext.user,
            password: sshContext.config.password,
            passphrase: sshContext.config.passphrase,
            privateKey: privateKey
        };
        var client = new Client();
        client.on('error', e => callback(e, void 0));
        client.on('ready', () => {
            client.exec(json.command, json.config, function(err, stream) {
                if(err) callback(err, void 0);
                var stdout = "";
                var stderr = "";
                stream.on('close', function(code, signal) {
                    var error = code === 0 ? void 0 : "Non-zero exit code: " + code;
                    if(json.config.check !== false) callback(error, {out: stdout, error: stderr});
                    else callback(void 0, {out: stdout, error: stderr, problem: error, code: code, signal: signal});
                    client.end();
                }).on('data', function(data) {
                    stdout += data;
                }).stderr.on('data', function(data) {
                    stderr += data;
                });
            });
        }).connect(connectOptions);
    },
};
