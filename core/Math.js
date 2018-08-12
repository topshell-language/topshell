exports.mod_ = function(top) {
    return function(bottom) {
        return top % bottom;
    };
};
