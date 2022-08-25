package com.zhufu.opencraft.special_block

import de.tr7zw.nbtapi.NBTBlock
import de.tr7zw.nbtapi.NBTCompound
import org.bukkit.Location
import java.util.*

/**
 * A special block with NBT access
 */
abstract class StatefulSpecialBlock(location: Location, id: UUID) : SpecialBlock(location, id) {
    private val nbt: NBTCompound

    init {
        nbt = NBTBlock(location.block).data
    }
}