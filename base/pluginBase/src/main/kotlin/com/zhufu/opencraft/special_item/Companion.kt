package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.util.Language
import de.tr7zw.nbtapi.NBTCompound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import java.util.*
import kotlin.reflect.KClass

interface SICompanion {
    fun newInstance(getter: Language.LangGetter, madeFor: Player): SpecialItem
    val SIID: UUID
    val recipe: Recipe?
        get() = null
}

interface StatefulSICompanion : SICompanion {
    fun deserialize(specialItemID: UUID, nbt: NBTCompound, getter: Language.LangGetter): StatefulSpecialItem
    override fun newInstance(getter: Language.LangGetter, madeFor: Player): StatefulSpecialItem
}

interface StatelessSICompanion : SICompanion {
    fun fromItemStack(item: ItemStack): StatelessSpecialItem
}

interface Placeable {
    val block: KClass<*>
}