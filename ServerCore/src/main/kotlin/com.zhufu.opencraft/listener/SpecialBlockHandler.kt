package com.zhufu.opencraft.listener

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.zhufu.opencraft.Base.Extend.toPrettyString
import com.zhufu.opencraft.data.Database
import com.zhufu.opencraft.special_block.Dropable
import com.zhufu.opencraft.special_block.SBCompanion
import com.zhufu.opencraft.special_block.SpecialBlock
import com.zhufu.opencraft.special_item.Placeable
import com.zhufu.opencraft.special_item.SpecialItem
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import kotlin.reflect.full.companionObjectInstance

object SpecialBlockHandler : Listener {
    private lateinit var plugin: JavaPlugin
    private lateinit var collection: MongoCollection<Document>
    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
        Bukkit.getPluginManager().registerEvents(this, plugin)

        collection = Database.specialBlock()
        collection.find().forEach {
            val type = it.get("type", UUID::class.java)
            val location = it["location"]?.let { l ->
                if (l is Location) l
                else {
                    val doc = l as Document
                    Location.deserialize(doc)
                }
            } ?: return@forEach
            val klass = SpecialBlock.predefined.firstOrNull { sb ->
                (sb.kotlin.companionObjectInstance as SBCompanion).SBID == type
            }
            if (klass != null) {
                val sb = (klass.kotlin.companionObjectInstance as SBCompanion).from(location)
                register(sb)
            } else {
                plugin.logger.warning("Failed to deserialize special block $type at ${location.toPrettyString()}, " +
                        "is it removed?")
            }
        }
    }

    private val cache = hashMapOf<Location, SpecialBlock>()
    private fun register(block: SpecialBlock, new: Boolean = false) {
        cache[block.location] = block
        Bukkit.getPluginManager().registerEvents(block.eventListener, plugin)
        if (!new) return
        collection.insertOne(
            Document("type", block.type)
                .append("location", Document(block.location.serialize()))
        )
    }

    private fun remove(location: Location): SpecialBlock? {
        val sb = cache.remove(location)
        if (sb != null)
            collection.deleteOne(Filters.eq("location", Document(location.serialize())))
        return sb
    }

    @EventHandler
    fun onCreatedFromSpecialItem(event: BlockPlaceEvent) {
        if (event.isCancelled) {
            return
        }
        val si = SpecialItem[event.itemInHand] ?: return
        if (si is Placeable) {
            val sb = (si.block.companionObjectInstance as SBCompanion).from(event.block.location)
            register(sb, true)
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val removed = remove(event.block.location)
        if (removed is Dropable) {
            event.isDropItems = false
            event.block.world.dropItemNaturally(event.block.location, removed.itemToDrop)
        }
    }
}