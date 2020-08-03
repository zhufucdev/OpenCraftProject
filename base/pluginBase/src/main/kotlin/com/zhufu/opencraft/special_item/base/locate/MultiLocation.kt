package com.zhufu.opencraft.special_item.base.locate

import org.bukkit.inventory.ItemStack

class MultiLocation(locations: List<ItemLocation>) : ItemLocation() {
    private val mLocations = arrayListOf<ItemLocation>()

    @Suppress("UNCHECKED_CAST")
    val locations get() = mLocations.clone() as List<ItemLocation>

    init {
        this.mLocations.addAll(locations)
    }

    override val itemStack: ItemStack
            by lazy { locations.firstOrNull { it.isAvailable }?.itemStack ?: error("No location is available.") }
    override val isAvailable: Boolean
        get() = try {
            itemStack; true
        } catch (ignored: Exception) {
            false
        }

    override fun push() {
        super.push()
        mLocations.forEach {
            it.itemStack.apply {
                itemMeta = itemStack.itemMeta
                type = itemStack.type
                amount = itemStack.amount
            }
            it.push()
        }
    }

    override fun serialize(): MutableMap<String, Any> {
        val r = mutableMapOf<String, Any>()
        mLocations.forEachIndexed { index, location ->
            r[index.toString()] = location
        }
        return r
    }

    companion object {
        @JvmStatic
        fun deserialize(d: Map<String, Any>): MultiLocation {
            return MultiLocation(d.values.map { it as ItemLocation })
        }
    }
}