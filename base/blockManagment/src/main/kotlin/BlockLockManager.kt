package com.zhufu.opencraft

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object BlockLockManager {
    val selected = HashMap<Player, BaseInfo>()

    private val mList = ArrayList<BaseInfo>()
    val size = mList.size
    fun forEach(l: (BaseInfo) -> Unit) = mList.forEach(l)
    fun forEachBlock(l: (BlockInfo) -> Unit) {
        mList.forEach {
            if (it is BlockInfo) {
                l(it)
            } else {
                (it as GroupBlockInfo).children.forEach(l)
            }
        }
    }

    fun forEverything(l: (BaseInfo) -> Unit) {
        mList.forEach {
            if (it is BlockInfo)
                l(it)
            else if (it is GroupBlockInfo) {
                l(it)
                it.children.forEach(l)
            }
        }
    }

    abstract class BaseInfo(override var name: String) : Nameable {
        abstract val fullPath: String

        var accessMap = HashMap<Date, UUID>()

        var owner: String = "unknown"
        var accessible = ArrayList<UUID>()

        abstract fun toJson(): JsonObject

        open fun canAccess(player: Player): Boolean {
            return ownedBy(player) || this.accessible.contains(player.uniqueId)
        }

        open fun canAccess(uuid: UUID): Boolean {
            val player = Bukkit.getPlayer(uuid) ?: throw IllegalArgumentException("UUID doesn't belong to any player!")
            return ownedBy(player) || this.accessible.contains(uuid)
        }
        fun ownedBy(player: Player) = (owner == "op" && player.isOp) || owner == player.uniqueId.toString()

        abstract fun contains(from: Location, to: Location): Boolean

        companion object {
            fun fromJson(obj: JsonObject): BaseInfo? {
                return if (obj.has("blocks"))
                    GroupBlockInfo.fromJson(obj)
                else BlockInfo.fromJson(obj)
            }
        }
    }

    class BlockInfo(val location: Location, name: String) : BaseInfo(name) {
        override val fullPath: String
            get() = if (parent != null) "${parent!!.name}/$name" else name
        var parent: GroupBlockInfo? = null

        override fun toJson(): JsonObject {
            return JsonObject().apply {
                val locationObj = JsonObject()
                locationObj.apply {
                    addProperty("x",location.blockX)
                    addProperty("y",location.blockY)
                    addProperty("z",location.blockZ)
                    addProperty("world",location.world.name)
                }
                add("location",locationObj)
                addProperty("name", name)
                addProperty("owner", owner)
                add("accessible", JsonArray().also { accessible.forEach { acc -> it.add(acc.toString()) } })
                val accesses = JsonObject()
                accessMap.forEach { (t, u) ->
                    accesses.addProperty(t.time.toString(), u.toString())
                }
                add("accessMap", accesses)
            }
        }

        companion object {
            fun fromJson(obj: JsonObject): BlockInfo? {
                var location: Location? = null
                var name = ""
                var owner = ""
                val accessible = ArrayList<UUID>()
                val accessMap = HashMap<Date, UUID>()
                if (obj.has("location")){
                    val locationObj = obj["location"].asJsonObject
                    location = Location(
                        Bukkit.getWorld(locationObj["world"].asString),
                        locationObj["x"].asInt.toDouble(),
                        locationObj["y"].asInt.toDouble(),
                        locationObj["z"].asInt.toDouble()
                    )
                }
                if (obj.has("name"))
                    name = obj["name"].asString
                if (obj.has("owner"))
                    owner = obj["owner"].asString
                if (obj.has("accessible"))
                    obj["accessible"].asJsonArray.forEach { accessible.add(UUID.fromString(it.asString)) }
                if (obj.has("accessMap")) {
                    val accesses = obj["accessMap"].asJsonObject
                    accesses.entrySet().forEach {
                        val time = it.key.toLongOrNull()
                        if (time != null)
                            accessMap[Date(time)] = UUID.fromString(it.value.asString)
                    }
                }

                if (location == null || name.isEmpty() || owner.isEmpty())
                    return null

                val r = BlockInfo(location, name)
                r.accessible = accessible
                r.owner = owner
                r.accessMap.putAll(accessMap)
                return r
            }
        }

        override fun contains(from: Location, to: Location): Boolean = from == location || to == location

        override fun equals(other: Any?): Boolean {
            return (other is BlockInfo) && other.location == this.location && other.name == this.name && other.owner == this.owner
        }

        override fun hashCode(): Int {
            var result = location.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + owner.hashCode()
            return result
        }
    }

    class GroupBlockInfo(name: String) : BaseInfo(name) {
        override val fullPath: String
            get() = name
        val children = ArrayList<BlockInfo>()

        override fun contains(from: Location, to: Location): Boolean = children.any { it.contains(from, to) }

        fun add(info: BlockInfo) {
            children.add(info)
            info.parent = this
            info.accessMap.forEach { (t, u) ->
                accessMap[t] = u
            }
            this.accessible.forEach {
                if (!info.accessible.contains(it))
                    info.accessible.add(it)
            }
        }

        fun remove(info: BlockInfo) {
            children.remove(info)
            info.parent = null
        }

        fun contains(info: BlockInfo) = children.contains(info)
        fun contains(name: String) = children.any { it.name == name }

        override fun toJson(): JsonObject {
            val r = JsonObject()
            r.addProperty("name", name)
            r.addProperty("owner", owner)
            r.add("accessible", JsonArray().also { accessible.forEach { info -> it.add(info.toString()) } })
            r.add("blocks", JsonArray().also { children.forEach { block -> it.add(block.toJson()) } })
            r.add(
                "accessMap",
                JsonObject().also { accessMap.forEach { (t, u) -> it.addProperty(t.time.toString(), u.toString()) } })
            return r
        }

        companion object {
            fun fromJson(obj: JsonObject): GroupBlockInfo? {
                var owner = "op"
                var name = ""
                val accessible = ArrayList<UUID>()
                val accesses = HashMap<Date, UUID>()
                val blocks = ArrayList<BlockInfo>()
                if (obj.has("name"))
                    name = obj["name"].asString
                if (obj.has("owner"))
                    owner = obj["owner"].asString
                if (obj.has("accessible")) {
                    obj["accessible"].asJsonArray.forEach {
                        accessible.add(UUID.fromString(it.asString))
                    }
                }
                if (obj.has("accessMap")) {
                    obj["accessMap"].asJsonObject.entrySet().forEach {
                        accesses[Date(it.key.toLong())] = UUID.fromString(it.value.asString)
                    }
                }
                if (obj.has("blocks")) {
                    obj["blocks"].asJsonArray.forEach {
                        blocks.add(
                            BlockInfo.fromJson(it.asJsonObject) ?: return@forEach
                        )
                    }
                }
                if (name.isEmpty())
                    return null
                return GroupBlockInfo(name).also {
                    it.owner = owner
                    it.accessible = accessible
                    it.accessMap = accesses
                    blocks.forEach { block ->
                        block.parent = it
                        it.children.add(block)
                    }
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            return other is GroupBlockInfo
                    && other.name == this.name
                    && other.owner == this.owner
                    && other.accessible == this.accessible
                    && other.children == this.children
        }

        override fun hashCode(): Int {
            return children.hashCode()
        }
    }

    fun loadFromFile(dataFolder: File) {
        dataFolder.listFiles()?.forEach {
            val parse = JsonParser().parse(it.readText()).asJsonObject
            val r = BaseInfo.fromJson(parse)
            if (r == null) {
                println("[BlockLockManager] Could not loading ${it.name}")
                return@forEach
            }
            add(r)
        }
    }

    fun saveToFile(dataFolder: File) {
        if (!dataFolder.exists())
            dataFolder.mkdirs()
        mList.forEach {
            val file = File(dataFolder, "${it.name}-${it.owner}.json")
            if (!file.exists())
                file.createNewFile()
            file.writeText(it.toJson().toString())
        }
        dataFolder.listFiles()?.forEach { file ->
            if (!mList.any { it.name == file.nameWithoutExtension }) {
                file.delete()
            }
        }
    }

    fun add(element: BaseInfo): Boolean {
        val index = mList.indexOf(element)
        return if (index != -1) {
            mList[index] = element
            true
        } else {
            mList.add(element)
        }
    }

    operator fun get(l: Location): BlockInfo? {
        return mList.firstOrNull {
            if (it is BlockInfo) {
                it.location == l
            } else {
                val index = (it as GroupBlockInfo).children.firstOrNull { info -> info.location == l }
                if (index != null) { return index } else false
            }
        } as BlockInfo?
    }

    fun firstOrNull(l: (BaseInfo) -> Boolean) = mList.firstOrNull(l)
    fun removeIf(l: (BaseInfo) -> Boolean) =
        mList.removeIf {
            val r = l(it)
            if (r && it is BlockInfo) {
                it.parent?.remove(it)
            }
            r
        }

    fun remove(info: BaseInfo) = removeIf { it == info }
    fun remove(name: String) = removeIf { it.name == name }

    fun contains(name: String) = mList.any { it.name == name || (it is GroupBlockInfo && it.contains(name)) }
    fun containsBlock(name: String) =
        mList.any { (it is BlockInfo && it.name == name) || (it is GroupBlockInfo && it.contains(name)) }
    fun contains(location: Location) = get(location) != null

    operator fun get(i: Int) = mList[i]
    operator fun get(name: String): BaseInfo? {
        return if (name.contains('/')) {
            val split = name.split('/')
            val group = mList.firstOrNull { it is GroupBlockInfo && it.name == split.first() } ?: return null
            (group as GroupBlockInfo).children.firstOrNull { it.name == split[1] }

        } else {
            mList.firstOrNull { it.name == name }
        }
    }
}