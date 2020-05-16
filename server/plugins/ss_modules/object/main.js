'use object, requireJava, getJavaClass, listJava, isModuleLoaded';
module.exports = {
    /**
     * Create an {@link #Object} with getters and setters.
     * @param config {{[memberKey]: { getter: function, setter: function }|{getter: function}|{setter: function}|*,...}}
     * @return {Object} to have created.
     */
    withProperties: (config) => object(config),
    /**
     * Require a Java object.
     * @param packageName {string} of the object to require.
     * @return {JavaObject}
     */
    fromJava: (packageName) => requireJava(packageName),
    /**
     * Gets an {@link #Array} of java packages whose name starts with the given prefix.
     * @param prefix {string} name of packages to start with.
     * @return {Array<string>} result.
     */
    javaPackages: (prefix) => listJava(prefix),
    /**
     * Gets the Java Class of a Java Object.
     * @param javaObject {Object} instance of the Java Object to get.
     * @return {JavaClass} Java Class for reflections.
     */
    javaClass: (javaObject) => getJavaClass(javaObject),
    /**
     * Gets whether the object is or of subclass of the given package.
     * @param obj {JavaObject} the object.
     * @param packageName {string|JavaObject} the Java package name.
     * @return {boolean} True if the object is of the given package.
     */
    isOfClass: (obj, packageName) => (typeof packageName === 'string' ? requireJava(packageName) : packageName).javaClass.isAssignableFrom(getJavaClass(obj)),
    /**
     * Adds members to the give object.
     * @param obj {Object} the object to be extended.
     * @param extension {Object} members to extend.
     */
    extend: (obj, extension) => {
        for (let key in extension) {
            obj[key] = extension[key]
        }
        return obj;
    },
    /**
     * Gets all keys of a object.
     * @param obj {Object} The object to get keys from.
     * @returns {[string|number]} Array of the keys.
     */
    keyOf: (obj) => {
        const r = [];
        for (let key in obj) {
            r.push(key);
        }
        return r
    },
    isModuleLoaded: (name) => isModuleLoaded(name)//TODO: Seperate into another package
};

module.shareContext = true;