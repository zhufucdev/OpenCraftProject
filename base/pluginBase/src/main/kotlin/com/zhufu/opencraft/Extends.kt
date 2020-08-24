package com.zhufu.opencraft

import com.zhufu.opencraft.special_item.Tickable
import com.zhufu.opencraft.special_item.dynamic.SpecialItem
import com.zhufu.opencraft.special_item.static.WrappedItem
import com.zhufu.opencraft.util.ActionBarTextUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.Vector
import org.jetbrains.annotations.Contract
import java.io.File
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.security.MessageDigest
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.*
import kotlin.reflect.full.isSuperclassOf

fun getLang(lang: String, value: String, vararg replaceWith: Any?): String = Language.got(lang, value, replaceWith)
fun getLang(player: ServerPlayer, value: String, vararg replaceWith: Any?): String =
    Language.got(lang = player.userLanguage, value = value, replaceWith = replaceWith)

fun getLang(sender: CommandSender, value: String, vararg replaceWith: Any?): String {
    return if (sender is Player) {
        val info = OfflineInfo.findByUUID(sender.uniqueId)
            ?: return Language.got(Language.LANG_ZH, "player.error.unknown", null)
        Language.got(info.userLanguage, value, replaceWith)
    } else {
        Language.got(Language.default.getString("info.code")!!, value, replaceWith)
    }
}

fun getLangGetter(player: ChatInfo?): Language.LangGetter {
    return if (player != null) {
        Language[player.targetLang]
    } else {
        Language.LangGetter(Language.default)
    }
}

fun HumanEntity.info() = Info.findByPlayer(uniqueId)
fun OfflinePlayer.offlineInfo() = OfflineInfo.findByUUID(uniqueId)
fun CommandSender?.getter() = getLangGetter(if (this is HumanEntity) this.info() else null)
fun ChatInfo?.getter() = getLangGetter(this)

fun CommandSender.success(msg: String) {
    this.sendMessage(TextUtil.success(msg))
}

fun CommandSender.info(msg: String) {
    this.sendMessage(TextUtil.info(msg))
}

fun CommandSender.error(msg: String) {
    this.sendMessage(TextUtil.error(msg))
}

fun CommandSender.tip(msg: String) {
    this.sendMessage(TextUtil.tip(msg))
}

fun CommandSender.warn(msg: String) {
    this.sendMessage(TextUtil.warn(msg))
}

fun Player.sendActionText(msg: String) {
    ActionBarTextUtil.sendActionText(this, msg)
}

