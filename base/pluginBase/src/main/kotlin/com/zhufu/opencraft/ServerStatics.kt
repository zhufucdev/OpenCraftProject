package com.zhufu.opencraft

import com.mongodb.client.model.Filters
import com.zhufu.opencraft.data.Database
import org.bson.Document

object ServerStatics {
    private val collection = Database.statics(Base.serverID)
    private val doc = collection.find(Filters.eq(Base.serverID)).first()
        ?: Document("_id", Base.serverID).also { d -> collection.insertOne(d) }

    fun update() {
        collection.replaceOne(Filters.eq(Base.serverID), doc)
    }

    val playerCount: Int
        get() = Database.tag.countDocuments().toInt()
    var onlineTime: Long
        get() = doc.getLong("online") ?: 0
        set(value) {
            doc["online"] = value
            update()
        }
}