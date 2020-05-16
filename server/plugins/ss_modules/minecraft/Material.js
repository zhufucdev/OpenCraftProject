const object = require('object');
const material = object.fromJava('org.bukkit.Material');
function Material(name, isLegacyName) {
    let wrapper;
    if (object.isOfClass(name, 'org.bukkit.Material')) {
        wrapper = name;
    } else {
        wrapper = material.matchMaterial(name, isLegacyName);
    }
    this.$wrapper = wrapper;
    this.prototype = object.withProperties({
        name: { getter: () => wrapper.getKey().toString() },
        namespace: { getter: () => wrapper.getKey().getNamespace() },
        isBlock: { getter: () => wrapper.isBlock() },
        isAir:{ getter: () => wrapper.isAir() },
        isSolid: { getter: () => wrapper.isSolid() },
        isFlammable: { getter: () => wrapper.isFlammable() },
        isBurnable: { getter: () => wrapper.isBurnable() },
        isFuel: { getter: () => wrapper.isFuel() },
        isOccluding: { getter: () => wrapper.isOccluding() },
        hasGravity: { getter: () => wrapper.hasGravity() },
        isItem: { getter: () => wrapper.isItem() },
        isInteractable: { getter: () => wrapper.isInteractable() },
        hardness: { getter: () => wrapper.getHardness() },
        blastResistance: { getter: () => wrapper.getBlastResistance() },
        maxDurability: { getter: () => wrapper.getMaxDurability() },
        maxStackSize: { getter: () => wrapper.getMaxStackSize() }
    });
    this.MCType = "Material";
}
module.exports = Material;