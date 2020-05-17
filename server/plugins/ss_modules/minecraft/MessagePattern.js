const format = require('util/Format');
const object = require('object');
const textFormats = require('Text');
const lang = object.fromJava('com.zhufu.opencraft.Language');

/**
 * An object representing Minecraft-formated Text.
 * @param from {Array|Object} The pattern.
 * <p> {text: "Hello World", color: 'green'} => <strong style="color: green">Hello World</strong> </p>
 * <p> [{text: "Hi", color: 'green'}, {text: "World", color: 'purple'}] => <strong style="color: green">Hi</strong>
 * <strong style="color: purple">World</strong> </p>
 * @param toWhom
 * @return {string|string|*}
 * @constructor
 */
function MessagePattern(from, toWhom) {
    if (typeof from !== 'object')
        return from;
    if (from.MCType === 'MessagePattern')
        return from.getString();
    let result = "", isRaw = false;

    function select(selection) {
        function append(what) {
            result += what;
        }

        function includes(name) {
            return selection[name] !== undefined;
        }

        // Handle format
        if (!includes('raw')) {
            if (includes('format')) {
                append(textFormats[selection.format]);
            }
            if (includes('color')) {
                append(textFormats[selection.color]);
            }
        }
        /* Process text */
        if (includes('lang') && includes('text') && includes('raw')) throw "One node mustn't contain more than one text-like selection."
        // Handle language quote
        if (includes('lang')) {
            append(lang.got(
                toWhom === undefined ? (includes('name') ? selection.name : lang.defaultLangCode) : toWhom.getUserLanguage(),
                selection.lang,
                includes('args') ? args : []
            ))
        } else if (includes('text')) {
            append(selection.text);
        } else if (includes('raw')) {
            if (typeof selection.raw === 'object') {
                append(JSON.stringify(selection.raw))
            } else {
                append(selection.raw);
            }
            isRaw = true;
        }
    }

    if (Array.isArray(from)) {
        for (let i in from) {
            const selection = from[i];
            select(selection)
        }
    } else {
        select(from);
    }

    this.getString = () => result;
    this.isRaw = isRaw;
    this.MCType = "MessagePattern"
}

module.exports = MessagePattern;