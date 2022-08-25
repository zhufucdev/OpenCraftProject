package com.zhufu.opencraft.data

import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import java.nio.file.Paths
import java.util.*

open class OfflineInfo(uuid: UUID, createNew: Boolean = false) : ServerPlayer(createNew, uuid) {
    companion object {
        fun forEach(l: (OfflineInfo) -> Unit) =
            Database.tag.find().forEach {
                val id = it.get("_id", UUID::class.java)
                if (Bukkit.getPlayer(id) != null) {
                    l(OfflineInfo(id, false))
                }
            }

        fun listPlayers(): List<OfflineInfo> {
            val r = ArrayList<OfflineInfo>()
            forEach { r.add(it) }
            return r
        }

        fun listPlayers(regex: (OfflineInfo) -> Boolean): List<OfflineInfo> {
            val r = ArrayList<OfflineInfo>()
            forEach { if (regex(it)) r.add(it) }
            return r
        }

        fun findByUUID(uuid: UUID): OfflineInfo? =
            cache.firstOrNull { it.uuid == uuid }
                ?: Info.findByPlayer(uuid)
                ?: try {
                    OfflineInfo(uuid).also { cache.add(it) }
                } catch (e: Exception) {
                    null
                }
        fun findByName(name: String): OfflineInfo? {
            return cache.firstOrNull { it.name == name }
                ?: Database.tag(name)
                ?.let { OfflineInfo(it.get("_id", UUID::class.java), false) }
        }

        val cache = ArrayList<OfflineInfo>()

        val count: Long
            get() = Database.tag.countDocuments()
    }
    override val playerDir: File
        get() = Paths.get("plugins", "playerDir", uuid.toString()).toFile()
            .also {
                if (!it.exists()) it.mkdirs()
            }

    var survivalSpawn: Location?
        get() {
            return Location.deserialize(doc["survivalSpawn"] as Document? ?: return null)
        }
        set(value) {
            if (value == null) {
                doc.remove("survivalSpawn")
            } else {
                doc["survivalSpawn"] = Document(value.serialize())
            }
            update()
        }

    var lastDeath: DeathInfo?
        get() {
            return DeathInfo.deserialize(doc.get("lastDeath", Document::class.java) ?: return null)
        }
        set(value) {
            if (value == null) {
                doc.remove("lastDeath")
            } else {
                doc["lastDeath"] = value.toDocument()
            }
            update()
        }

    var isTradeTutorialShown: Boolean
        get() = doc.getBoolean("isTTShown", false)
        set(value) {
            doc["isTTShown"] = value
            update()
        }

    var npcTradeCount: Int
        get() = doc.getInteger("npcTrade", 0)
        set(value) {
            doc["npcTrade"] = npcTradeCount
            update()
        }

    var isInsuranceAdShown: Boolean
        get() = doc.getBoolean("isInsuranceAdShown", false)
        set(value) {
            doc["isInsuranceAdShown"] = value
            update()
        }

    override fun destroy() {
        cache.removeAll { it.uuid == uuid }
        super.destroy()
    }

    override fun delete() {
        super.delete()
        destroy()
    }

    var inventory: DualInventory = DualInventory(this)
}