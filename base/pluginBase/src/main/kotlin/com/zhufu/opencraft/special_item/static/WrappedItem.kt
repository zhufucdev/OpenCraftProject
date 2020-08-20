package com.zhufu.opencraft.special_item.static

import com.zhufu.opencraft.PlayerModifier
import com.zhufu.opencraft.special_item.Tickable
import net.minecraft.server.v1_16_R1.NBTTagCompound
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective

abstract class WrappedItem(material: Material) : ItemStack(material), Tickable {
    companion object {
        private val registered = arrayListOf<Class<WrappedItem>>()

        @JvmStatic
        fun register(clazz: Class<WrappedItem>) {
            if (registered.contains(clazz)) return
            registered.add(clazz)
        }

        fun isSpecial(itemStack: ItemStack) = CraftItemStack.asNMSCopy(itemStack).tag?.hasKey("wrapped_type") == true

        operator fun get(itemStack: ItemStack, player: Player? = null, slot: Int = -1): WrappedItem? {
            val type = CraftItemStack.asNMSCopy(itemStack).tag?.getString("wrapped_type") ?: return null
            val clazz = registered.firstOrNull { it.let { c -> c.canonicalName ?: c.simpleName } == type } ?: return null
            val instance = clazz.getConstructor().newInstance()
            instance.apply {
                amount = itemStack.amount
                itemMeta = itemStack.itemMeta
                onDisplay(player ?: return@apply, slot)
            }
            return instance
        }

        operator fun get(player: Player): List<WrappedItem> {
            val r = arrayListOf<WrappedItem>()
            player.inventory.forEachIndexed { index, it ->
                if (it == null) return@forEachIndexed
                r.add((get(it, player) ?: return@forEachIndexed).also { item -> item.onDisplay(player, index) })
            }
            return r
        }

        fun make(name: String, amount: Int, owner: Player, vararg args: Any): WrappedItem? {
            val clazz = registered.firstOrNull { it.simpleName == name }
                ?: return null
            val instance = clazz.getConstructor().newInstance()
            instance.amount = amount
            instance.onCreate(owner, *args)
            return instance
        }

        val types: List<String>
            get() = registered.map { it.simpleName }
    }

    open fun onCreate(owner: Player, vararg args: Any) {
        val tag = NBTTagCompound()
        tag.setString("wrapped_type", this::class.let { it.qualifiedName ?: it.simpleName })
        CraftItemStack.asCraftMirror(CraftItemStack.asNMSCopy(this).apply {
            setTag(tag)
        })
        holder = owner
    }

    fun push() {
        val holder = holder ?: error("holder must not be null.")
        val slot = slot
        val index = holder.inventory.getItem(slot)
        if (index == null || index.itemMeta != itemMeta) {
            error("${holder.name}.inventory[$slot] is not a ${this::class.simpleName}.")
        }

        holder.inventory.setItem(slot, this)
    }

    /**
     * Called each tick if the item is in a player's inventory.
     */
    override fun doPerTick(mod: PlayerModifier, contract: YamlConfiguration, score: Objective, scoreboardSorter: Int) {}

    private var holder: Player? = null
    private var slot: Int = -1
    /**
     * Called whenever [WrappedItem.get] is handed with a [Player] parameter.
     */
    open fun onDisplay(showing: Player, slot: Int) {
        holder = showing
        this.slot = slot
    }
}