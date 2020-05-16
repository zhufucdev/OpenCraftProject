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
    /**
     * Whether to use a unqiue context for all requests.
     * If a module doesn't share context, it will take more resource, as every varible is independent.
     * It will be non-thread-blocking, however, which means every single cross-context call doesn't need to wait for
     * the previous one to finish.
     * @default true
     */
    shareContext: boolean;

    exports: object | null;
    onDisable: () => any;
};