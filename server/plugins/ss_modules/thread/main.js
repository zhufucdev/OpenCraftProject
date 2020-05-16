'use callAsyncIn'; // Puts Executable {callAsyncIn} into context.
const thread = require('object').fromJava('com.zhufu.opencraft.lang.Extension').thread;
/**
 * @param run {function|{name: string, in: JavaObject, with: Array, done: function}} representing what to do in the new thread.
 * @param name {string} representing the name of the new thread.
 * @constructor
 */
function Thread(run, name) {
    let wrapper;
    if (typeof run !== "function") {
        if (run.name && run.in) {
            if (run.with) {
                if (!Array.isArray(run.with)) {
                    run.with = [run.with];
                }
                if (run.done) {
                    if (name)
                        wrapper = callAsyncIn(run.in, run.name, run.with, run.done, name)
                    else
                        wrapper = callAsyncIn(run.in, run.name, run.with, run.done)
                }
                else {
                    if (name)
                        wrapper = callAsyncIn(run.in, run.name, run.with, null, name)
                    else
                        wrapper = callAsyncIn(run.in, run.name, run.with)
                }
            } else {
                if (run.done) {
                    if (name)
                        wrapper = callAsyncIn(run.in, run.name, [], run.done, name)
                    else
                        wrapper = callAsyncIn(run.in, run.name, [], run.done)
                }
                else {
                    if (name)
                        wrapper = callAsyncIn(run.in, run.name, [], null, name)
                    else
                        wrapper = callAsyncIn(run.in, run.name, [])
                }
            }
        } else throw 'Parameter {run} must be a Function or Object with {name} and {in}.'
    } else {
        if (name === undefined)
            wrapper = thread(run);
        else
            wrapper = thread(run, name);
    }

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
module.shareContext = false;