package com.zhufu.opencraft.special_item.base

import com.zhufu.opencraft.Base
import com.zhufu.opencraft.Game
import com.zhufu.opencraft.PlayerModifier
import com.zhufu.opencraft.special_item.base.locate.*
import net.minecraft.server.v1_16_R1.NBTTagCompound
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Objective
import org.reflections.Reflections
import java.io.File
import java.lang.reflect.Modifier
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.reflect

abstract class SpecialItem {
    companion object {
        // Utility
        // Prebuilt special item by reflection
        private val prebuilt = Reflections("com.zhufu.opencraft.special_item")
            .getSubTypesOf(SpecialItem::class.java)
            .filter { it.packageName.endsWith("special_item") && !Modifier.isAbstract(it.modifiers) }
        private val registered = arrayListOf<Class<out SpecialItem>>()

        // Existing special item for index
        private val mList = HashMap<UUID, SpecialItem>()

        // Existing inventory for item search
        private val inventories = arrayListOf<Inventory>()

        // Unloaded files for reloading
        private val unloaded = arrayListOf<File>()
        val root = Paths.get("plugins", "specialItems").toFile()

        fun save() {
            if (!root.exists()) root.mkdirs()

            var index = 0
            val iterator = mList.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                val id = next.key
                val item = next.value

                val file = File(root, "$index.yml")
                val config = YamlConfiguration()
                try {
                    item.onSaveStatus(config)

                    val store = YamlConfiguration()
                    val type = item::class.let { it.qualifiedName ?: it.simpleName }
                    if (type == null) {
                        Bukkit.getLogger().warning(
                            "[SpecialItem] Item at ${item.itemLocation} is anonymous which" +
                                    "can't be serialized. Ignoring."
                        )
                        continue
                    }
                    store.apply {
                        set("type", type)
                        set("SIID", id.toString())
                    }
                    if (config.getKeys(false).size > 0) {
                        store.set("data", config)
                    }

                    store.save(file)
                    index++
                } catch (e: RemoveRequest) {
                    Bukkit.getLogger().info("[SpecialItem] $item[$id] is being removed.")
                    item.onDestroy()
                    iterator.remove()
                } catch (e: Exception) {
                    Bukkit.getLogger().warning(
                        "[SpecialItem] Failed to save ${index}: ${e::class.qualifiedName}: " +
                                "${e.message}"
                    )
                    if (Game.env.getBoolean("debug", false)) e.printStackTrace()
                }
            }
            // Remove old files
            val old =
                root
                    .listFiles { f: File ->
                        !f.isHidden
                                && f.isFile
                                && f.extension == "yml"
                                && f.nameWithoutExtension.toIntOrNull().let { it == null || it >= mList.size }
                    }!!
            old.forEach { it.delete() }
        }

        private fun load(type: Class<out SpecialItem>, store: ConfigurationSection, file: String): Boolean {
            val instance =
                type.getConstructor().newInstance()
            var r = true
            val id = UUID.fromString(store.getString("SIID"))
            mList[id] = instance
            try {
                instance.onRestore(store.getConfigurationSection("data") ?: YamlConfiguration())
            } catch (e: Exception) {
                Bukkit.getLogger().throwing("SpecialItem", "init@$file", e)
                r = false
            }
            instance.onPostInit()
            return r
        }

        private fun destroy(siid: UUID): Boolean {
            val toRemove = mList[siid] ?: return false
            toRemove.onDestroy()
            mList.remove(siid)
            return true
        }

        private val mListener = object : Listener {
            @EventHandler
            fun onItemDespawn(event: ItemDespawnEvent) {
                val siid = getSIID(event.entity.itemStack) ?: return
                destroy(siid)
            }

            @EventHandler
            fun onInventoryClick(event: InventoryClickEvent) {
                val siid = getSIID(event.currentItem ?: return) ?: return
                mList[siid]?.onClick(event)
            }

            @EventHandler
            fun onInteract(event: PlayerInteractEvent) {
                val siid = getSIID(event.item ?: return) ?: return
                mList[siid]?.onInteractWith(event)
            }

            @EventHandler
            fun onDropItem(event: PlayerDropItemEvent) {
                val siid = getSIID(event.itemDrop.itemStack)
                mList[siid]?.onDrop(event)
            }

            @EventHandler
            fun onInventoryOpen(event: InventoryOpenEvent) {
                if (!inventories.contains(event.inventory))
                    inventories.add(event.inventory)
            }

            @EventHandler(priority = EventPriority.HIGHEST)
            fun onInventoryClose(event: InventoryCloseEvent) {
                if (event.inventory.holder == null) {
                    mList.filterValues { s -> event.inventory.any { it != null && getSIID(it) == s.SIID } }
                        .keys.forEach { destroy(it) }
                    inventories.remove(event.inventory)
                }
            }
        }

