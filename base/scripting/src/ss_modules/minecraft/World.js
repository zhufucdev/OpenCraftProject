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
function World(name) {
    checkObjectType(name, 'string');
    const mWorlds = manager.getAvailableWorlds();
    let wrapper;
    for (let i in mWorlds) {
        if (mWorlds[i].getWorld().getName() === name) {
            wrapper = mWorlds[i];
            break;
        }
    }
    if (!wrapper)
        throw "No such world available: " + name + name.startsWith("game_") ? " cause it's the world of a mini game." : ".";
    this.$wrapper = wrapper;

    this.prototype = object.withProperties({
        name: {getter: () => wrapper.getWorld().getName()},
        blockAt: (location) => (location.x && location.y && location.z) ? new Block(wrapper.getWorld().getBlockAt(location.x, location.y, location.z)) : null,
        permission: {getter: () => wrapper.permission, setter: (value) => wrapper.permission = value},
        spawnpoint: {
            getter: () => new Location(wrapper.getWorld().getSpawnLocation()),
            setter: (value) => {
                wrapper.getWorld().setSpawnLocation(configLocation(value))
            }
        },
        gameRule: (name) => new GameRule(name, this)
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
        block: {getter: () => world.$wrapper.getWorld().getBlockAt(wrapper)}
    });

    this.MCType = "Location";
}

const BlockFace = require('BlockFace');
function Block(wrapper) {
    this.$wrapper = wrapper;
    this.prototype = object.withProperties({
        material: {
            getter: () => Material(wrapper.getType()),
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
    this.MCType = "Chunk"
}

module.exports = {
    World: World,
    Location: Location
};