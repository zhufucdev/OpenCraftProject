const Material = require('Material');
const World = require('World');
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
    world: (name) => new World.World(name),
    /**
     * Gets an object representing a coordinate in a specific Minecraft world.
     * @param world {World|string} for the coordinate to be in.
     * @param x {number} The East.
     * @param y {number} The Height.
     * @param z {number} The South.
     * @return {Location}
     */
    location: (world, x, y, z) => {
        const r = new World.Location(typeof world === 'string' ? new World.World(world) : world, x, y, z);
        r.toVector = () => new Vector(r.x, r.y, r.z);
        return r
    },
    /**
     * Gets a direction.
     * @param x {number}
     * @param y {number}
     * @param z {number}
     * @return {Vector}
     */
    vector: (x, y, z) => {
        const r = new Vector(x, y, z);
        r.toLocation = (world) => new World.Location(typeof world === 'string' ? new World.World(world) : world, r.x, r.y, r.z);
        return r
    },
    defineItem: (id, definition) => require('CustomItem').define(id, definition),
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