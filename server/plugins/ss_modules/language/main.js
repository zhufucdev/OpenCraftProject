const object = require('object');
const impl = object.fromJava('com.zhufu.opencraft.Language')

/**
 * Gets text from Server Language Mapping for a speciafic kind of language,
 * which is part of the DualLanguage.
 * @see <span style="color: aqua">plugins/lang</span>
 * @param languageName {string} Code name defined in the info selection of the resource file.
 * @param resourceName {string} Path to the text according to the resource file.
 * @param arguments {string} Replace alternative phrases in the text with the given content.
 * @returns {string}
 */
function getString(languageName, resourceName, ...arguments) {
    return impl.got(languageName, resourceName, arguments === undefined ? [] : arguments);
}

module.exports = {
    getString: getString
}