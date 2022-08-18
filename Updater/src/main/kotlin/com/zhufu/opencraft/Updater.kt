package com.zhufu.opencraft

import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Updater : JavaPlugin() {
    companion object {
        val PLUGINS_DIR get() = File("plugins")
        val NEW_PLUGINS_DIR get() = File("new_plugins")
    }

    override fun onEnable() {
        getCommand("updater")!!.setExecutor(UpdaterCommandExecutor)
    }

    override fun onDisable() {
        if (!UpdaterCommandExecutor.issuedReload)
            return
        var success = 0
        var failure = 0
        logger.info("Reload issued by Updater.")
        NEW_PLUGINS_DIR.listFiles()?.forEach {
            if (!it.isHidden && it.isFile) {
                if (it.name == this.file.name) {
                    logger.warning("Will copy Updater itself, which should never happen.")
                }
                try {
                    it.copyTo(File(PLUGINS_DIR, it.name), overwrite = true)
                    success ++
                } catch (e: Exception) {
                    logger.warning("Error while copying ${it.name}.")
                    e.printStackTrace()
                    failure ++
                }
            }
        }
        logger.info("Updater finished with $success successes, $failure failures.")
    }
}