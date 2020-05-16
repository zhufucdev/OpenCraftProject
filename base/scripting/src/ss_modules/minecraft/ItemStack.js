const object = require('object');

/**
 * Representing a stack of items, maybe from an Inventory, e.t.c
 * @param type {Material} Type of each item.
 * @param amount {number} Amount of the items in the stack.
 * @constructor
 */
function ItemStack(type, amount) {
    const ItemFlags = object.fromJava('org.bukkit.inventory.ItemFlag');
    const javaClass = object.fromJava('org.bukkit.inventory.ItemStack');
    const Material = require('Material');
    const DynamicArray = require('util/DynamicArray');
    const toJSArray = require('util/toJSArray');
    const Enum = require('Enum');

    let wrapper;
    if (typeof type !== 'object' || !object.isOfClass(type, javaClass)) {
        const t = type.MCType === 'Material' ? type.$wrapper : new Material(type);
        if (typeof amount === 'number')
            wrapper = new JavaClass(t, amount)
        else
            wrapper = new JavaClass(t)
    } else {
        wrapper = type;
    }

    const lore = new DynamicArray(() => wrapper.setLore(lore.items), () => wrapper.setLore(lore.items), toJSArray(wrapper.getLore()));
    const flags = new DynamicArray((item) => wrapper.addItemFlags(ItemFlags[Enum.toJavaName(item)]),
        (item) => wrapper.removeItemFlags(ItemFlags[Enum.toJavaName(item)]),
        toJSArray(wrapper.getItemFlags()))
    this.prototype = object.withProperties({
        amount: {
            getter: () => wrapper.getAmount(),
            setter: (value) => wrapper.setAmount(value)
        },
        type: {
            getter: () => new Material(wrapper.getType()),
            setter: (value) => wrapper.setType(value.$wrapper)
        },
        lore: {getter: () => lore},
        flags: {getter: () => flags},
        displayName: {
            getter: () => wrapper.hasItemMeta() && wrapper.getItemMeta().hasDisplayName()
                ? wrapper.getItemMeta().getDisplayName()
                : null,
            setter: (value) => wrapper.getItemMeta().setDisplayName(value)
        },
        unbreakable: {
            getter: () => wrapper.hasItemMeta() && wrapper.getItemMeta().isUnbreakable(),
            setter: (value) => wrapper.getItemMeta().setUnbreakable(value)
        }
    })

    this.$wrapper = wrapper;
    this.MCType = "ItemStack";
}

module.exports = ItemStack;
module.shareContext = false;