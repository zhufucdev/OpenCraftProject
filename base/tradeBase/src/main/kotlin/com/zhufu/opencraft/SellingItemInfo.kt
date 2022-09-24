package com.zhufu.opencraft

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bson.Document
import org.bson.types.Binary
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.StringReader

class SellingItemInfo(val item: ItemStack, var unitPrise: Long, var amount: Int) : Cloneable {
    val prise: Long
        get() = unitPrise * amount

    fun toDocument() = Document()
        .append("item", item.serializeAsBytes())
        .append("uPrise", unitPrise)
        .append("amount", amount)

    companion object {
        fun of(document: Document) = SellingItemInfo(
            item = ItemStack.deserializeBytes(document.get("item", Binary::class.java).data),
            unitPrise = document.getLong("uPrise"),
            amount = document.getInteger("amount")
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is SellingItemInfo
                && other.item === this.item
                && other.prise == this.prise
    }

    override fun hashCode(): Int {
        var result = item.hashCode()
        result = 31 * result + prise.toInt()
        result = 31 * result + amount
        return result
    }

    public override fun clone(): SellingItemInfo {
        return super.clone() as SellingItemInfo
    }
}
