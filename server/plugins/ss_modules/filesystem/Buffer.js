const Thread = require('thread');
const object = require('object');
const read = object.fromJava('com.zhufu.opencraft.lang.Extension').readerToString;
/**
 * Buffer is used for file I/O.
 * Do not create this object on your own!
 * @param javaFile
 * @constructor
 */
function Buffer(javaFile) {
    const Writer = Java.type('java.io.FileWriter');
    const OutputStreamWriter = Java.type('java.io.OutputStreamWriter');
    const Reader = Java.type('java.io.FileReader');
    const OutputStream = Java.type('java.io.FileOutputStream');
    this.$wrapper = javaFile;

    /**
     * Gets the text content in UTF-8 of the file.
     * @return {string}
     */
    this.readText = () => {
        const reader = new Reader(javaFile);
        return read(reader);
    };
    /**
     * Writes the content of the file.
     * @param buffer {string|Buffer|File}
     */
    this.write = (buffer) => {
        const w = new Writer(javaFile);
        doWrite(w, buffer)
    };
    /**
     * Reads the text content in UTF-8 of the file async.
     * @param onresult {function(string)}
     */
    this.readTextAsync = (onresult) => {
        const t = new Thread({
            name: 'readerToString', in: object.fromJava('com.zhufu.opencraft.lang.Extension'),
            with: new Reader(javaFile), done: onresult
        });
        t.start()
    };
    /**
     * Appends new content to the end of the file.
     * @param buffer {string|Buffer|File}
     */
    this.append = (buffer) => {
        const w = new OutputStreamWriter(new OutputStream(javaFile, true));
        doWrite(w, buffer)
    };
    this.typeFs = "Buffer";

    function doWrite(writer, buffer) {
        if (typeof buffer === 'string') {
            writer.write(buffer, 0, buffer.length);
            writer.flush();
            writer.close();
        } else if (buffer.typeFs === 'Buffer') {
            const r = new Reader(buffer.$wrapper);
            copyReader(r, writer)
        } else if (buffer.typeFs === 'File') {
            const r = new Reader(buffer.buffer().$wrapper);
            copyReader(r, writer)
        } else {
            throw buffer + " is not writable."
        }
    }
}

module.exports = Buffer;
module.shareContext = false;