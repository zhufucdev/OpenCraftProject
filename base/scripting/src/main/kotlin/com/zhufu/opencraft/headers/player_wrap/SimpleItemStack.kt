package com.zhufu.opencraft.headers.player_wrap

import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class SimpleItemStack private constructor(private val wrap: ItemStack) {
    fun getAmount() = wrap.amount
    fun getType() = wrap.type
    fun getLore() = wrap.lore
    fun getDamage() = wrap.itemMeta.let { if (it is Damageable) it.damage else 0 }
    fun getDisplayName() = wrap.itemMeta?.displayName

    fun hasEnchant() = wrap.enchantments.isNotEmpty()
    fun hasEnchant(name: String) = wrap.enchantments.keys.any { it.key.key == name }
    fun getEnchants(): List<String> {
        val r = arrayListOf<String>()
        wrap.enchantments.keys.forEach {
            r.add(it.key.key)
        }
        return r
    }

    fun getEnchantLevel(enchant: String) =
        wrap.enchantments.getOrDefault(Enchantment.getByKey(NamespacedKey.minecraft(enchant))!!, 0)

    companion object {
        fun from(itemStack: ItemStack): SimpleItemStack {
            return SimpleItemStack(itemStack)
        }
        fun recover(itemStack: SimpleItemStack?) = itemStack?.wrap
    }
}

fun Collection<ItemStack?>.toSimpleItemStackList(): List<SimpleItemStack?> {
    val r = arrayListOf<SimpleItemStack?>()
    this.forEach {
        r.add(
            if (it != null)
                SimpleItemStack.from(it)
            else
                null
        )
    }
    return r
}