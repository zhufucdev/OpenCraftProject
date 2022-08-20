package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.TextUtil
import com.zhufu.opencraft.util.toComponent
import de.tr7zw.nbtapi.NBTCompound
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.BookMeta
import java.util.UUID

class Insurance(
    getter: Language.LangGetter,
    val player: String,
    val number: Long = System.currentTimeMillis(),
    id: UUID = UUID.randomUUID()
) :
    StatefulSpecialItem(Material.WRITTEN_BOOK, getter, id, SIID) {

    init {
        nbt.setString("player", player)
        nbt.setLong("number", number)
        updateMeta(getter)
    }

    override fun updateMeta(getter: Language.LangGetter) {
        updateItemMeta<BookMeta> {
            title = TextUtil.info(getter["insurance.name"])
            author = getter["insurance.content.2"]
            addPages(buildString {
                appendLine(getter["insurance.content.1", player])
                for (i in 2..3)
                    appendLine(getter["insurance.content.$i"])
            }.toComponent())

            lore(listOf(player.toComponent(), number.toString().toComponent()))
            addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS)
            isUnbreakable = true
            addUnsafeEnchantment(Enchantment.KNOCKBACK, 1)
        }
    }

    companion object : StatefulSICompanion {
        override fun deserialize(
            specialItemID: UUID,
            nbt: NBTCompound,
            getter: Language.LangGetter
        ): StatefulSpecialItem =
            Insurance(
                getter,
                nbt.getString("player"),
                nbt.getLong("number"),
                specialItemID
            )

        override fun newInstance(getter: Language.LangGetter, madeFor: Player): StatefulSpecialItem = Insurance(getter, madeFor.name)

        override val SIID: UUID
            get() = UUID.fromString("10DABED2-51AD-4F8D-9CD4-7C09A4E2DECC")

        const val PRICE = 100
    }

}