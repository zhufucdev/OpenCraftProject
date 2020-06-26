package com.zhufu.opencraft.chunkgenerator

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.generator.ChunkGenerator
import java.util.*

class VoidGenerator(val spawnHeight: Int) : ChunkGenerator() {
    override fun getFixedSpawnLocation(world: World, random: Random): Location {
        return Location(world, 0.toDouble(), spawnHeight.toDouble(), 0.toDouble())
    }

    override fun generateChunkData(world: World, random: Random, x: Int, z: Int, biome: BiomeGrid): ChunkData {
        for (X in 0..15)
            for (Y in 0..255)
                for (Z in 0..15)
                    biome.setBiome(X, Y, Z, Biome.THE_VOID)
        return createChunkData(world)
    }
}