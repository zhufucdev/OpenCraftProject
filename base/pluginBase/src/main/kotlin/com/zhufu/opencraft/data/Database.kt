package com.zhufu.opencraft.data

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.*
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.zhufu.opencraft.Base
import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.conversions.Bson
import org.bukkit.Bukkit
import java.util.*


object Database {
    lateinit var client: MongoClient
    private lateinit var db: MongoDatabase
    val tag: MongoCollection<Document> by lazy { db.getCollection("tags") }
    val friendship: MongoCollection<Document> by lazy { db.getCollection("friendship") }
    val trades: MongoCollection<Document> by lazy { db.getCollection("trades") }

    fun init(uri: String) {
        client = MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(uri))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
        )
        Bukkit.getLogger().info("Database connected to $uri")
        db = client.getDatabase("opencraft")
    }

    fun close() {
        client.close()
    }

    fun find(type: String, uuid: UUID): MongoCollection<Document> = db.getCollection("$uuid-$type")
    fun find(collection: MongoCollection<Document>, uuid: UUID, create: Boolean = true) =
        collection.find(eq("_id", uuid)).first()
            ?: if (create) {
                Document("_id", uuid).also { collection.insertOne(it) }
            } else null

    fun drop(player: UUID) {
        tag.deleteOne(eq("_id", player))
        statics(player).drop()
        checkpoint(player).drop()
    }

    fun tag(player: UUID, create: Boolean = true) = find(tag, player, create)
    fun tag(name: String): Document? = tag.find(eq("name", name)).first()

    fun tag(player: UUID, update: Document) {
        tag.replaceOne(eq(player), update)
    }

    fun statics(player: UUID) = find("statics", player)

    fun checkpoint(player: UUID) = find("checkpoint", player)

    fun messagePool(player: UUID) = find("messagePool", player)

    fun friendship(id: UUID) = find(friendship, id)
    fun friendship(owner: ServerPlayer): FindIterable<Document> {
        fun byOwnerID(role: String) = eq(role, owner.uuid)
        return friendship.find(Filters.or(byOwnerID("a"), byOwnerID("b")))
    }
    fun friendship(a: UUID, b: UUID): Document? {
        return friendship.find(friendshipInvolving(a, b)).first()
    }
    fun friendshipInvolving(a: UUID, b: UUID): Bson = Filters.or(
        Filters.and(
            eq("a", a),
            eq("b", b)
        ),
        Filters.and(
            eq("a", b),
            eq("b", a)
        )
    )

    fun friendship(id: UUID, update: Document) {
        if (friendship(id) == null) {
            friendship.insertOne(update)
        } else {
            friendship.replaceOne(eq(id), update)
        }
    }

    fun inventory(player: UUID) = find("inventory", player)

    fun specialBlock() = find("sb", Base.serverID)
    fun ads() = find("ad", Base.serverID)
    fun trade(id: UUID, update: Document) {
        trades.findOneAndReplace(eq("_id", id), update)
    }
}