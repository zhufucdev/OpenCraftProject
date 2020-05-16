const Buffer = require('Buffer');
const object = require('object');

/**
 * File is a wrap of the Java File.
 * @param path The absolute or relative path of the file. The file of the path can not really exist.
 * @constructor
 */
function File(path) {
    const JavaFile = Java.type("java.io.File");
    const wrapper = new JavaFile(path);
    const Thread = require('thread');
    this.$wrapper = wrapper;
    /* Getters */
    this.prototype = object.withProperties({
        path: {getter: () => wrapper.getPath()},
        /**
         * Gets the name of the file with it's extension or rename the file.
         * @type {*|boolean} undefined if parameter rename is not defined, true if the file was renamed successfully otherwise.
         */
        name: {
            getter: () => wrapper.getName(),
            setter: (value) => wrapper.renameTo(JavaFile(wrapper.getParentFile(), rename))
        },
        absolutePath: {getter: () => wrapper.getAbsolutePath()},
        /**
         * @type {boolean} True if the file exists.
         */
        exists: {getter: () => wrapper.exists()},
        isDirectory: {getter: () => wrapper.isDirectory()},
        isFile: {getter: () => wrapper.isFile()},
        /**
         * Gets the extension of the file.
         * An extension of a file is include in it's name starting by {.}
         * @example A file named {File.js} has the extension of {js}.
         * @type {string|null}
         */
        extension: {
            getter: () => {
                const name = this.name();
                const index = name.lastIndexOf('.');
                if (index === -1) return null;
                return name.substr(index + 1)
            }
        },
        parent: { getter: () => new File(wrapper.getPath()) },
        /**
         * Gets all child files if the file is a directory.
         * @type {Array|null}
         */
        children: {
            getter: () => {
                if (this.isDirectory()) {
                    let result = Array.of();
                    const list = wrapper.list();
                    for (let p in list) {
                        result.push(new File(list[p]))
                    }
                    return result;
                }
                return null;
            }
        },
        /**
         * Gets whether this name of file is marked as hidden in the filesystem.
         * @type {boolean} True if the file is hidden.
         */
        isHidden: { getter: () => wrapper.isHidden() }
    });

    /**
     * Gets the file's child file if the file is a directory.
     * @param name {string}
     * @return {File|null}
     */
    this.child = (name) => {
        if (this.isDirectory()) return new File(this.path() + JavaFile.separatorChar);
        else return null;
    };

    /**
     * Gets the Buffer of the file for I/O.
     * @return {Buffer}
     */
    this.buffer = () => new Buffer(wrapper);

    /* Operations */
    /**
     * Deletes the file from disk sync.
     * @returns {boolean} True if the file is deleted successfully.
     */
    this.delete = () => wrapper.delete();
    /**
     * Deletes the file async.
     * @param onresult {function} which will be called when finished, and tells whether the operation was successful.
     */
    this.deleteAsync = (onresult) => {
        const t = new Thread({
            name: 'delete', in: wrapper,
            done: onresult
        });
        t.start();
    };
    /**
     * Creates the file if not existing on the disk.
     * @return {boolean} True if the file was successfully created.
     */
    this.touch = () => wrapper.createNewFile();
    /**
     * Creates the directory recursively if not existing on the disk.
     * @return {boolean} True if the directories were created.
     */
    this.mkdir = () => wrapper.mkdirs();

    this.typeFs = "File";
}

module.exports = File;
module.shareContext = false;