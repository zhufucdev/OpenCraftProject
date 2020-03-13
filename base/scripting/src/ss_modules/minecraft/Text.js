const object = require('object');
const textColor = object.fromJava('com.zhufu.opencraft.TextUtil').TextColor;
let format;
(function () {
    format = require('Enum').formatJavaName
})();
/**
 * Minecraft Text Format
 * @see com.zhufu.opencraft.TextUtil.TextColor
 */
const formats = {};
for (let key in textColor) {
    const newKey = format(key);
    if (key !== 'javaClass' && typeof textColor[key] === 'object') {
        formats[newKey] = textColor[key]
    }
}

module.exports = formats;