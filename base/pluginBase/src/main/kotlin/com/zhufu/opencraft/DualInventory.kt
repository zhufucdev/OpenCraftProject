package com.zhufu.opencraft

import com.zhufu.opencraft.Info.Companion.plugin
import com.zhufu.opencraft.special_item.base.SpecialItem
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.potion.PotionEffect
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class DualInventory(val player: Player? = null, private val parent: ServerPlayer) {
    private val files: List<File>
        get() = parent.inventoriesFile
            .also { if (!it.exists()) it.mkdirs() }
            .listFiles()!!.filter { it.isFile }
    private val mList = ArrayList<InventoryInfo>()

    fun delete() {
        files.forEach {
            if (!it.delete()) {
                throw IllegalStateException("Could not delete ${it.path}")
            }
        }
        parent.inventoriesFile.delete()
        mList.forEach { it.destroy() }
        mList.clear()
    }

    init {
        files.forEach {
            try {
                val t = InventoryInfo(player, it.nameWithoutExtension, it, this)
                mList.add(t)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    companion object {
        const val RESET = "reset"
        const val NOTHING = "none"

        fun resetPlayer(player: Player) {
            player.inventory.clear()
            player.healthScale = 20.toDouble()
            player.foodLevel = 20
            player.totalExperience = 0
            player.walkSpeed = 0.2f
            player.flySpeed = 0.1f
            player.totalExperience = 0
            player.activePotionEffects.forEach {
                player.removePotionEffect(it.type)
            }
            player.allowFlight = player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR
        }
    }

    var present = InventoryInfo(player, NOTHING, File(""), this)
    var last = InventoryInfo(player, NOTHING, File(""), this)
    fun load(name: String = "default") {
        present.save()

        val index = mList.indexOfFirst { it.name == name }
        if (index == -1) throw InventoryNotFoundException(name, "Have you created it?")
        mList[index].load()
    }

    fun save(name: String = "default") {
        val index = mList.indexOfFirst { it.name == name }
        if (index == -1) throw InventoryNotFoundException(name, "Have you created it?")
        mList[index].save()
    }

    fun create(name: String = "default"): InventoryInfo {
        val e = mList.firstOrNull { it.name == name }
        if (e == null) {
            val element = InventoryInfo(
                player,
                name,
                File(
                    File("plugins${File.separatorChar}inventories${File.separatorChar}${parent.uuid}").also { if (!it.exists()) it.mkdirs() },
                    "$name.yml"
                ),
                this
            )
            mList.add(element)
            return element
        }
        return e
    }

    fun destroy(name: String): Boolean {
        val index = mList.indexOfFirst { it.name == name }
        if (index == -1)
            return false
        mList[index].destroy()
        mList.removeAt(index)
        return true
    }

    fun forEach(l: (InventoryInfo) -> Unit) = mList.forEach(l)
    class InventoryNotFoundException(which: String, msg: String) : Exception("No such inventory: $which. $msg")

    class InventoryInfo(val player: Player?, val name: String, val file: File, val parent: DualInventory) {
        var inventoryOnly: Boolean = false

        private val config: YamlConfiguration
        var isDestroyed = false
            private set

        init {
            validateFile()
            config = when {
                file.exists() -> YamlConfiguration.loadConfiguration(file)
                name != NOTHING && name != RESET -> {
                    file.createNewFile()
                    YamlConfiguration()
                }
                else -> YamlConfiguration()
            }
        }

        private fun validateFile(): Boolean {
            if (file.path.isNotEmpty() && !file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                return false
            }
            return true
        }

        fun save() {
            if (isDestroyed)
                throw IllegalAccessException("This object must not have been destroyed!")
            if (player == null)
                throw IllegalStateException("Can't read player's info when it doesn't exists!")

            if (name == NOTHING) return

            Bukkit.getLogger()
                .info("Saving inventory named $name for player ${player.name}${if (inventoryOnly) "[InventoryOnly]" else ""}")
            player.inventory.forEachIndexed { index, itemStack ->
                val path = "inventory.$index"
                config.set(path, null)
                if (itemStack == null)
                    return@forEachIndexed

                config.set(
                    path,
                    SpecialItem.getSIID(itemStack)?.toString() ?: itemStack
                )
            }

            if (!inventoryOnly) {
                config.set("gameMode", player.gameMode.name)
                config.set("health", player.health)
                config.set("foodLevel", player.foodLevel)
                config.set("walkSpeed", player.walkSpeed)
                config.set("flySpeed", player.flySpeed)
                config.set("exp", player.totalExperience)
                config.set("location", player.location)

                val potion = config.createSection("potion")
                potion.getKeys(false).forEach {
                    potion.set(it, null)
                }
                player.activePotionEffects.forEach {
                    potion.set(it.type.name, it)
                }
                config.set("potion", potion)
            }
            config.save(file)
        }

        fun load(savePresent: Boolean = true, inventoryOnly: Boolean = false) {
            if (isDestroyed)
                throw IllegalAccessException("This object must not be destroyed!")
            if (player == null)
                throw IllegalStateException("Can't read player's info when it doesn't exists!")

            this.inventoryOnly = inventoryOnly
            if (savePresent)
                parent.present.save()

            Bukkit.getPluginManager().callEvent(PlayerLoadInventoryEvent(player, parent.present, this))

            parent.last = parent.present
            parent.present = this
            Bukkit.getLogger().info("Present inventory of ${player.name} is ${this.name}")

            if (name == NOTHING)
                return
            else if (name == RESET) {
                if (!inventoryOnly) {
                    ServerCaller["SolvePlayerLobby"]?.invoke(
                        listOf(
                            player.info()
                                ?: throw IllegalStateException("Could not found ${player.name}'s info to rest.")
                        )
                    ) ?: Bukkit.getLogger().warning("SolvePlayerLobby of ServerCaller is missing out.")
                    player.gameMode = GameMode.CREATIVE
                } else {
                    player.inventory.clear()
                    return
                }
            }
            if (!validateFile()) {
                resetPlayer(player)
                return
            }

            val failureList = ArrayList<String>()
            player.inventory.clear()
            if (config.isSet("inventory")) {
                for (i in 0 until player.inventory.size) {
                    val path = "inventory.$i"
                    val item =
                        if (config.isString(path)) {
                            fun spawnUnknownItem() = ItemStack(Material.BARRIER).updateItemMeta<ItemMeta> {
                                val getter = player.getter()
                                setDisplayName(getter["inventory.siNotFound.title"].toErrorMessage())
                                lore =
                                    TextUtil.formatLore(getter["inventory.siNotFound.subtitle", config.getString(path)])
                                        .map { TextUtil.info(it) }
                            }

                            val id = try {
                                UUID.fromString(config.getString(path))
                            } catch (ignored: IllegalArgumentException) {
                                null
                            }
                            if (id == null) spawnUnknownItem()
                            else try { SpecialItem[id]?.itemStack } catch (e: Exception) { null }?: spawnUnknownItem()
                        } else
                            config.getItemStack(path, ItemStack(Material.AIR))
                    player.inventory.setItem(i, item)
                }
            }

            if (!inventoryOnly && name != RESET) {
                Bukkit.getScheduler().runTask(plugin) { _ ->
                    player.fallDistance = 0f
                    val location = config.getSerializable("location", Location::class.java, null)
                    if (location != null)
                        player.teleport(location)
                    else {
                        failureList.add("player/location: invalid argument")
                    }

                    if (config.isSet("gameMode"))
                        player.gameMode = GameMode.valueOf(config["gameMode"] as String)

                    if (config.isSet("health")) {
                        val health = config.getDouble("health")
                        if (health <= 20)
                            player.health = health
                        else {
                            failureList.add("player/health: health value reaches the top limit")
                        }
                    }
                    if (config.isSet("foodLevel"))
                        player.foodLevel = config.getInt("foodLevel")
                    if (config.isSet("exp"))
                        player.totalExperience = config.getInt("exp")
                    if (config.isSet("walkSpeed"))
                        player.walkSpeed = config.getDouble("walkSpeed").toFloat()
                    if (config.isSet("flySpeed"))
                        player.flySpeed = config.getDouble("flySpeed").toFloat()
                    //For Potion Effects
                    player.activePotionEffects.forEach {
                        player.removePotionEffect(it.type)
                    }
                    config.getConfigurationSection("potion")?.getKeys(false)?.forEach {
                        try {
                            player.addPotionEffect(config.getSerializable("potion.$it", PotionEffect::class.java)!!)
                        } catch (e: Exception) {
                            failureList.add("player/potion/$it: ${e::class.simpleName}: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }

            if (failureList.isNotEmpty()) {
                player.error(getLang(player, "user.error.whileLoading"))
                player.warn(
                    buildString {
                        failureList.forEach {
                            append(it)
                            append(',')
                        }
                        deleteCharAt(lastIndex)
                    }
                )
            }
        }

        fun destroy() {
            if (file.exists()) file.delete()
            isDestroyed = true
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> get(path: String): T? {
            return try {
                config.get(path) as T?
            } catch (e: Exception) {
                null
            }
        }

        fun has(path: String) = config.isSet(path)

        fun set(path: String, value: Any) = config.set(path, value)
        fun addItem(item: ItemStack): Boolean {
            val inventory = config.getConfigurationSection("inventory") ?: YamlConfiguration()
            val itemStack: Any = SpecialItem.getByItem(item, player) ?: item
            val max = inventory.getKeys(false).maxBy { key -> key.toInt() }?.toIntOrNull()
            fun msg(i: Int) {
                config.set("inventory", inventory)
                this.player?.sendMessage(TextUtil.success("物品已添加至您的${name}物品栏第${i + 1}格"))
            }
            if (max == null) {
                inventory.set("0", itemStack)
                msg(0)
                return true
            } else {
                for (i in 0..if (max < 35) max + 1 else 35) {
                    if (!inventory.isSet(i.toString())) {
                        inventory.set(i.toString(), itemStack)
                        msg(i)
                        return true
                    }
                }
            }
            return false
        }

        fun setItem(index: Int, item: ItemStack?) = config.set(
            "inventory.$index",
            item?.let { SpecialItem.getByItem(item, player)?.itemStack } ?: item
        )

        fun any(l: (ConfigurationSection) -> Boolean): Boolean =
            config.getConfigurationSection("inventory")
                ?.getKeys(false)
                ?.any {
                    val section = config.getConfigurationSection("inventory.$it")
                    l(section ?: return@any false)
                }
                ?: false

        fun items(): List<ItemStack?> {
            val result = arrayListOf<ItemStack?>()
            val config = config.getConfigurationSection("inventory") ?: YamlConfiguration()
            (0..35).forEach {
                val item = config.get(it.toString())
                if (item is ItemStack) {
                    result.add(item)
                } else if (item is String) {
                    result.add(SpecialItem[UUID.fromString(item)]?.itemStack ?: return@forEach)
                }
            }
            return result
        }

        override fun equals(other: Any?): Boolean {
            return other is InventoryInfo
                    && other.name == this.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }
}