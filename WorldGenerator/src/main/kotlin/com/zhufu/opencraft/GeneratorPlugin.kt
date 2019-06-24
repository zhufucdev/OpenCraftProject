package com.zhufu.opencraft

import com.zhufu.opencraft.chunkgenerator.VoidGenerator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin

class GeneratorPlugin: JavaPlugin() {
    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator? =
        when(id) {
            "lobby" -> VoidGenerator(100)
            else -> null
        }
}