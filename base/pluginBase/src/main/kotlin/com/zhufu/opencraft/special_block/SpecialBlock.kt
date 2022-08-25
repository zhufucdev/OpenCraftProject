package com.zhufu.opencraft.special_block

import de.tr7zw.nbtapi.NBTBlock
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.event.Listener
import org.reflections.Reflections
import java.util.UUID

/**
 * A self-programmed block.
 */
abstract class SpecialBlock(val location: Location, val type: UUID) {
    abstract val eventListener: Listener

    init {
        val nbt = NBTBlock(location.block)
        nbt.data.setUUID(KEY_ID, type)
    }

    protected fun Block.isThis() = try {
        location == this@SpecialBlock.location
    } catch (e: Exception) {
        false
    }

    companion object {
        const val KEY_ID = "sb_id"

        val predefined: Set<Class<out SpecialBlock>> = Reflections("com.zhufu.opencraft.special_block")
            .let {
                it.getSubTypesOf(StatelessSpecialBlock::class.java) +
                        it.getSubTypesOf(StatefulSpecialBlock::class.java)
            }
    }
}