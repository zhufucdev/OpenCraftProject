const object = require('object');
const format = require('Format');
const logger = require('logger');
const java = object.fromJava('org.bukkit.configuration.file.YamlConfiguration');

function setAll(to, layer, path) {
    for (let key in layer) {
        const it = layer[key];
        if (typeof it === 'object') {
            setAll(to, it, path === undefined ? key : path + '.' + key);
        } else if (typeof it === 'function') {
            logger.warn('Skipping %s cause function is not supported.', path + '.' + key)
        } else {
            to.set(path + '.' + key, it);
        }
    }
}

function Yaml(wrapper) {
    let impl;
    if (typeof wrapper === 'undefined') {
        impl = new java();
    } else if (object.isOfClass(wrapper, 'org.bukkit.configuration.ConfigurationSection')) {
        impl = wrapper;
    } else if (typeof wrapper === 'string') {
        impl = java.loadFromString(wrapper);
    } else if (wrapper.typeFs === 'Buffer' || 'File') {
        impl = java.loadConfiguration(wrapper.$wrapper);
    } else {
        throw format('Cannot initialize Yaml from %s. You may use function ' +
        'require("util/Yaml").parse instead.', wrapper);
    }
    if (!impl) throw format('Could not initialize Yaml from %s', wrapper);
    this.$wrapper = impl;

    this.get = (path) => impl.get(path);
    this.set = (path, value) => value === undefined ? setAll(impl, path) : (typeof path === 'object' ? setAll(impl, value, path) : impl.set(path, value));
}

Yaml.parse = (any) => {

    if (typeof any !== 'object') throw 'Only parsing an object is allowed.'
    const result = new java();
    setAll(result, any);

    return new Yaml(result);
}

module.exports = Yaml;