        fun init(plugin: JavaPlugin) {
            Reflections("com.zhufu.opencraft.special_item.base.locate").getSubTypesOf(ItemLocation::class.java)
                .forEach {
                    ConfigurationSerialization.registerClass(it)
                }

            val list =
                root.listFiles { f -> !f.isHidden && f.extension == "yml" && f.nameWithoutExtension.toIntOrNull() != null }
                    ?: return
            list.forEach { f ->
                val store = YamlConfiguration.loadConfiguration(f)
                val tS = store.getString("type")
                val type = prebuilt.firstOrNull { it.canonicalName == tS }
                    ?: registered.firstOrNull {
                        it.canonicalName == tS || it.simpleName == tS
                    }
                if (type == null) {
                    unloaded.add(f)
                    return@forEach
                }

                load(type, store, f.name)
            }

            if (unloaded.isNotEmpty()) {
                val move = File(root, "unloaded")
                move.mkdir()
                unloaded.forEachIndexed { index, file ->
                    file.renameTo(File(move, "$index.yml"))
                }
            }

            Bukkit.getPluginManager().registerEvents(mListener, plugin)
        }

        fun cleanUp() {
            HandlerList.unregisterAll(mListener)
            mList.values.forEach { it.onShutdown() }
        }

        @JvmStatic
        fun register(clazz: Class<out SpecialItem>): Boolean {
            if (registered.contains(clazz)) return false
            registered.add(clazz)
            val load = unloaded.mapNotNull {
                val temp = YamlConfiguration.loadConfiguration(it)
                (it to temp).takeIf { config ->
                    config.second.getString("type").let { s -> s == clazz.canonicalName || s == clazz.simpleName }
                }
            }

            load.forEach { (f, stored) ->
                if (load(clazz, stored, f.name)) {
                    f.renameTo(File(root, "${mList.size}.yml"))
                }
            }
            return true
        }

        @JvmStatic
        fun unregisterAll() {
            registered.forEach { r ->
                mList.filter { (_, item) -> item::class.java == r }.keys.forEach { destroy(it) }
            }
            registered.clear()
        }

        fun isSpecial(item: ItemStack): Boolean = getSIID(item) != null

        fun getByItem(item: ItemStack, showing: Player?) =
            mList[getSIID(item)]?.also { if (showing != null) it.onDisplay(showing) }

        fun getAll(player: Player): List<SpecialItem> {
            val r = ArrayList<SpecialItem>()
            player.inventory.forEach {
                r.add(
                    getByItem(it, player)
                        ?: return@forEach
                )
            }
            return r
        }

        operator fun get(siid: UUID): SpecialItem? = mList[siid]

        operator fun get(itemStack: ItemStack): SpecialItem? = getByItem(itemStack, null)

        operator fun get(entity: Item): SpecialItem? = get(entity.itemStack)

        @JvmStatic
        fun make(name: String, amount: Int = 1, owner: Player, vararg arguments: Any): SpecialItem? {
            val clazz = prebuilt.firstOrNull { it.simpleName.equals(name, true) }
                ?: throw IllegalArgumentException("$name is not a SpecialItem.")
            val instance = clazz.getConstructor().newInstance()
            mList[UUID.randomUUID()] = instance
            instance.itemLocation.itemStack.amount = amount
            instance.onCreate(owner, *arguments)
            instance.onPostInit()
            return instance
        }

        @JvmStatic
        fun getSIID(item: ItemStack?): UUID? {
            return CraftItemStack.asNMSCopy(item ?: return null).tag?.getString("siid")?.let {
                try {
                    UUID.fromString(it)
                } catch (ignored: Exception) {
                    null
                }
            }
        }

