package com.zhufu.opencraft

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.StringReader

class SellingItemInfo(val item: ItemStack, var unitPrise: Prise<*>, var amount: Int) : Cloneable {
    val prise: Prise<*>
        get() = unitPrise * amount

    override fun equals(other: Any?): Boolean {
        return other is SellingItemInfo
                && other.item === this.item
                && other.prise == this.prise
    }

    override fun hashCode(): Int {
        var result = item.hashCode()
        result = 31 * result + prise.hashCode()
        result = 31 * result + amount
        return result
    }

    public override fun clone(): SellingItemInfo {
        return SellingItemInfo(item.clone(), unitPrise, amount)
    }
}
