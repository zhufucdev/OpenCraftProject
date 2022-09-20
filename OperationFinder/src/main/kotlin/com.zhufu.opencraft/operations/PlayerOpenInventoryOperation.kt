package com.zhufu.opencraft.operations

import com.google.gson.JsonObject
import com.zhufu.opencraft.OperationChecker
import com.zhufu.opencraft.OperationType
import com.zhufu.opencraft.PlayerOperation
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.Location
import org.bukkit.World
import java.util.*

class PlayerOpenInventoryOperation(player: String, time: Long, inventory: Inventory? = null) :
    PlayerOperation(player, time) {
    var inventory: Location? = inventory?.location
    var type: InventoryType? = inventory?.type
    var name: String? = inventory?.holder?.javaClass?.simpleName
    override val data: JsonObject
        get() = JsonObject()
            .also {
                if (inventory == null)
                    return@also
                it.addProperty("x", inventory?.x)
                it.addProperty("y", inventory?.y)
                it.addProperty("z", inventory?.z)
                it.addProperty("world", inventory?.world?.name)
                it.addProperty("type", type?.name)
                it.addProperty("name", name)
            }

    override val operationType: OperationType
        get() = OperationType.OPEN_INVENTORY

    override val location: Location?
        get() = this.inventory

    override fun toLocalMessage(): String =
        "$player 于${format.format(Date(time))} 打开了位于 ${inventory?.world?.name}(${inventory?.x},${inventory?.y},${inventory?.z}) 的 名为${name}的 ${type?.name} 物品栏"

    override fun deserialize(data: JsonObject) {
        var x = 0.0
        if (data.has("x"))
            x = data["x"].asDouble
        var y = 0.0
        if (data.has("y"))
            y = data["y"].asDouble
        var z = 0.0
        if (data.has("z"))
            z = data["z"].asDouble
        var world: World? = null
        if (data.has("world"))
            world = Bukkit.getWorld(data["world"].asString)
        if (data.has("type"))
            this.type = InventoryType.valueOf(data["type"].asString)
        if (data.has("name"))
            this.name = data["name"].asString
        if (world != null)
            this.inventory = Location(world, x, y, z)
    }
}