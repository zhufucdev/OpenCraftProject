const File = require('File');
const Buffer = require('Buffer');
const JavaFile = Java.type('java.io.File');
module.exports = {
    separator: JavaFile.separatorChar,
    /**
     * Creates an instance of File with the give path.
     * @param path {string}
     * @return {File} represents the file of the path.
     */
    of: function (...path) {
        return new File(path.join(JavaFile.separatorChar));
    },
    /**
     * Writes the file with specific content.
     * @param file {File} to write.
     * @param buffer {string|Buffer|File} the content to write.
     */
    write: function (file, buffer) {
        file.buffer().write(buffer)
    },
    /**
     * Reads the text content in UTF-8 of the file.
     * @param file {File} to read.
     * @return {string}
     */
    read: function (file) {
        return file.buffer().readText()
    },
    /**
     * Copies file.
     * @param from {File} to copy from.
     * @param to {File} to copy to.
     * @param overwrite {Boolean} true if the file existing will be overwritten.
     * @return {boolean} true if the target file was written.
     */
    copy: function (from, to, overwrite) {
        if (!overwrite && to.exists()) return false;
        to.touch();
        to.buffer().write(from.buffer());
        return true;
    }
};
module.shareContext = false;