package com.zhufu.opencraft.special_block

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.UUID

interface SBCompanion {
    fun from(location: Location): SpecialBlock
    val material: Material
    val SBID: UUID
}

interface Dropable {
    val itemToDrop: ItemStack
}