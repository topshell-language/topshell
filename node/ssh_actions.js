var fs = require('fs');
var child_process = require('child_process');

module.exports = {
    'File.readText': (json, context, callback) => {
        execFile(context.ssh, json.config, "cat", ["--", json.path], "", false, (error, result) => {
            if(error == null) callback(void 0, result.out);
            else callback(error, result);
        });
    },
    'File.writeText': (json, context, callback) => {
        execFile(context.ssh, json.config, "dd", ["of=" + json.path], json.contents, false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.appendText': (json, context, callback) => {
        execFile(context.ssh, json.config, "dd", ["conv=notrunc", "oflag=append", "of=" + json.path], json.contents, false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.readBytes': (json, context, callback) => {
        execFile(context.ssh, json.config, "cat", ["--", json.path], "", true, (error, result) => {
            if(error == null) callback(void 0, result.out.toString('hex'));
            else callback(error, result);
        });
    },
    'File.writeBytes': (json, context, callback) => {
        execFile(context.ssh, json.config, "dd", ["of=" + json.path], Buffer.from(json.contents, 'hex'), false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.appendBytes': (json, context, callback) => {
        execFile(context.ssh, json.config, "dd", ["conv=notrunc", "oflag=append", "of=" + json.path], Buffer.from(json.contents, 'hex'), false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.copy': (json, context, callback) => {
        execFile(context.ssh, json.config, "cp", ["-R", "--", json.path, json.target], "", false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.rename': (json, context, callback) => {
        execFile(context.ssh, json.config, "mv", ["--", json.path, json.target], "", false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.createDirectory': (json, context, callback) => {
        execFile(context.ssh, json.config, "mkdir", ["--", json.path], "", false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.deleteDirectory': (json, context, callback) => {
        execFile(context.ssh, json.config, "rmdir", ["--", json.path], "", false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.delete': (json, context, callback) => {
        execFile(context.ssh, json.config, "rm", ["--", json.path], "", false, (error, result) => {
            if(error == null) callback(void 0, {});
            else callback(error, result);
        });
    },
    'File.list': (json, context, callback) => {
        execFile(context.ssh, json.config, "ls", [json.path], "", false, (error, result) => {
            if(error == null) callback(void 0, result.out.split("\n").filter(f => f.length !== 0));
            else callback(error, result);
        });
    },
    'File.listStatus': (json, context, callback) => {
        execFile(context.ssh, json.config, "ls", ["--almost-all", "--escape", "--quote", "--file-type", "--", json.path], "", false, (error, result) => {
            function parse(f) {
                // TODO: JSON.parse isn't quite the right parser here
                if(f.endsWith("\"")) return {name: JSON.parse(f), isFile: true, isDirectory: false};
                else return {name: JSON.parse(f.slice(0, -1)), isFile: false, isDirectory: f.slice(-1) === "/"};
            }
            if(error == null) callback(void 0, result.out.split("\n").filter(s => s.length !== 0).map(parse));
            else callback(error, result);
        });
    },
    'File.status': (json, context, callback) => {
        execFile(context.ssh, json.config, "ls", ["--almost-all", "--escape", "--quote", "--file-type", "--directory", "--", json.path], "", false, (error, result) => {
            function parse(f) {
                // TODO: JSON.parse isn't quite the right parser here
                if(f.endsWith("\"")) return {name: JSON.parse(f), isFile: true, isDirectory: false};
                else return {name: JSON.parse(f.slice(0, -1)), isFile: false, isDirectory: f.slice(-1) === "/"};
            }
            if(error == null) callback(void 0, result.out.split("\n").filter(s => s.length !== 0).map(parse)[0]);
            else callback(error, result);
        });
    },
    'Process.run': (json, context, callback) => {
        execFile(context.ssh, json.config, json.path, json.arguments, json.config.in || "", false, callback);
    },
    'Process.shell': (json, context, callback) => {
        let arguments = execFileSshArguments.concat([context.ssh.user + "@" + context.ssh.host, json.command]);
        let child = child_process.execFile("ssh", arguments, json.config, (error, stdout, stderr) => {
            if(json.config.check !== false) callback(error, {out: stdout, error: stderr});
            else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
        });
        child.stdin.end(json.config.in || "");
    },
};

let execFileSshArguments =
    ["-oControlMaster=auto", "-oControlPersist=5m", "-oControlPath=~/.ssh/.topshell-%C", "-oBatchMode=yes", "--"];

function execFile(ssh, config, path, arguments, stdin, binary, callback) {
    config = config || {};
    if(binary) config.encoding = 'buffer';
    let escape = a => "'" + a.replace(/'/g, "'\\''") + "'";
    let command = [path].concat(arguments).map(a => escape(a)).join(" ");
    let newArguments = execFileSshArguments.concat([ssh.user + "@" + ssh.host, command]);
    let child = child_process.execFile("ssh", newArguments, config, (error, stdout, stderr) => {
        if(binary) stderr = stderr.toString('utf8');
        if(config.check !== false) callback(error, {out: stdout, error: stderr});
        else callback(void 0, {out: stdout, error: stderr, problem: error.message, code: error.code, killed: error.killed, signal: error.signal});
    });
    child.stdin.end(stdin);
}

