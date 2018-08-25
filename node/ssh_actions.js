var fs = require('fs');
var path = require('path');
var child_process = require('child_process');

module.exports = {
    'Process.run': (json, sshContext, callback) => {
        let escape = a => "'" + a.replace(/'/g, "'\\''") + "'";
        var command = [json.path].concat(json.arguments).map(a => escape(a)).join(" ");
        var arguments = ["-o", "BatchMode yes", sshContext.user + "@" + sshContext.host, command];
        child_process.execFile("ssh", arguments, json.config, (error, stdout, stderr) => {
            if(json.config.check !== false) callback(error, {out: stdout, error: stderr});
            else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
        });
    },
    'Process.shell': (json, sshContext, callback) => {
        var arguments = ["-o", "BatchMode yes", sshContext.user + "@" + sshContext.host, json.command];
        child_process.execFile("ssh", arguments, json.config, (error, stdout, stderr) => {
            if(json.config.check !== false) callback(error, {out: stdout, error: stderr});
            else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
        });
    },
};
