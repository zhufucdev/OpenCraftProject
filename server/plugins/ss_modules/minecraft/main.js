const Material = require('Material');
const world = require('World');
const Vector = require('Vector');
const text = require('Text');
const GameRule = require('GameRule');
module.exports = {
    /**
     * Gets an object representing the type of a Minecraft item or block.
     * @param name {string} of Minecraft Item ID. e.g. For stone, "minecraft:stone" or "stone".
     * @param isLegacyName {boolean} magic value.
     * @return {Material} representing the material.
     */
    material: (name, isLegacyName) => new Material(name, isLegacyName === undefined ? false : isLegacyName),
    /**
     * Gets an object representing a Minecraft world.
     * @param name {string} name of the world.
     * @return {World} representing the instance of the world.
     */
    world: (name) => new world.World(name),
    /**
     * Gets an object representing a coordinate in a specific Minecraft world.
     * @param world {World} for the coordinate to be in.
     * @param x {number} The East.
     * @param y {number} The Height.
     * @param z {number} The South.
     * @return {Location}
     */
    location: (world, x, y, z) => {
        const r = new world.Location(typeof world === 'string' ? new world.World(world) : world, x, y, z);
        r.toVector = () => new Vector(r.x, r.y, r.z);
        return r
    },
    /**
     * Gets an object with directions.
     * @param x {number}
     * @param y {number}
     * @param z {number}
     * @return {Vector}
     */
    vector: (x, y, z) => {
        const r = new Vector(x, y, z);
        r.toLocation = (world) => new world.Location(typeof world === 'string' ? new world.World(world) : world, r.x, r.y, r.z);
        return r
    },
    /**
     * An object for text format.
     * @type {Object}
     * @see com.zhufu.opencraft.TextUtil.TextColor
     */
    text: text,
    /**
     * An object representing GameRule.
     * @type {GameRule}
     */
    GameRule: GameRule,
};
module.shareContext = false;