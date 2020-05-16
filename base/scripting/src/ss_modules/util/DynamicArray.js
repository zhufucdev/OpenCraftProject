/**
 * Kind of Array that listens addition and removal.
 * @param addItem {function(Object)} Called when an item is added to the array.
 * @param removeItem {function(Object, number)} Called when an item is removed from the array.
 * @param items {Array} An array to be extended on.
 * @constructor
 */
function DynamicArray(addItem, removeItem, items) {
    let array = typeof items === 'undefined' ? [] : items;
    this.add = (item) => {
        array.push(item);
        if (typeof addItem === 'function') addItem(item)
    };
    this.removeAt = (index) => {
        const deleted = array.splice(index, 1);
        if (typeof removeItem === 'function') removeItem(deleted, index)
    };
    this.remove = (item) => {
        const index = array.indexOf(item);
        if (index == -1) return;
        this.removeAt(index);
        if (typeof removeItem === 'function') removeItem(item, index)
    }
    this.prototype = require('object').withProperties({
        items: {
            getter: () => array
        }
    })
}

module.exports = DynamicArray;
module.shareContext = false;