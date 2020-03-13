/**
 * Calls the {@param action} right after a module or Bukkit Plugin is loaded or enabled.
 * @param plugin Name of the module or plugin.
 * @example "bukkit:ServerCore" for Bukkit Plugin called ServerCore or "ss:initialization" for module plugins/initalization.js
 * @param action What to do right after the module or plugin is loaded.
 * @exception NoSuchModule When {@param plugin} is not mapped with a module or plugin, or the module it is mapped with is invalid.
 */
declare function after(plugin: string, action: () => any);

/**
 * Gets the export object of a specific module.
 * When the file tree of the server folder is as follows:
 * <p>
 * <p style="color:aqua">..</p>
 * <p style="color:aqua">.</p>
 * <p>paper.jar<p>
 * <p>server.properties</p>
 * <i>{...}</i>
 * <p style="color:aqua;">plugins</p>
 *  <p style="color:aqua; margin-left: 10px">.</p>
 *  <p style="color:aqua; margin-left: 10px">..</p>
 *  <p style="color:aqua; margin-left: 10px">ss_modules</p>
 *      <p style="color:aqua; margin-left: 20px">someModule</p>
 *          <p style="margin-left: 30px">main.js</p>
 *  <p style="margin-left: 10px"><strong>someModule.js</strong></p>
 *  <p style="margin-left: 10px"><strong>someOtherModule.js</strong></p>
 * </p>
 * Requiring "someModule" by {@link #require} as <strong>someOtherModule.js</strong> will get plugins/someModule.js's exports.
 * Requiring "someModule" as <strong>someModule.js</strong> will get plugins/ss_modules/someModule/main.js's exports.
 * @param module Name of the module to require.
 * @exception CircleDepend When the requester of this module is required by this module before this module exports.
 */
declare function require(module: string): any;

/**
 * @deprecated Use {@link require('object').javaClass} instead.
 * @param javaObject
 */
declare function getJavaClass(javaObject: JavaObject): JavaClass;

/**
 * @deprecated Use {@link require('object').fromJava} instead.
 * @param packageName
 */
declare function requireJava(packageName: string): JavaObject;

/**
 * @deprecated Use {@link require('object').javaPackages} instead.
 * @param prefix
 */
declare function listJava(prefix: String): [string]

/**
 * Represents any Java Object.
 */
declare class JavaObject {
    javaClass: JavaClass
}

/**
 * Represents any Java Class.
 */
declare class JavaClass extends JavaObject {
    getName(): string;
    getSimpleName(): string;
    getPackageName(): string;
    isAssignableFrom(clazz: JavaClass): boolean;
}