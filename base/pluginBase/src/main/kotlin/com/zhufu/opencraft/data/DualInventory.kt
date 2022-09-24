package com.zhufu.opencraft.data

import com.mongodb.client.model.Filters
import com.zhufu.opencraft.*
import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.data.DualInventory.Companion.NOTHING
import com.zhufu.opencraft.data.DualInventory.Companion.RESET
import com.zhufu.opencraft.data.Info.Companion.plugin
import org.bson.Document
import org.bson.types.Binary
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import javax.security.auth.Destroyable

class DualInventory(val parent: ServerPlayer) {
    internal val collection = Database.inventory(parent.uuid)

    fun delete() {
        collection.drop()
    }

    companion object {
        const val RESET = "reset"
        const val NOTHING = "none"

        fun resetPlayer(player: Player) {
            player.inventory.clear()
            player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
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

    var present = InventoryInfo(NOTHING, this)
    var last = InventoryInfo(NOTHING, this)

    fun getOrCreate(name: String = "default"): InventoryInfo {
        return InventoryInfo(name, this)
    }

    internal fun update(inventory: InventoryInfo) {
        collection.replaceOne(Filters.eq(inventory.name), inventory.doc)
    }
}

class InventoryInfo internal constructor(val name: String, val parent: DualInventory) :
    Destroyable {
    val player: Player? get() = parent.parent.onlinePlayerInfo?.player
    var inventoryOnly: Boolean = false

    val doc: Document = parent.collection.find(Filters.eq(name)).first()
        ?: Document("_id", name).also { parent.collection.insertOne(it) }

    fun update() {
        parent.update(this)
    }

    var gameMode: GameMode?
        get() {
            return GameMode.valueOf(doc.getString("gameMode") ?: return null)
        }
        set(value) {
            doc["gameMode"] = value
            update()
        }
    val location: Location?
        get() {
            return Location.deserialize(doc.get("location", Document::class.java) ?: return null)
        }

    private var destroyed = false
    override fun isDestroyed() = destroyed

    fun sync() {
        if (destroyed)
            throw IllegalAccessException("This object must not have been destroyed!")
        val player = this.player ?: throw IllegalStateException("Can't read player's info when it doesn't exists!")

        if (name == NOTHING) return

        Bukkit.getLogger()
            .info("Saving inventory named $name for player ${player.name}${if (inventoryOnly) "[InventoryOnly]" else ""}")
        val inventoryDoc = Document()
        player.inventory.forEachIndexed { index, itemStack ->
            if (itemStack == null || itemStack.type == Material.AIR)
                return@forEachIndexed
            inventoryDoc[index.toString()] = itemStack.serializeAsBytes()
        }
        doc["inventory"] = inventoryDoc

        if (!inventoryOnly) {
            doc["gameMode"] = player.gameMode.name
            doc["health"] = player.health
            doc["foodLevel"] = player.foodLevel
            doc["exp"] = player.totalExperience
            doc["location"] = Document(player.location.serialize())
            doc["potion"] = player.activePotionEffects.map { Document(it.serialize()) }
        }
        update()
    }

    fun load(savePresent: Boolean = true, inventoryOnly: Boolean = false) {
        if (destroyed)
            throw IllegalAccessException("This object must not be destroyed.")
        val player = this.player ?: throw IllegalStateException("Couldn't read player's info when it doesn't exists.")

        this.inventoryOnly = inventoryOnly
        if (savePresent)
            parent.present.sync()

        val event = PlayerLoadInventoryEvent(player, parent.present, this)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) {
            return
        }

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
                player.removePotionEffect(PotionEffectType.BLINDNESS)
                player.walkSpeed = 0.2f
                player.flySpeed = 0.1f
            } else {
                player.inventory.clear()
                return
            }
        }

        val failureList = ArrayList<String>()
        fun runCatching(key: String, block: () -> Unit) {
            if (doc.containsKey(key)) {
                try {
                    block()
                } catch (e: Exception) {
                    Bukkit.getLogger().warning("Error while loading inventory $name for player ${player.name}.")
                    e.printStackTrace()
                    failureList.add("player/$key: ${e.message}")
                }
            }
        }

        runCatching("inventory") {
            player.inventory.clear()
            val invDoc = doc.get("inventory", Document::class.java)
            for (i in 0 until player.inventory.size) {
                if (invDoc.containsKey(i.toString())) {
                    val data = invDoc.get(i.toString(), Binary::class.java)
                    val item = ItemStack.deserializeBytes(data.data)
                    player.inventory.setItem(i, item)
                }
            }
        }

        if (!inventoryOnly && name != RESET) {
            Bukkit.getScheduler().runTask(plugin) { _ ->
                player.fallDistance = 0f
                runCatching("location") {
                    when (val locationData = doc["location"]) {
                        is Document -> {
                            val location = Location.deserialize(locationData)
                            player.teleport(location)
                        }

                        is Location -> {
                            player.teleport(locationData)
                        }

                        else -> {
                            failureList.add("player/location: invalid argument")
                        }
                    }
                }

                runCatching("gameMode") {
                    player.gameMode = gameMode!!
                }
                runCatching("foodLevel") {
                    player.foodLevel = doc.getInteger("foodLevel")
                }
                runCatching("exp") {
                    player.totalExperience = doc.getInteger("exp")
                }
                player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
                runCatching("potion") {
                    doc.getList("potion", Document::class.java).forEachIndexed { index, document ->
                        val effect = PotionEffect(document)
                        try {
                            player.addPotionEffect(effect)
                        } catch (e: Exception) {
                            failureList.add("player/potion/$index: ${e::class.simpleName}: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
                runCatching("health") {
                    val health = doc.getDouble("health")
                    if (health <= player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value)
                        player.health = health
                    else {
                        failureList.add("player/health: health value reaches the top limit")
                    }
                }
            }
        }

        if (failureList.isNotEmpty()) {
            player.error(getLang(player, "user.error.whileLoading"))
            player.warn(failureList.joinToString())
        }
    }

    override fun destroy() {
        parent.collection.deleteOne(Filters.eq(name))
        destroyed = true
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(path: String): T? {
        return try {
            doc[path] as T?
        } catch (e: Exception) {
            null
        }
    }

    fun has(path: String) = doc.containsKey(path)

    fun set(path: String, value: Any) = doc.set(path, value)
    fun addItem(item: ItemStack): Boolean {
        val inventory = doc.get("inventory", Document::class.java) ?: Document()
        val max = inventory.maxOfOrNull { it.key.toInt() }
        fun msg() {
            doc["inventory"] = inventory
            update()
        }
        if (max == null) {
            inventory["0"] = item.serializeAsBytes()
            msg()
            return true
        } else {
            for (i in 0..if (max < 35) max + 1 else 35) {
                if (!inventory.containsKey(i.toString())) {
                    inventory[i.toString()] = item.serializeAsBytes()
                    msg()
                    return true
                }
            }
        }
        return false
    }

    fun setItem(index: Int, item: ItemStack?) {
        val invDoc = doc.get("inventory", Document::class.java) ?: return
        if (item == null) {
            invDoc.remove(index.toString())
        } else {
            invDoc[index.toString()] = item.serializeAsBytes()
        }
    }

    fun any(l: (ItemStack) -> Boolean): Boolean =
        doc.get("inventory", Document::class.java)
            ?.any {
                l(ItemStack.deserializeBytes((it.value as Binary? ?: return@any false).data))
            }
            ?: false

    override fun equals(other: Any?): Boolean {
        return other is InventoryInfo
                && other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
