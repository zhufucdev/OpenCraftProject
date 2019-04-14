package com.zhufu.opencraft

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object BlockLockManager {
    val selected = HashMap<Player,BaseInfo>()

    private val mList = ArrayList<BaseInfo>()
    val size = mList.size
    fun forEach(l: (BaseInfo) -> Unit) = mList.forEach(l)
    fun forEachBlock(l: (BlockInfo) -> Unit){
        mList.forEach {
            if (it is BlockInfo){
                l(it)
            } else {
                (it as GroupBlockInfo).children.forEach(l)
            }
        }
    }
    fun forEverything(l: (BaseInfo) -> Unit){
        mList.forEach {
            if (it is BlockInfo)
                l(it)
            else if (it is GroupBlockInfo){
                l(it)
                it.children.forEach(l)
            }
        }
    }

    fun Location.toXZ(): BlockLockManager.XZ = BlockLockManager.XZ(this.blockX,this.blockZ)

    abstract class BaseInfo(var name: String){
        abstract val fullPath: String

        var accessMap = HashMap<Date,UUID>()

        var owner: String = "unknown"
        var accessible = ArrayList<UUID>()

        abstract fun toJson(): JsonObject

        open fun canAccess(player: Player): Boolean {
            return (this.owner == "op" && player.isOp) || this.owner == player.uniqueId.toString() || this.accessible.contains(player.uniqueId)
        }
        open fun canAccess(uuid: UUID): Boolean {
            val player = Bukkit.getPlayer(uuid) ?: throw IllegalArgumentException("UUID not found!")
            return (this.owner == "op" && player.isOp) || this.owner == player.uniqueId.toString() || this.accessible.contains(uuid)
        }

        abstract fun contains(from: XZ, to: XZ): Boolean

        companion object {
            fun fromJson(obj: JsonObject): BaseInfo?{
                return if (obj.has("blocks"))
                    GroupBlockInfo.fromJson(obj)
                else BlockInfo.fromJson(obj)
            }
        }
    }

    class BlockInfo(val from: XZ, val to: XZ, val world: String, name: String): BaseInfo(name) {
        override val fullPath: String
            get() = if (parent != null) "${parent!!.name}/$name" else name
        val center: XZ
            get() = XZ((from.x - to.x) / 2 + to.x, (from.z - to.z) / 2 + to.z)
        var parent: GroupBlockInfo? = null

        override fun canAccess(player: Player): Boolean {
            return super.canAccess(player) || player.world.name != world
        }

        override fun toJson(): JsonObject{
            val r = JsonObject()
            r.add("from",from.toJson())
            r.add("to",to.toJson())
            r.addProperty("name",name)
            r.addProperty("world",world)
            r.addProperty("owner",owner)
            r.add("accessible",JsonArray().also { accessible.forEach { acc -> it.add(acc.toString()) } })
            val accesses = JsonObject()
            accessMap.forEach { t, u ->
                accesses.addProperty(t.time.toString(),u.toString())
            }
            r.add("accessMap",accesses)
            return r
        }

        companion object {
            fun fromJson(obj: JsonObject): BlockInfo?{
                var from: XZ? = null
                var to: XZ? = null
                var name = ""
                var world = ""
                var owner = ""
                val accessible = ArrayList<UUID>()
                val accessMap = HashMap<Date,UUID>()
                if (obj.has("from"))
                    from = XZ.fromJson(obj["from"].asJsonObject)
                if (obj.has("to"))
                    to = XZ.fromJson(obj["to"].asJsonObject)
                if (obj.has("name"))
                    name = obj["name"].asString
                if (obj.has("world"))
                    world = obj["world"].asString
                if (obj.has("owner"))
                    owner = obj["owner"].asString
                if (obj.has("accessible"))
                    obj["accessible"].asJsonArray.forEach { accessible.add(UUID.fromString(it.asString)) }
                if (obj.has("accessMap")){
                    val accesses = obj["accessMap"].asJsonObject
                    accesses.entrySet().forEach {
                        val time = it.key.toLongOrNull()
                        if (time != null)
                            accessMap[Date(time)] = UUID.fromString(it.value.asString)
                    }
                }

                if (world.isEmpty())
                    world = Base.surviveWorld.name

                if (from == null || to == null || name.isEmpty() || owner.isEmpty())
                    return null

                val r = BlockInfo(from, to, world, name)
                r.accessible = accessible
                r.owner = owner
                r.accessMap.putAll(accessMap)
                return r
            }
        }

        private fun validateZ(from: XZ, to: XZ): Boolean{
            if (this.from.z < this.to.z) if (from.z < to.z) return (from.z .. to.z).any { it in this.from.z .. this.to.z }
            else                return (to.z .. from.z).any { it in this.from.z .. this.to.z }
            else                        if (from.z < to.z)  return (from.z .. to.z).any { it in this.to.z .. this.from.z }
            else                return (to.z .. from.z).any { it in this.to.z .. this.from.z }
        }
        override fun contains(from: XZ, to: XZ): Boolean {
            if (this.from.x < this.to.x) if (from.x < to.x) return (from.x .. to.x).any { it in this.from.x .. this.to.x } && validateZ(from,to)
            else               return (to.x .. from.x).any { it in this.from.x .. this.to.x } && validateZ(from,to)
            else                         if (from.x < to.x) return (from.x .. to.x).any { it in this.to.x .. this.from.x } && validateZ(from,to)
            else               return (to.x .. from.x).any { it in this.to.x .. this.from.x } && validateZ(from,to)
        }
        override fun equals(other: Any?): Boolean {
            return (other is BlockInfo) && other.from == this.from && other.to == this.to && other.name == this.name && other.world == this.world
        }

        override fun hashCode(): Int {
            var result = from.hashCode()
            result = 31 * result + to.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + world.hashCode()
            return result
        }
    }

    class GroupBlockInfo(name: String): BaseInfo(name){
        override val fullPath: String
            get() = name
        val children = ArrayList<BlockInfo>()

        override fun contains(from: XZ, to: XZ): Boolean = children.any { it.contains(from,to) }

        fun add(info: BlockInfo){
            children.add(info)
            info.parent = this
            info.accessMap.forEach { t, u ->
                accessMap[t] = u
            }
            this.accessible.forEach {
                if (!info.accessible.contains(it))
                    info.accessible.add(it)
            }
        }
        fun remove(info: BlockInfo){
            children.remove(info)
            info.parent = null
        }
        fun contains(info: BlockInfo) = children.contains(info)
        fun contains(name: String) = children.any { it.name == name }

        override fun canAccess(player: Player): Boolean {
            return super.canAccess(player) || !children.any { it.world == player.world.name }
        }

        override fun toJson(): JsonObject {
            val r = JsonObject()
            r.addProperty("name",name)
            r.addProperty("owner",owner)
            r.add("accessible",JsonArray().also { accessible.forEach { info -> it.add(info.toString()) } })
            r.add("blocks",JsonArray().also { children.forEach { block -> it.add(block.toJson()) } })
            r.add("accessMap",JsonObject().also { accessMap.forEach { t, u -> it.addProperty(t.time.toString(),u.toString()) } })
            return r
        }

        companion object {
            fun fromJson(obj: JsonObject): GroupBlockInfo?{
                var owner = "op"
                var name = ""
                val accessible = ArrayList<UUID>()
                val accesses = HashMap<Date,UUID>()
                val blocks = ArrayList<BlockInfo>()
                if (obj.has("name"))
                    name = obj["name"].asString
                if (obj.has("owner"))
                    owner = obj["owner"].asString
                if (obj.has("accessible")){
                    obj["accessible"].asJsonArray.forEach {
                        accessible.add(UUID.fromString(it.asString))
                    }
                }
                if (obj.has("accessMap")){
                    obj["accessMap"].asJsonObject.entrySet().forEach {
                        accesses[Date(it.key.toLong())] = UUID.fromString(it.value.asString)
                    }
                }
                if (obj.has("blocks")){
                    obj["blocks"].asJsonArray.forEach {
                        blocks.add(
                                BlockInfo.fromJson(it.asJsonObject) ?:return@forEach
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

    class XZ(var x: Int, var z: Int): ConfigurationSerializable,Cloneable {
        companion object {
            fun deserialize(args: Map<String,Any>) = XZ(args["x"] as Int, args["z"] as Int)

            fun fromJson(jsonObject: JsonObject): XZ?{
                var x: Int? = null
                var z: Int? = null
                if (jsonObject.has("x"))
                    x = jsonObject["x"].asInt
                if (jsonObject.has("z"))
                    z = jsonObject["z"].asInt

                if (x == null || z == null)
                    return null
                return XZ(x, z)
            }
        }

        override fun serialize(): MutableMap<String, Any> {
            val r = HashMap<String,Int>()
            r["x"] = x
            r["z"] = z
            return r.toMutableMap()
        }

        fun toLocation(world: World,y: Double) = Location(world,x.toDouble(),y,z.toDouble())
        fun toJson(): JsonObject{
            val r = JsonObject()
            r.addProperty("x",x)
            r.addProperty("z",z)
            return r
        }

        override fun toString(): String = "x: $x,z: $z"

        override fun equals(other: Any?): Boolean {
            return other is XZ && other.x == this.x && other.z == this.z
        }

        override fun hashCode(): Int {
            var result = x
            result = 31 * result + z
            return result
        }

        public override fun clone(): XZ {
            return super.clone() as XZ
        }
    }

    fun loadFromFile(dataFolder: File) {
        dataFolder.listFiles()?.forEach {
            val parse = JsonParser().parse(it.readText()).asJsonObject
            val r = BaseInfo.fromJson(parse)
            if (r == null){
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
            val file = File(dataFolder, "${it.name}.json")
            if (!file.exists())
                file.createNewFile()
            file.writeText(it.toJson().toString())
        }
        dataFolder.listFiles().forEach { file ->
            if (!mList.any{ it.name == file.nameWithoutExtension }){
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
        fun handleAsBlockInfo(it: BlockInfo): Boolean{
            fun validateZ(info: BlockInfo): Boolean {
                return if (info.from.z < info.to.z) {
                    l.blockZ in info.from.z..info.to.z
                } else {
                    l.blockZ in info.to.z..info.from.z
                }
            }
            return if (it.from.x < it.to.x) {
                (l.blockX in it.from.x..it.to.x) && validateZ(it) && it.world == l.world!!.name
            } else {
                (l.blockX in it.to.x..it.from.x) && validateZ(it) && it.world == l.world!!.name
            }
        }

        return mList.firstOrNull {
            if (it is BlockInfo) {
                handleAsBlockInfo(it)
            } else {
                val index = (it as GroupBlockInfo).children.firstOrNull { info ->
                    handleAsBlockInfo(info)
                }
                if (index != null){
                    return index
                } else false
            }
        } as BlockInfo?
    }

    fun firstOrNull(l: (BaseInfo) -> Boolean) = mList.firstOrNull(l)
    fun removeIf(l: (BaseInfo) -> Boolean) =
            mList.removeIf{
                val r = l(it)
                if (r && it is BlockInfo){
                    it.parent?.remove(it)
                }
                r
            }
    fun remove(info: BaseInfo) = removeIf { it == info }
    fun remove(name: String) = removeIf { it.name == name }

    fun contains(name: String) = mList.any { it.name == name || (it is GroupBlockInfo && it.contains(name)) }
    fun containsBlock(name: String) = mList.any { (it is BlockInfo && it.name == name) || (it is GroupBlockInfo && it.contains(name)) }

    operator fun get(i: Int) = mList[i]
    operator fun get(name: String): BaseInfo?{
        return if (name.contains('/')){
            val split = name.split('/')
            val group = mList.firstOrNull { it is GroupBlockInfo && it.name == split.first() }?:return null
            (group as GroupBlockInfo).children.firstOrNull { it.name == split[1] }

        } else {
            mList.firstOrNull { it.name == name }
        }
    }
}