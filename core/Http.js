// TODO: This needs Task to implement
exports.get_ = function(url) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.onload = function() {
        if(xhr.readyState === 4) {
            if(xhr.status === 200) {
                console.dir(xhr.responseText);
            } else {
                console.dir(xhr.statusText);
            }
        }
    };
    xhr.send();
};
