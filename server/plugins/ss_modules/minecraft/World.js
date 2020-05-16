const format = require('util/Format');
const object = require('object');

function checkMCType(target, shouldBe) {
    if (world.MCType !== shouldBe) throw "Parameter " + target + " is not a Minecraft " + shouldBe + " object.";
}

function checkObjectType(target, shouldBe) {
    if (typeof target !== shouldBe) throw "Parameter " + target + " is not a " + shouldBe;
}

function configLocation(config, world) {
    if (config.MCType === "Location") {
        return config;
    } else {
        return new Location(world, config.x, config.y, config.z);
    }
}

const manager = object.fromJava('com.zhufu.opencraft.WorldManager');
const JavaLocation = object.fromJava('org.bukkit.Location');
const Material = require('Material');
const GameRule = require('GameRule');
const Player = require('Player');

function World(name) {
    checkObjectType(name, 'string');
    if (!object.isModuleLoaded("bukkit:WorldTeleporter")) {
        throw 'bukkit:WorldTeleporter is not enabled. You may use ' +
        'the function {after(name, function)}.';

    }
    const mWorlds = manager.getAvailableWorlds();
    let wrapper;
    for (let i in mWorlds) {
        if (mWorlds[i].getWorld().getName() === name) {
            wrapper = mWorlds[i];
            break;
        }
    }
    if (!wrapper) {
        throw "No such world available: " + name + name.startsWith("game_") ? " cause it's the world of a mini game." : ".";
    }
    this.$wrapper = wrapper;

    const worldWrap = wrapper.getWorld();

    this.prototype = object.withProperties({
        name: {getter: () => worldWrap.getName()},
        blockAt: (location) => {
            if (location.x !== undefined && location.y !== undefined && location.z !== undefined) {
                if (location.MCType === "Location") {
                    return new Block(worldWrap.getBlockAt(location.blockX, location.blockY, location.blockZ));
                }
                return new Block(worldWrap.getBlockAt(location.x, location.y, location.z));
            }
            throw "Parameter must be like {x: $number, y: $number, z: $number}."
        },
        chunkAt: (x, z) => new Chunk(worldWrap.getChunkAt(x, z)),
        permission: {
            getter: () => wrapper.getPermission().name().toLowerCase(),
            setter: (value) => {
                if (typeof value === 'string') {
                    const permission = require('WorldPermission');
                    if (permission[value] !== undefined)
                        wrapper.setPermission(permission[value]);
                    else
                        throw format('No such World Permission: %s.', value);
                } else {
                    wrapper.setPermission(value)
                }
            }
        },
        spawnpoint: {
            getter: () => new Location(worldWrap.getSpawnLocation()),
            setter: (value) => {
                worldWrap.setSpawnLocation(configLocation(value))
            }
        },
        gameRule: (name) => new GameRule(name, this),
        players: {
            getter: () => {
                const list = worldWrap.getPlayers();
                let r = [];
                for (let i in list) {
                    r.push(new Player(list[i]))
                }
                return r
            }
        }
    });
    this.MCType = "World";

}

function Location(world, x, y, z) {
    let wrapper;

    if (object.isOfClass(world, 'org.bukkit.Location')) {
        wrapper = world;
        world = new World(wrapper.getWorld().getName())
    } else {
        checkMCType(world, "World");
        checkObjectType(x, 'number');
        checkObjectType(y, 'number');
        checkObjectType(z, 'number');
        wrapper = new JavaLocation(world.$wrapper, x, y, z);
    }
    this.$wrapper = wrapper;

    this.prototype = object.withProperties({
        world: {getter: () => world},
        x: {getter: () => wrapper.getX(), setter: (value) => wrapper.setX(value)},
        blockX: {getter: () => wrapper.getBlockX()},
        y: {getter: () => wrapper.getY(), setter: (value) => wrapper.setY(value)},
        blockY: {getter: () => wrapper.getBlockY()},
        z: {getter: () => wrapper.getZ(), setter: (value) => wrapper.setZ(value)},
        blockZ: {getter: () => wrapper.getBlockZ()},
        distanceTo:
            (location) => location.MCType === "Location" ? wrapper.distance(location.$wrapper) : (location.x && location.y && location.z ? wrapper.distance(new JavaLocation(location.x, location.y, location.z)) : null),
        squaredDistanceTo:
            (location) => location.MCType === "Location" ? wrapper.distanceSquared(location.$wrapper) : (location.x && location.y && location.z ? wrapper.distanceSquared(new JavaLocation(location.x, location.y, location.z)) : null),
        block: {getter: () => new Block(world.$wrapper.getWorld().getBlockAt(wrapper))},
        chunk: {getter: () => new Chunk(world.$wrapper.getWorld().getChunkAt(wrapper))}
    });
    this.MCType = "Location";

}

const BlockFace = require('BlockFace');

function Block(wrapper) {
    this.$wrapper = wrapper;
    this.prototype = object.withProperties({
        material: {
            getter: () => new Material(wrapper.getType()),
            setter: (value) => {
                if (typeof value === 'string') {
                    wrapper.setType(new Material(value).$wrapper)
                } else {
                    checkMCType(value, "Material");
                    wrapper.setType(value.$wrapper)
                }
            }
        },
        location: {getter: () => new Location(wrapper.getLocation())},
        lightLevel: {getter: () => wrapper.getLightLevel()},
        skyLight: {getter: () => wrapper.getLightFromSky()},
        blockLight: {getter: () => wrapper.getLightFromBlock()},
        relativeBy: (face, distance) => {
            checkMCType(face, "BlockFace");
            checkObjectType(distance, 'number');
            return new BlockFace(wrapper.getRelative(face.$wrapper, parseInt(distance)))
        },
        faceTo: (block) => {
            checkMCType(block, "Block");
            return new BlockFace(wrapper.getFace(block.$wrapper))
        },
    });
    this.MCType = "Block";

}

function Chunk(wrapper) {
    this.$wrapper = wrapper;

    this.prototype = object.withProperties({
        isLoaded: {getter: () => wrapper.isLoaded()},
        load: () => wrapper.load(),
        unload: (save) => save !== undefined ? wrapper.unload(save) : wrapper.unload(),
        forceLoaded: {getter: () => wrapper.isForceLoaded(), setter: (value) => wrapper.setForceLoaded(value)},
        x: {getter: () => wrapper.getX()},
        z: {getter: () => wrapper.getZ()},
        blockAt: (location) => {
            if (location.x !== undefined && location.y !== undefined && location.z !== undefined) {
                if (location.MCType === "Location") {
                    return new Block(wrapper.getBlock(location.blockX, location.blockY, location.blockZ));
                }
                return new Block(wrapper.getBlock(location.x, location.y, location.z));
            }
            throw "Parameter must be like {x: $number, y: $number, z: $number}."
        }
    });

    this.MCType = "Chunk"
}

module.exports = {
    World: World,
    Location: Location,
    locationLike: function (obj, world) {
        if (obj.x !== undefined && obj.y !== undefined && obj.z !== undefined) {
            if (obj.MCType === "Location") return obj;
            if (obj.world !== undefined) world = obj.world;
            return new Location(typeof world === 'string' ? new World(world) : world, obj.x, obj.y, obj.z)
        } else if (object.isOfClass(obj, 'org.bukkit.Location')) {
            return new Location(obj)
        }
    }
};
module.shareContext = false;