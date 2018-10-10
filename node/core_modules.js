var fs = require('fs');
var path = require('path');

module.exports = {
    findCoreModules: function(callback) {
        var corePath = path.join("..", "core");
        fs.readdir(corePath, (outerError, files) => {
            if(outerError) callback(outerError, null); else {
                var jsFiles = files.filter(file => file.endsWith(".js"));
                var pending = jsFiles.length;
                var results = {};
                var failed = false;
                jsFiles.forEach((file, i) => {
                    var filePath = path.join(corePath, file);
                    fs.readFile(filePath, 'utf8', (error, js) => {
                        if(!failed) {
                            if(error) {
                                failed = true;
                                callback(error);
                            } else {
                                var pattern = /(?:[/][/][:]([^\r\n]+)[\r]?)?(?:[\n]|^)exports[.]([a-zA-Z0-9]+)/g;
                                var symbols = [];
                                js.replace(pattern, function(match, t, x) {
                                    t = t || "any";
                                    symbols.push({name: x, type: t.trim()});
                                });
                                var m = file.replace(".js", "");
                                results[m] = symbols;
                                if(--pending === 0) callback(null, results);
                            }
                        }
                    });
                });
            }
        });
    },
    toJs: function(modules) {
        return (
            "var global = this;\n" +
            "global.tsh = global.tsh || {};\n" +
            "global.tsh.coreModules = " + JSON.stringify(modules, null, "    ") + ";\n"
        );
    }
};
