const object = require('object');
const blockFace = object.fromJava('org.bukkit.block.BlockFace');
const formatName = require('Enum').formatJavaName;

function BlockFace(name) {
    let wrapper;

    if (object.isOfClass(name, 'org.bukkit.block.BlockFace')) {
        wrapper = name;
    } else {
        for (let key in blockFace) {
            if (name === formatName(key)) {
                wrapper = blockFace[key]
            }
        }

        if (!wrapper) throw "No such BlockFace: " + name + ".";
    }

    this.$wrapper = wrapper;
    this.prototype = object.withProperties({
        name: {getter: () => formatName(wrapper.name())},
        modX: {getter: () => wrapper.getModX()},
        modY: {getter: () => wrapper.getModY()},
        modZ: {getter: () => wrapper.getModZ()},
        opposite: {getter: () => new BlockFace(wrapper.getOppositeFace())},
        direction: {
            getter: () => {
                const Vector = require('Vector');
                return new Vector(wrapper.getDirection())
            }
        }
    });

    this.MCType = "BlockFace";
}

BlockFace = object.extend(BlockFace, {
    down: new BlockFace(blockFace.DOWN),
    east: new BlockFace(blockFace.EAST),
    north: new BlockFace(blockFace.NORTH),
    northEast: new BlockFace(blockFace.NORTH_EAST),
    northWest: new BlockFace(blockFace.NORTH_WEST),
    self: new BlockFace(blockFace.SELF),
    south: new BlockFace(blockFace.SOUTH),
    southEast: new  BlockFace(blockFace.SOUTH_EAST),
    southWest: new BlockFace(blockFace.SOUTH_WEST),
    up: new BlockFace(blockFace.UP),
    west: new BlockFace(blockFace.WEST),
});

module.exports = BlockFace;