fun runSync(l: () -> Unit) {
    Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
        l()
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T : ItemMeta> ItemStack.updateItemMeta(block: T.() -> Unit): ItemStack {
    itemMeta = (itemMeta as T).apply(block)
    return this
}

fun File.size(): Long =
    if (this.isDirectory) {
        var size = 0L
        this.listFiles()?.forEach {
            size += it.size()
        }
        size
    } else {
        inputStream().channel.size()
    }

fun File.MD5(): String {
    val md = MessageDigest.getInstance("MD5")
    val input = inputStream()
    md.update(input.channel.map(FileChannel.MapMode.READ_ONLY, 0, length()))
    input.close()
    return BigInteger(1, md.digest()).toString(16)
}

fun Pair<Location, Location>.center(): Location {
    val x = (first.x - second.x) / 2
    val y = (first.y - second.y) / 2
    val z = (first.z - second.z) / 2
    return Location(second.world, second.x + x, second.y + y, second.z + z)
}

fun HumanEntity.setInventory(type: ItemStack, amount: Int): Boolean {
    if (amount < 0) {
        val sub = -amount
        var aAmount = 0
        fun isSimilar(a: ItemStack): Boolean {
            return a.type == type.type
                    && (SpecialItem.getByItem(a, Bukkit.getPlayer(uniqueId))
                ?.let { it::class == (SpecialItem[type] ?: return true)::class } != false)
                    && (WrappedItem[a, Bukkit.getPlayer(uniqueId)]?.let { it::class == type::class } != false)
        }
        this.inventory.forEach {
            if (it != null && isSimilar(it))
                aAmount += it.amount
        }
        if (aAmount < sub) {
            return false
        }
        var removed = 0
        for (i in 0 until this.inventory.size) {
            val itemStack = this.inventory.getItem(i)
            if (itemStack != null && isSimilar(itemStack)) {
                if (itemStack.amount >= sub - removed) {
                    itemStack.amount -= sub - removed
                    removed += sub - removed
                } else {
                    removed += itemStack.amount
                    this.inventory.setItem(i, null)
                }
                if (removed > sub) {
                    (removed - sub).also { add ->
                        this.inventory.addItem(ItemStack(type.type, add))
                    }
                    break
                }
            }
        }
    } else {
        val notStored = inventory.addItem(
            type.clone()
                .also { it.amount = amount }
        )
        if (notStored.isNotEmpty()) {
            notStored.forEach { (_, u) ->
                world.dropItemNaturally(eyeLocation, u)
            }
        }
    }
    return true
}

val Inventory.containsTickable: Boolean
    get() = this.any { if (it != null) SpecialItem.isSpecial(it) || WrappedItem.isSpecial(it) else false }
val Inventory.tickable: List<Tickable>
    get() {
        val r = ArrayList<Tickable>()
        for (i in 0 until this.size) {
            val it = this.getItem(i) ?: continue
            val showing =
                if (holder is Player) holder as Player else viewers.firstOrNull()?.let { Bukkit.getPlayer(it.uniqueId) }
            SpecialItem.getByItem(it, showing)?.apply {
                r.add(this)
            }
            val slot = if (holder is Player) i else -1
            WrappedItem[it, showing, slot]?.apply {
                r.add(this)
            }
        }
        return r
    }

fun vector(yaw: Double, pitch: Double) = Vector(sin(pitch) * cos(yaw), sin(pitch) * sin(yaw), cos(pitch))

fun Inventory.contentSize() = count { it != null }

val Location.blockLocation: Location
    get() = clone().apply {
        x = blockX.toDouble()
        y = blockY.toDouble()
        z = blockZ.toDouble()
    }
val Location.center: Location
    get() = clone().add(Vector(0.5, 0.0, 0.5))

fun List<Any>.toString(split: String) = buildString {
    forEach {
        append(it.toString() + split)
    }
}.removeSuffix(split)

@Suppress("UNCHECKED_CAST")
infix fun <T> Comparable<T>.smaller(second: T): T = if (this <= second) this as T else second

@Suppress("UNCHECKED_CAST")
infix fun <T> Comparable<T>.bigger(second: T): T = if (this <= second) second else this as T

fun Vector.unitVector(): Vector {
    val k = sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    return Vector(x / k, y / k, z / k)
}

val EntityType.isUndead
    get() = when (this) {
        EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.ZOMBIE,
        EntityType.HUSK, EntityType.PIGLIN, EntityType.ZOMBIE_VILLAGER, EntityType.DROWNED,
        EntityType.ZOMBIE_HORSE, EntityType.ZOMBIFIED_PIGLIN, EntityType.SKELETON_HORSE, EntityType.PHANTOM -> true
        else -> false
    }
val EntityType.isMonster
    get() = when (this) {
        EntityType.ENDERMAN, EntityType.SKELETON, EntityType.CREEPER, EntityType.ZOMBIE,
        EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOGLIN, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER,
        EntityType.HUSK, EntityType.WITHER_SKELETON, EntityType.STRAY, EntityType.BLAZE,
        EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.ELDER_GUARDIAN, EntityType.PHANTOM,
        EntityType.ENDERMITE, EntityType.EVOKER, EntityType.EVOKER_FANGS, EntityType.GHAST,
        EntityType.GIANT, EntityType.GUARDIAN, EntityType.MAGMA_CUBE, EntityType.PILLAGER,
        EntityType.VEX, EntityType.VINDICATOR -> true
        else -> false
    }

@ExperimentalContracts
fun ItemStack?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || this.type == Material.AIR
}