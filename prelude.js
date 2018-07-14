(function(_g) {
    _g.tsh_prelude = {};
    var _p = _g.tsh_prelude;

    _p.true_ = true;
    _p.false_ = false;
    _p.null_ = null;
    _p.undefined_ = void 0;

    _p.recordRest = function(m, r) {
        for(var k in r) {
            if(Object.prototype.hasOwnProperty.call(r, k) && !Object.prototype.hasOwnProperty.call(m, k)) m[k] = r[k];
        }
        return m;
    };

    _p._map = function(r) {
        if(Array.isArray(r)) {
            return function(f) {
                var result = [];
                for(var i = 0; i < r.length; i++) {
                    var a = f(r[i]);
                    result.push(a);
                }
                return result;
            };
        } else return r.map_;
    };
    _p._then = function(r) {
        if(Array.isArray(r)) {
            return function(f) {
                var result = [];
                for(var i = 0; i < r.length; i++) {
                    var a = f(r[i]);
                    for(var j = 0; j < a.length; j++) {
                        result.push(a[j]);
                    }
                }
                return result;
            };
        } else return r.then_;
    };
    _p._size = function(r) { if(Array.isArray(r)) return r.length; else return r.size_; };
    _p._at = function(r) { if(Array.isArray(r)) return function(i) { return r[i]; }; else return r.at_; };
    _p._take = function(r) { if(Array.isArray(r)) return function(i) { return r.slice(0, i); }; else return r.take_; };
    _p._drop = function(r) { if(Array.isArray(r)) return function(i) { return r.slice(i); }; else return r.drop_; };
    _p._takeLast = function(r) { if(Array.isArray(r)) return function(i) { return r.slice(-i); }; else return r.takeLast_; };
    _p._dropLast = function(r) { if(Array.isArray(r)) return function(i) { return r.slice(0, -i); }; else return r.dropLast_; };
    _p._takeWhile = function(r) { return r.takeWhile_; };
    _p._dropWhile = function(r) { return r.dropWhile_; };
    _p._takeLastWhile = function(r) { return r.takeLastWhile_; };
    _p._dropLastWhile = function(r) { return r.dropLastWhile_; };
    _p._zip = function(r) { return r.zip_; };
    _p._unzip = function(r) { return r.unzip_; };
    _p._indexes = function(r) { return r.indexes_; };
    _p._filter = function(r) { return r.filter_; };
    _p._startsWith = function(r) { return r.startsWith_; };
    _p._endsWith = function(r) { return r.endsWith_; };
    _p._find = function(r) { return r.find_; };
    _p._any = function(r) { return r.any_; };
    _p._all = function(r) { return r.all_; };
    _p._empty = function(r) { return r.empty_; };
    _p._reverse = function(r) { return r.reverse_; };
    _p._join = function(r) { return r.join_; };
    _p._sort = function(r) { return r.sort_; };
    _p._first = function(r) { if(Array.isArray(r)) return r[0]; else return r.head_; };
    _p._last = function(r) { if(Array.isArray(r)) return r[r.size - 1]; else return r.last_; };
    _p._append = function(r) { if(Array.isArray(r)) return function(a) { return r.concat(a); }; else return r.append_; };
    _p._foldLeft = function(r) { return r.foldLeft_; };
    _p._foldRight = function(r) { return r.foldRight_; };
    _p._scanLeft = function(r) { return r.scanLeft_; };
    _p._scanRight = function(r) { return r.scanRight_; };


    _p.isTag_ = function(tag) { return tag instanceof _p.Tag; };

    _p.tag_ = function(tagName) { return function(attributes) { return function(children) {
        return new _p.Tag(tagName, attributes, children);
    }}};

    _p.Tag = function(tagName, attributes, children) {
        this.tagName = tagName;
        this.attributes = attributes;
        this.children = children;
    };

    _p.isVisual_ = function(v) { return v instanceof _p.Visual; };

    _p.visual_ = function(options) { return new _p.Visual(options); };

    _p.Visual = function(options) {
        this.state = options.state_;
        this.render = options.render_;
    };

    _p.Visual.prototype.setHtml = function(container) {
        if(container == null) return;
        function flatEach(array, body) {
            for(var i = 0; i < array.length; i++) {
                if(Array.isArray(array[i])) flatEach(array[i], body);
                else if(array[i] != null) body(array[i])
            }
        }
        function go(node) {
            if(node instanceof _p.Tag) {
                var e = document.createElement(node.tagName);
                for(var k in node.attributes) if(node.attributes.hasOwnProperty(k)) {
                    e.setAttribute(k.replace("_", ""), node.attributes[k]);
                }
                if(Array.isArray(node.children)) {
                    flatEach(node.children, function(n) {
                        var c = go(n);
                        if(c != null) e.appendChild(c);
                    });
                } else {
                    var child = go(node.children);
                    if(child != null) e.appendChild(child);
                }
                return e;
            } else if(typeof node === "string" || typeof node === "number" || typeof node === "boolean") {
                return document.createTextNode(node);
            } else if(node == null) {
                return null;
            } else {
                throw "Can't convert to HTML: " + node;
            }
        }
        while(container.firstChild) container.removeChild(container.firstChild);
        var html;
        try { html = go(this.render(this.state)); } catch(e) { html = document.createTextNode(e + ""); }
        container.appendChild(html);
    };

})(this);
