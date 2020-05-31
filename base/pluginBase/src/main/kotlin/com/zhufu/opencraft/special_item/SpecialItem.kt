package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.Language
import com.zhufu.opencraft.PlayerModifier
import com.zhufu.opencraft.getter
import com.zhufu.opencraft.info
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective
import org.reflections.Reflections
import kotlin.jvm.internal.Reflection
import kotlin.reflect.full.companionObjectInstance

abstract class SpecialItem(m: Material, val getter: Language.LangGetter) : ItemStack(m) {
    companion object {
        // Prebuilt special items by reflection
        private val prebuilt = Reflections("com.zhufu.opencraft.special_item").getSubTypesOf(SpecialItem::class.java)

        fun isSpecial(config: ConfigurationSection?): Boolean {
            return config?.getBoolean("isSpecialItem", false) ?: false
        }

        fun isSpecial(item: ItemStack): Boolean {
            if (prebuilt.any {
                    (Reflection.createKotlinClass(it).companionObjectInstance as SISerializable?)?.isThis(item) == true
                })
                return true
            if (adapters.any { it.isThis(item) })
                return true

            return false
        }

        fun getByConfig(config: ConfigurationSection, getter: Language.LangGetter): SpecialItem? {
            prebuilt.forEach {
                val instance = Reflection.createKotlinClass(it).companionObjectInstance as SISerializable?
                if (instance?.isThis(config) == true)
                    return instance.deserialize(config, getter)
            }
            adapters.forEach {
                if (it.isThis(config)) {
                    val item = SpecialItemAdapter.AdapterItem(it, getter)
                    if (it.deserialize != null) {
                        (it.deserialize)(item, config, getter)
                    } else {
                        Bukkit.getLogger().warning(
                            "[SpecialItem] Adapter ${it.name} doesn't contain any method of " +
                                    "deserialization."
                        )
                    }
                    return item
                }
            }
            return null
        }

        fun getByItem(item: ItemStack, getter: Language.LangGetter): SpecialItem? {
            prebuilt.forEach {
                val instance = Reflection.createKotlinClass(it).companionObjectInstance as SISerializable?
                if (instance?.isThis(item) == true)
                    return instance.deserialize(item, getter)
            }
            adapters.forEach {
                if (it.isThis(item))
                    return SpecialItemAdapter.AdapterItem(it, item, getter)
            }
            return null
        }

        fun getSerialized(item: ItemStack, getter: Language.LangGetter) =
            getByItem(item, getter)?.getSerialized() ?: YamlConfiguration().apply { set("item", item) }

        fun getAll(player: Player): List<SpecialItem> {
            val r = ArrayList<SpecialItem>()
            val getter = player.getter()
            player.inventory.forEach {
                r.add(
                    getByItem(it, getter) ?: return@forEach
                )
            }
            return r
        }

        private val adapters = arrayListOf<SpecialItemAdapter>()

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

        fun make(name: String, amount: Int, give: Player): SpecialItem? {
            val clazz = prebuilt.firstOrNull { it.simpleName.equals(name, true) }
            val getter = give.info().getter()
            if (clazz != null) {
                if (clazz == Insurance::class.java) {
                    return Insurance(getter, give.name)
                }
                return (clazz.getConstructor(Language.LangGetter::class.java)
                    .newInstance(getter) as SpecialItem).apply { setAmount(amount) }
            }
            val adapter = adapters.firstOrNull { it.name == name }
            if (adapter != null)
                return SpecialItemAdapter.AdapterItem(adapter, getter).apply { setAmount(amount) }
            return null
        }

        val types
            get() = arrayListOf<String>().apply {
                prebuilt.forEach { add(it.simpleName) }
                adapters.forEach { add(it.name) }
            }
    }

    var inventoryPosition: Int = -1
    open fun getSerialized(): ConfigurationSection {
        return YamlConfiguration().apply {
            set("isSpecialItem", true)
            set("type", this@SpecialItem::class.simpleName)
        }
    }

    open fun doPerTick(mod: PlayerModifier, data: YamlConfiguration, score: Objective, scoreboardSorter: Int) {}
}