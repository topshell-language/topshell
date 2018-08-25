var fs = require('fs');
var path = require('path');
var child_process = require('child_process');

module.exports = {
    'File.readText': (json, context, callback) => {
        execFile(context.ssh, json.config, "cat", [json.path], "", callback);
    },
    'File.writeText': (json, context, callback) => {
        execFile(context.ssh, json.config, "dd", ["of=" + json.path], json.contents, (error, result) => {
            if(error == null) callback(void 0, "");
            else callback(error, result);
        });
    },
    'File.list': (json, context, callback) => {
        execFile(context.ssh, json.config, "ls", [json.path], "", (error, result) => {
            if(error == null) callback(void 0, result.out.split("\n"));
            else callback(error, result);
        });
    },
    'File.listStatus': (json, context, callback) => {
        callback('File.listStatus not yet implemented for ssh', void 0);
    },
    'File.status': (json, context, callback) => {
        callback('File.status not yet implemented for ssh', void 0);
    },
    'Process.run': (json, context, callback) => {
        execFile(context.ssh, json.config, json.path, json.arguments, "", callback);
    },
    'Process.shell': (json, context, callback) => {
        var arguments = ["-o", "BatchMode yes", context.ssh.user + "@" + context.ssh.host, json.command];
        child_process.execFile("ssh", arguments, json.config, (error, stdout, stderr) => {
            if(json.config.check !== false) callback(error, {out: stdout, error: stderr});
            else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
        });
    },
};



function execFile(ssh, config, path, arguments, stdin, callback) {
    config = config || {};
    let escape = a => "'" + a.replace(/'/g, "'\\''") + "'";
    let command = [path].concat(arguments).map(a => escape(a)).join(" ");
    let newArguments = ["-o", "BatchMode yes", ssh.user + "@" + ssh.host, command];
    let child = child_process.execFile("ssh", newArguments, config, (error, stdout, stderr) => {
        if(config.check !== false) callback(error, {out: stdout, error: stderr});
        else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
    });
    child.stdin.end(stdin);
}

