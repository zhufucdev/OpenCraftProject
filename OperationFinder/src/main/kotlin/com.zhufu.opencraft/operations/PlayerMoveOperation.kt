package com.zhufu.opencraft.operations

import com.google.gson.JsonObject
import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.OperationType
import com.zhufu.opencraft.PlayerOperation
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

class PlayerMoveOperation(player: String, time: Long, var l: Location? = null) : PlayerOperation(player, time) {
    override val operationType: OperationType
        get() = OperationType.MOVE
    override val data: JsonObject
        get() = JsonObject()
                .also {
                    it.addProperty("x",l?.x)
                    it.addProperty("y",l?.y)
                    it.addProperty("z",l?.z)
                    it.addProperty("world",l?.world?.name)
                }
    override val location: Location?
        get() = l

    override fun deserialize(data: JsonObject) {
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var world = ""
        if (data.has("x"))
            x = data["x"].asDouble
        if (data.has("y"))
            y = data["y"].asDouble
        if (data.has("z"))
            z = data["z"].asDouble
        if (data.has("world"))
            world = data["world"].asString

        this.l = Location(Bukkit.getWorld(world),x,y,z)
    }

    override fun toLocalMessage(): String = "$player 于${format.format(Date(time))} 来到了 ${l?.toPrettyString()}"
}