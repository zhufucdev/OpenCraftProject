package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.util.Language
import de.tr7zw.nbtapi.NBTCompound
import de.tr7zw.nbtapi.NBTItem
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective
import org.reflections.Reflections
import java.util.UUID
import kotlin.reflect.full.companionObjectInstance

/**
 * A self-programmed item.
 *
 * @param id Identity of the item. If null, this item is [frozen].
 */
abstract class StatefulSpecialItem(m: Material, val getter: Language.LangGetter, id: UUID) :
    StatelessSpecialItem(m, id) {
    companion object {
        const val KEY_INSTANCE_ID = "si_instance_id"
        const val KEY_COMPOUND = "special_item"

        // Prebuilt special items by reflection
        private val prebuilt =
            Reflections("com.zhufu.opencraft.special_item").getSubTypesOf(StatefulSpecialItem::class.java)

        private val cache = mutableSetOf<StatefulSpecialItem>()
        private val adapters = arrayListOf<SpecialItemAdapter>()

        fun isSpecial(item: ItemStack): Boolean {
            val nbt = NBTItem(item)
            return nbt.hasKey(KEY_INSTANCE_ID)
        }

        operator fun get(item: ItemStack): StatefulSpecialItem? {
            if (item.type == Material.AIR) {
                return null
            }
            try {
                val nbt = NBTItem(item)
                val id = nbt.getUUID(KEY_INSTANCE_ID)
                val cached = cache.firstOrNull { it.specialItemID == id }
                if (cached != null) {
                    return cached
                } else {
                    val typeID = nbt.getUUID(KEY_SIID)
                    // construct from nbt
                    val c = prebuilt.firstOrNull { (it.kotlin.companionObjectInstance as StatefulSICompanion).SIID == typeID }
                    if (c != null) {
                        val compound = nbt.getCompound(KEY_COMPOUND)
                        val getter = Language.LangGetter(compound.getString("lang"))
                        return (c.kotlin.companionObjectInstance as StatefulSICompanion)
                            .deserialize(id, compound, getter)
                    }
                }
            } catch (_: Exception) {
            }
            return null
        }

        @JvmStatic
        fun byItemStack(itemStack: ItemStack) = this[itemStack]

        fun getAll(player: Player): List<StatefulSpecialItem> {
            val r = ArrayList<StatefulSpecialItem>()
            player.inventory.forEach {
                r.add(
                    StatefulSpecialItem[it] ?: return@forEach
                )
            }
            return r
        }

        @JvmStatic
        fun registerAdapter(adapter: SpecialItemAdapter) {
            if (!adapters.contains(adapter))
                adapters.add(adapter)
        }

        @JvmStatic
        fun unregister(adapter: SpecialItemAdapter) {
            adapters.remove(adapter)
        }

        @JvmStatic
        fun unregisterAll() {
            adapters.clear()
        }

        fun make(name: String, amount: Int, give: Player): StatelessSpecialItem? {
            val clazz = (prebuilt + StatelessSpecialItem.prebuilt).firstOrNull { it.simpleName.equals(name, true) }
            val getter = give.info().getter()
            if (clazz != null) {
                return (clazz.kotlin.companionObjectInstance as SICompanion)
                    .newInstance(getter, give)
                    .apply { setAmount(amount) }
            }
            val adapter = adapters.firstOrNull { it.name == name }
            if (adapter != null)
                return SpecialItemAdapter.AdaptedItem(adapter, getter).apply { setAmount(amount) }
            return null
        }

        val types
            get() = (prebuilt + StatelessSpecialItem.prebuilt).map { it.simpleName } + adapters.map { it.name }
    }

    val specialItemID: UUID = id
    protected val nbt: NBTCompound
    open fun tick(mod: PlayerModifier, data: YamlConfiguration, score: Objective, scoreboardSorter: Int) {}

    init {
        val nbt = NBTItem(this, true)
        nbt.setUUID(KEY_INSTANCE_ID, specialItemID)
        this.nbt = nbt.getOrCreateCompound(KEY_COMPOUND)
        cache.add(this)
    }

    override fun froze(): ItemStack {
        cache.remove(this)
        return super.froze()
    }

    override fun equals(other: Any?): Boolean {
        return other is StatefulSpecialItem && this.specialItemID == other.specialItemID
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        if (!frozen)
            result = 31 * result + specialItemID.hashCode()
        return result
    }
}