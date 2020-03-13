'use requester';
const wrapper = Java.type('org.bukkit.Bukkit').getLogger();

function stringify(o, args) {
    let context;
    try {
        context = requester();
    } catch (ignored) {
    }
    function toString(o) {
        return typeof o === 'undefined' || o === null ? 'null' : (o.toString ? o.toString() : o)
    }
    let index, count = 0, string = toString(o);
    while ((index = string.indexOf('%s')) !== -1) {
        let insert = toString(args[count]);
        string = string.substr(0, index) + insert + string.substr(index + 2);
        count++;
    }
    return context ? "[" + context.name + "] " + string : string;
}

module.exports = {
    info: function (text, ...args) {
        wrapper.info(stringify(text, args));
    },
    config: function (text, ...args) {
        wrapper.config(stringify(text, args));
    },
    warn: function (text, ...args) {
        wrapper.warning(stringify(text, args));
    }
};