package com.zhufu.opencraft

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.generator.ChunkGenerator
import java.util.*

class TradeWorldGenerator : ChunkGenerator() {
    companion object {
        val base
            get() = 60
    }
    private val stoneSlab
        get() = Material.AIR
    override fun getFixedSpawnLocation(world: World, random: Random): Location = Location(world, 7.5, base + 2.toDouble(), 7.5)

    override fun generateChunkData(world: World, random: Random, x: Int, z: Int, biome: BiomeGrid): ChunkData {
        val data = createChunkData(world)
        for (X in 0..15) {
            for (Z in 0..15) {
                data.setBlock(X, base, Z, Material.BEDROCK)
                if (x == 0 && z == 0) {
                    data.setBlock(X, base + 1, Z, Material.WHITE_CONCRETE)
                } else {
                    data.setBlock(X, base + 1, Z, Material.GRASS_BLOCK)
                }
            }
        }

        if (x % 3 == 0 && z != 0) {
            buildRoad(data, true, z % 3 == 0)
        } else if (z % 3 == 0 && x != 0) {
            buildRoad(data, false, x % 3 == 0)
        }
        return data
    }

    private fun buildRoad(data: ChunkData, horizontal: Boolean, crossing: Boolean) {
        for (x in 3..12) {
            for (z in 0..15) {
                if (horizontal) {
                    data.setBlock(x, base + 1, z, Material.BLACK_CONCRETE)
                    if (crossing) {
                        for (X in 13..15)
                            for (Z in 3..12)
                                data.setBlock(X, base + 1, Z, Material.BLACK_CONCRETE)
                        for (X in 0..2)
                            for (Z in 3..12)
                                data.setBlock(X, base + 1, Z, Material.BLACK_CONCRETE)
                    }
                } else {
                    data.setBlock(z, base + 1, x, Material.BLACK_CONCRETE)
                    if (crossing) {
                        for (X in 3..12)
                            for (Z in 13..15)
                                data.setBlock(X, base + 1, Z, Material.BLACK_CONCRETE)
                        for (X in 3..12)
                            for (Z in 0..2)
                                data.setBlock(X, base + 1, Z, Material.BLACK_CONCRETE)
                    }
                }
            }
        }
        for (x in 0..15) {
            if (horizontal) {
                if (!crossing) {
                    data.setBlock(13, base + 2, x, stoneSlab)
                } else {
                    if (x in 0..2 || x in 13..15) {
                        data.setBlock(13, base + 2, x, stoneSlab)
                    }
                }
            } else {
                if (!crossing) {
                    data.setBlock(x, base + 2, 13, stoneSlab)
                } else {
                    if (x in 0..2 || x in 13..15) {
                        data.setBlock(x, base + 2, 13, stoneSlab)
                    }
                }
            }
        }
        if (horizontal && crossing) {
            data.setBlock(15, base + 2, 13, stoneSlab)
            data.setBlock(14, base + 2, 13, stoneSlab)

            data.setBlock(15, base + 2, 2, stoneSlab)
            data.setBlock(14, base + 2, 2, stoneSlab)
        } else if (!horizontal && crossing) {
            data.setBlock(2, base + 2, 15, stoneSlab)
            data.setBlock(2, base + 2, 14, stoneSlab)

            data.setBlock(13, base + 2, 15, stoneSlab)
            data.setBlock(13, base + 2, 14, stoneSlab)
        }

        for (x in 0..15) {
            if (horizontal) {
                if (!crossing) {
                    data.setBlock(2, base + 2, x, stoneSlab)
                } else {
                    if (x in 0..2 || x in 13..15) {
                        data.setBlock(2, base + 2, x, stoneSlab)
                    }
                }
            } else {
                if (!crossing) {
                    data.setBlock(x, base + 2, 2, stoneSlab)
                } else {
                    if (x in 0..2 || x in 13..15) {
                        data.setBlock(x, base + 2, 2, stoneSlab)
                    }
                }
            }
        }
        if (horizontal && crossing) {
            data.setBlock(1, base + 2, 13, stoneSlab)
            data.setBlock(1, base + 2, 2, stoneSlab)

            data.setBlock(0, base + 2, 2, stoneSlab)
            data.setBlock(0, base + 2, 13, stoneSlab)
        } else if (!horizontal && crossing) {
            data.setBlock(2, base + 2, 1, stoneSlab)
            data.setBlock(2, base + 2, 0, stoneSlab)

            data.setBlock(13, base + 2, 1, stoneSlab)
            data.setBlock(13, base + 2, 0, stoneSlab)
        }
    }
}