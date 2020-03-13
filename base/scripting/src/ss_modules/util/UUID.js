const object = require('object');
const java = object.fromJava('java.util.UUID');
const logger = require('logger');

/**
 * The Java UUID Wrap
 * @param string {string|java.util.UUID}
 * @constructor
 */
function UUID(string) {
    let wrapper;
    if (object.isOfClass(string, 'java.util.UUID')) {
        wrapper = string;
    } else if (typeof string === 'string') {
        wrapper = java.fromString(string);
    } else {
        wrapper = java.randomUUID();
        logger.warn('%s is not compatible with UUID. Using a random one.', wrapper)
    }
    this.$wrapper = wrapper;
    this.equals = (other) => {
        if (object.type !== "UUID") throw 'Cannot compare objects of different types.';
        return wrapper.equals(other.$wrapper);
    };
    this.prototype = object.withProperties({
        leastSignificantBits: {getter: () => wrapper.getLeastSignificantBits()},
        mostSignificantBits: {getter: () => wrapper.getMostSignificantBits()},
        timestamp: {getter: () => wrapper.timestamp()},
        clockSequence: {getter: () => wrapper.clockSequence()},
        node: {getter: () => wrapper.node()},
        toString: () => wrapper.toString()
    });
    this.type = "UUID"
}

module.exports = UUID;