package com.zhufu.opencraft.task.chunk_generator

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.util.noise.PerlinNoiseGenerator
import org.bukkit.util.noise.PerlinOctaveGenerator
import java.util.*
import kotlin.math.roundToInt

class FlatGenerator : ChunkGenerator() {
    override fun generateChunkData(world: World, random: Random, x: Int, z: Int, biome: BiomeGrid): ChunkData {
        val r = createChunkData(world)
        r.setRegion(0, 1, 0, 16, 11, 16, Material.STONE_BRICKS)
        r.setRegion(0, 0, 0, 16, 1, 16, Material.BEDROCK)
        return r
    }

    override fun getFixedSpawnLocation(world: World, random: Random): Location = Location(world, 0.0, 11.0, 0.0)

    override fun getDefaultPopulators(world: World): MutableList<BlockPopulator> =
        mutableListOf(LanternPopulator(), TerrainPopulator())

    class LanternPopulator : BlockPopulator() {
        override fun populate(world: World, random: Random, source: Chunk) {
            for (x1 in 0 until 16)
                for (z1 in 0 until 16) {
                    val x = (x1 + source.x * 16)
                    val z = (z1 + source.z * 16)
                    val height = (PerlinNoiseGenerator(random).noise(x.toDouble(), 19.0, z.toDouble()) * 6).toInt()
                    if (height == 0) continue
                    for (y in 20 - height until 20) {
                        world.getBlockAt(x, y, z).setType(Material.SEA_LANTERN, false)
                    }
                }
        }
    }

    class TerrainPopulator : BlockPopulator() {
        override fun populate(world: World, random: Random, source: Chunk) {
            if (random.nextDouble() > 0.1) return
            val x = random.nextInt(15)
            val z = random.nextInt(15)
            // "Tower"
            for (x1 in x..x + 1)
                for (z1 in z..z + 1) {
                    val height = (PerlinNoiseGenerator(random).noise(
                        source.x * 16.0 + x1,
                        11.0,
                        source.z * 16.0 + z1
                    ) * 2).roundToInt() + 1
                    for (y in 11..11 + height) {
                        source.getBlock(x1, y, z1).type =
                            if (random.nextBoolean()) Material.CHISELED_STONE_BRICKS else Material.MOSSY_COBBLESTONE
                    }
                }
            // Base
            val X = source.x * 16 + x
            val Z = source.z * 16 + z
            for (x2 in X - 2..X + 3)
                for (z2 in Z - 2..Z + 3) {
                    if ((x2 == X - 2 || x2 == X + 3) && (z2 == Z - 2 || z2 == Z + 3)) continue
                    world.getBlockAt(x2, 10, z2).type = Material.COARSE_DIRT
                }
        }
    }
}