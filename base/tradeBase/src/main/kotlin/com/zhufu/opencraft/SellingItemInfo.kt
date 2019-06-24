package com.zhufu.opencraft

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.StringReader

class SellingItemInfo(val item: ItemStack, var unitPrise: Long,var amount: Int): Cloneable{
    val prise: Long
    get() = unitPrise * amount
    fun appendToJson(writer: JsonWriter){
        val yaml = YamlConfiguration()
        yaml.set("YAML",item)
        writer
                .beginObject()
                .name("item").value(yaml.saveToString())
                .name("prise").value(unitPrise)
                .name("amount").value(amount)
                .endObject()
    }
    companion object {
        fun fromJson(reader: JsonReader): SellingItemInfo {
            reader.beginObject()
            var item: ItemStack? = null
            var prise: Long? = null
            var amount: Int? = null
            while (reader.hasNext()){
                when(reader.nextName()){
                    "item" -> {
                        val yaml = YamlConfiguration.loadConfiguration(StringReader(reader.nextString()))
                        item = yaml.getItemStack("YAML")
                    }
                    "prise" -> prise = reader.nextLong()
                    "amount" -> amount = reader.nextInt()
                }
            }
            reader.endObject()
            if (item == null || prise == null || amount == null){
                throw IllegalArgumentException("Could not deserialize Json for SellingItemInfo.")
            }
            return SellingItemInfo(item, prise, amount)
        }
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
