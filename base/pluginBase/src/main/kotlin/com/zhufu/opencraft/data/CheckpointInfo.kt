package com.zhufu.opencraft.data

import com.zhufu.opencraft.api.Nameable
import org.bukkit.Location

class CheckpointInfo(val location: Location, override var name: String) : Nameable, Cloneable {
    override fun equals(other: Any?): Boolean {
        return other is CheckpointInfo
                && other.location == this.location
                && other.name == this.name
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    public override fun clone(): CheckpointInfo = CheckpointInfo(location, name)
}