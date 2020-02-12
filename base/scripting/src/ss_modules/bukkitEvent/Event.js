const object = require('object');
const bukkit = object.fromJava('org.bukkit.Bukkit');
const logger = require('logger');

function Event(wrapper) {
    const constructor = {};
    const wrap = (name, method) => {
        function is(packageName) {
            return object.fromJava(packageName).javaClass.isAssignableFrom(object.javaClass(wrapper))
        }
        if (is('org.bukkit.event.server.ServerListPingEvent')) {
            if (name === "setServerIcon") {
                return (value) => {
                    if (value.typeFs === 'File' || value.typeFs === 'Buffer') {
                        wrapper.setServerIcon(bukkit.getServer().loadServerIcon(value.$wrapper));
                    }
                    else
                        throw 'This setter receives a File or Buffer object only.'
                }
            }
        }
        return method;
    };
    for (let key in wrapper) {
        if (typeof wrapper[key] === "function") {
            function name(prefix, value) {
                let name = key.substr(prefix.length);
                name = name[0].toLowerCase() + name.substr(1);
                if (!constructor[name]) constructor[name] = {};
                const t = constructor[name];
                t[prefix + "ter"] = value;
                constructor[name] = t;
            }

            if (key.startsWith("get")) {
                name("get", wrap(key, wrapper[key]))
            } else if (key.startsWith("set")) {
                const methods = object.javaClass(wrapper).getMethods();
                let found = 0;
                for (let i in methods) {
                    if (methods[i].getName() === key) {
                        found ++;
                        if (found >= 2) {
                            break;
                        }
                    }
                }
                if (found < 2)
                    name("set", wrap(key, wrapper[key]));
                else
                    constructor[key] = wrapper[key];
            } else {
                constructor[key] = wrapper[key]
            }
        } else {
            constructor[key] = wrapper[key]
        }
    }
    this.prototype = object.withProperties(constructor);
}

module.exports = Event;