package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.util.Language
import de.tr7zw.nbtapi.NBTCompound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

interface SICompanion {
    fun newInstance(getter: Language.LangGetter, madeFor: Player): StatelessSpecialItem
    val SIID: UUID
}

interface StatefulSICompanion : SICompanion {
    fun deserialize(specialItemID: UUID, nbt: NBTCompound, getter: Language.LangGetter): StatefulSpecialItem
    override fun newInstance(getter: Language.LangGetter, madeFor: Player): StatefulSpecialItem
}

interface StatelessSICompanion : SICompanion {
    fun fromItemStack(item: ItemStack): StatelessSpecialItem
}