        val types: List<String>
            get() = arrayListOf<String>().apply {
                prebuilt.forEach { add(it.simpleName) }
            }
    }

    var itemLocation: ItemLocation = MemoryLocation(ItemStack(material))
    open fun findItem(): Boolean {
        // WARN: This method can be very slow, so it should be called only when the existing location is unusable.
        val id = SIID
        val pool = Executors.newCachedThreadPool()
        val tasks = mutableSetOf<Callable<Unit>>()
        val found = arrayListOf<ItemLocation>()
        // Look for players
        Bukkit.getOnlinePlayers().forEach { p ->
            tasks.add(
                Callable {
                    if (p.inventory.any { getSIID(it) == id }) {
                        found.add(PlayerLocation(p, id))
                    }
                }
            )
        }
        // Look for inventories
        inventories.forEach { i ->
            tasks.add(
                Callable {
                    if (i.any { getSIID(it) == id }) {
                        found.add(InventoryLocation(i, id))
                    }
                }
            )
        }
        // Look for entities
        val worlds = Bukkit.getWorlds()
        worlds.forEach { w ->
            tasks.add(
                Callable {
                    w.entities.forEach {
                        if (it is Item && getSIID(it.itemStack) == id) {
                            found.add(DroppedItemLocation(it))
                        }
                    }
                }
            )
            Unit
        }
        // Start searching
        try {
            val workers = pool.invokeAll(tasks, 2, TimeUnit.SECONDS)
            // => Wait from results
            while (!workers.all { it.isDone }) {
                Thread.sleep(200)
            }
        } catch (e: Exception) {
            return false
        } finally {
            pool.shutdownNow()
        }
        // Process
        if (found.size == 1) {
            itemLocation = found.first()
            return true
        } else if (found.size > 1) {
            itemLocation = MultiLocation(found)
            return true
        }
        // Still can't find. Use reserved.
        return false
    }

    val itemStack: ItemStack
        get() {
            if (itemLocation is MemoryLocation) {
                // To ensure the item is validate, search the whole game first.
                findItem()
            }
            return try {
                itemLocation.itemStack
            } catch (ignored: Exception) {
                // When an exception occurs, which means the item is not in MemoryLocation or could not be found
                // at the old place, search the whole game
                findItem()
                try {
                    itemLocation.itemStack
                } catch (e: Exception) {
                    // If the item is still unavailable, throw en exception.
                    error("Could not fetch item at $itemLocation: ${e.message}")
                }
            }
        }

    val SIID: UUID
        get() = mList.entries.firstOrNull { it.value == this }?.key
            ?: error("This special item is not registered. Constructions are only allowed through Special#make method.")

    abstract val material: Material

    private fun labelItem() {
        val tag = NBTTagCompound()
        tag.setString("siid", SIID.toString())
        (itemLocation as MemoryLocation).itemStack =
            CraftItemStack.asCraftMirror(CraftItemStack.asNMSCopy(itemLocation.itemStack).apply {
                setTag(tag)
            })
    }

    /**
     * Called at the first appearance of the item.
     * @param owner The supposed owner of this item. Specified in [SpecialItem.make].
     * @param args Extra arguments for this construction. Specified in [SpecialItem.make].
     */
    open fun onCreate(owner: Player, vararg args: Any) {
        labelItem()
    }

    /**
     * Called at the deserialization of the item.
     * @param savedStatus Data to deserialize.
     */
    open fun onRestore(savedStatus: ConfigurationSection) {
        if (savedStatus.contains("location"))
            savedStatus.getSerializable("location", ItemLocation::class.java)?.let { itemLocation = it }
        else {
            labelItem()
        }
    }

    /**
     * Called after either [onRestore] or [onCreate].
     */
    open fun onPostInit() {}

    /**
     * Called if [SpecialItem.getByItem] is handed in the second parameter.
     */
    open fun onDisplay(showing: Player) {}

    /**
     * Called at the serialization of this item.
     * @param config A yaml configuration where data is stored.
     */
    open fun onSaveStatus(config: ConfigurationSection) {
        val found = findItem()
        fun getSuitableLocation(from: ItemLocation): ItemLocation =
            when (from) {
                is PlayerLocation ->
                    if (found || !from.player.isOnline) MemoryLocation(from.itemStack)
                    else throw RemoveRequest()
                is MultiLocation -> {
                    val l = from.locations.map { getSuitableLocation(it) }
                    MultiLocation(l)
                }
                else -> from
            }

        config.set(
            "location",
            getSuitableLocation(itemLocation)
        )
    }

    /**
     * Called when this item is not supposed to exist any longer.
     */
    open fun onDestroy() {}

    /**
     * Called each tick if the item is in a player's inventory.
     */
    open fun doPerTick(mod: PlayerModifier, contract: YamlConfiguration, score: Objective, scoreboardSorter: Int) {}

    /**
     * Called when server is about to close.
     */
    open fun onShutdown() {}

    open fun onClick(event: InventoryClickEvent) {}

    open fun onInteractWith(event: PlayerInteractEvent) {}

    open fun onDrop(event: PlayerDropItemEvent) {}

    protected fun <T : Event> listenOther(clazz: Class<T>, listener: Consumer<T>) {
        Bukkit.getPluginManager()
            .registerEvent(clazz, mListener, EventPriority.NORMAL, { _, e ->
                try {
                    listener.accept(e as T)
                } catch (ignore: ClassCastException) {
                }
            }, Base.pluginCore)
    }

    class RemoveRequest : Exception()
}