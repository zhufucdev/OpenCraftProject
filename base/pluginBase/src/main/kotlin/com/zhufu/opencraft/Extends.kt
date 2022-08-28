package com.zhufu.opencraft

import com.zhufu.opencraft.api.ChatInfo
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.data.ServerPlayer
import com.zhufu.opencraft.special_item.Coin
import com.zhufu.opencraft.special_item.SpecialItem
import com.zhufu.opencraft.special_item.StatefulSpecialItem
import com.zhufu.opencraft.special_item.StatelessSpecialItem
import com.zhufu.opencraft.util.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.Vector
import java.io.File
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.security.MessageDigest
import kotlin.math.cos
import kotlin.math.sin

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
    this.sendMessage(msg.toSuccessMessage())
}

fun CommandSender.info(msg: String) {
    this.sendMessage(msg.toInfoMessage())
}

fun CommandSender.error(msg: String) {
    this.sendMessage(msg.toErrorMessage())
}

fun CommandSender.tip(msg: String) {
    this.sendMessage(msg.toTipMessage())
}

fun CommandSender.warn(msg: String) {
    this.sendMessage(msg.toWarnMessage())
}

@Deprecated("Duplicated", ReplaceWith("Player.sendActionBar"))
fun Player.sendActionText(msg: String) {
    sendActionBar(msg.toComponent())
}

@Suppress("UNCHECKED_CAST")
fun <T : ItemMeta> ItemStack.updateItemMeta(block: T.() -> Unit): ItemStack {
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
                    && ((type is StatefulSpecialItem && StatefulSpecialItem[a]
                ?.let { it::class == type::class } == true)
                    || (type is StatelessSpecialItem && StatelessSpecialItem[a]
                ?.let { it::class == type::class } == true)
                    || type !is StatelessSpecialItem)
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
        notStored.forEach { (_, u) ->
            world.dropItemNaturally(eyeLocation, u)
        }
    }
    return true
}

fun HumanEntity.addCash(amount: Int) = setInventory(Coin(getter()), amount)

val Inventory.containsSpecialItem: Boolean
    get() = this.any { if (it != null) SpecialItem.isSpecial(it) else false }
val Inventory.specialItems: List<SpecialItem>
    get() =
        buildList {
            for (i in 0 until this@specialItems.size) {
                val it = this@specialItems.getItem(i) ?: continue
                SpecialItem[it]?.apply {
                    inventoryPosition = i
                    add(this)
                }
            }
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

val ItemStack.isEmpty get() = type == Material.AIR || amount <= 0