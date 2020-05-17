module.exports = function(from, ...args) {
    let index, count = 0;
    while ((index = from.indexOf('%s')) !== -1) {
        let insert = args[count];
        if (typeof insert.toString === 'function')
            insert = insert.toString();
        from = from.substr(0, index) + insert + from.substr(index + 2);
        count++;
    }
    return from;
};
module.shareContext = false;