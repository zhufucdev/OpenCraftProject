const Material = require('Material');
const world = require('World');
const Vector = require('Vector');
const text = require('Text');
const GameRule = require('GameRule');
module.exports = {
    material: (name, isLegacyName) => new Material(name, isLegacyName === undefined ? false : isLegacyName),
    world: (name) => new world.World(name),
    location: (world, x, y, z) => {
        const r = new world.Location(typeof world === 'string' ? new world.World(world) : world, x, y, z);
        r.toVector = () => new Vector(r.x, r.y, r.z)
    },
    vector: (x, y, z) => {
        const r = new Vector(x, y, z);
        r.toLocation = (world) => new world.Location(typeof world === 'string' ? new world.World(world) : world, r.x, r.y, r.z)
    },
    text: text,
    GameRule: GameRule
};