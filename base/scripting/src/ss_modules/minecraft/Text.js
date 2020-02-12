const object = require('object');
const textColor = object.fromJava('com.zhufu.opencraft.TextUtil').TextColor;
/**
 * Minecraft Text Format
 * @see com.zhufu.opencraft.TextUtil.TextColor
 */
const formats = {};
for (let key in textColor) {
    const newKey = key.toLowerCase();
    if (key !== 'javaClass' && typeof textColor[key] === 'object') {
        formats[newKey] = textColor[key]
    }
}

module.exports = formats;