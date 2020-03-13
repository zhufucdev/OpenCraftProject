/**
 * Object representing the module itself of every context.
 */
declare var module: {
    /**
     * Name of the module.
     * If the file of the module is named as "main.js" and is located under
     * any child directory of <i style="color:aqua">ss_modules</i>, it will get its name from it's
     * parent directory.
     */
    name: string;
    /**
     * Path of the module related to <i style="color:aqua">plugins</i>.
     */
    path: string;
    /**
     * Returns true if the directory where the module's parent directory locates at is
     * <i style="color:aqua">ss_modules</i>.
     */
    isDependency: boolean;

    exports: object | null;
    onDisable: () => any;
};