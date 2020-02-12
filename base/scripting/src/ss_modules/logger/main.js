'use requester';
const wrapper = Java.type('org.bukkit.Bukkit').getLogger();

function stringify(o) {
    let context;
    try {
        context = requester();
    } catch (ignored) {
    }
    const string = o.toString ? o.toString() : o;
    return context ? "[" + context.name + "] " + string : string;
}

module.exports = {
    info: function (text) {
        wrapper.info(stringify(text));
    },
    config: function (text) {
        wrapper.config(stringify(text));
    },
    warn: function (text) {
        wrapper.warning(stringify(text));
    }
};