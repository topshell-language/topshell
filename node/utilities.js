exports.sendResponse = (response, data, statusCode, headers) => {
    response.writeHead(statusCode, headers);
    response.end(data);
};

exports.readJsonRequest = (request, callback) => {
    var data = '';
    request.on('data', chunk => {
        data += chunk;
    });
    request.on('end', () => {
        callback(JSON.parse(data));
    });
};
