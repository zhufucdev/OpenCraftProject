'use thread'; // Puts a Executable {thread} into context.
/**
 * @param run {function} representing what to do in the new thread.
 * @param name {string} representing the name of the new thread.
 * @constructor
 */
function Thread(run, name) {
    let wrapper;
    if (name === undefined)
        wrapper = thread(run);
    else
        wrapper = thread(run, name);

    /**
     * Start the thread.
     * @returns {*}
     */
    this.start = () => wrapper.start();

    /**
     * Get the name of the thread.
     * @returns {string}
     */
    this.name = () => wrapper.getName();

    /**
     * Interrupt the thread.
     * @throws {*} if current thread is not permitted to do interruption.
     * @returns {*}
     */
    this.interrupt = () => wrapper.interrupt();
}

module.exports = Thread;