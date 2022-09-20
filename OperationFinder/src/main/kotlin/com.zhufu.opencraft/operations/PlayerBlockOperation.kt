package com.zhufu.opencraft.operations

import com.google.gson.JsonObject
import com.zhufu.opencraft.OperationChecker
import com.zhufu.opencraft.OperationType
import com.zhufu.opencraft.PlayerOperation
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import java.text.SimpleDateFormat
import java.util.*

enum class BlockOperationType {
    PLACE, BREAK
}

class PlayerBlockOperation(
    player: String,
    time: Long,
    var block: Material? = null,
    var l: Location? = null,
    var type: BlockOperationType? = null
) : PlayerOperation(player, time) {

    override val operationType: OperationType
        get() = OperationType.BLOCK
    override val data: JsonObject
        get() = JsonObject()
            .also { jsonObject ->
                jsonObject.addProperty("type", type?.name)
                jsonObject.add(
                    "block",
                    JsonObject().also { jsonObject1 ->
                        jsonObject1.addProperty("type", block?.name)
                        jsonObject1.add(
                            "location",
                            JsonObject().also {
                                it.addProperty("x", location?.blockX)
                                it.addProperty("y", location?.blockY)
                                it.addProperty("z", location?.blockZ)
                                it.addProperty("world", location?.world?.name)
                            }
                        )
                    }
                )
            }
    override val location: Location?
        get() = l

    override fun deserialize(data: JsonObject) {
        this.type = BlockOperationType.valueOf(data["type"].asString)
        if (data.has("block")) {
            val block = data.getAsJsonObject("block")
            this.block = Material.valueOf(block["type"].asString)
            val location = block.getAsJsonObject("location")
            val world = Bukkit.getWorld(location["world"].asString)
            val x = location["x"].asInt
            val y = location["y"].asInt
            val z = location["z"].asInt
            this.l = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        }
    }

    override fun toLocalMessage(): String =
        "$player 于${format.format(Date(time))} ${if (type == BlockOperationType.PLACE) "放置" else "破坏"} 了位于 ${location?.world?.name}(${location?.blockX},${location?.blockY},${location?.blockZ}) 的 ${block?.name} 方块"
}