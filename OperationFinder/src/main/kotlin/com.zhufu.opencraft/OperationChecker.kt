package com.zhufu.opencraft

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.zhufu.opencraft.operations.PlayerBlockOperation
import com.zhufu.opencraft.operations.PlayerMoveOperation
import com.zhufu.opencraft.operations.PlayerOpenInventoryOperation
import org.bukkit.Location
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object OperationChecker {
    enum class OperationType{
        MOVE,BLOCK,OPEN_INVENTORY
    }
    abstract class PlayerOperation(val player: String, val time: Long){

        abstract val operationType: OperationType
        abstract val data: JsonObject
        abstract val location: Location?
        abstract fun deserialize(data: JsonObject)

        abstract fun toLocalMessage(): String

        override fun toString(): String {
            val sw = StringWriter()
            val writer = JsonWriter(sw)
            writer.beginObject()
                    .name("time").value(time)
                    .name("player").value(player)
                    .name("type").value(operationType.name)
                    .name("data").jsonValue(data.toString())
                .endObject()
            return sw.toString()
        }
        companion object {
            fun fromJson(reader: JsonReader): PlayerOperation{
                reader.beginObject()
                var time = 0L
                var player = ""
                var type: OperationType? = null
                var data = JsonObject()
                while (reader.hasNext()){
                    when (reader.nextName()){
                        "time" -> time = reader.nextLong()
                        "player" -> player = reader.nextString()
                        "type" -> type = OperationType.valueOf(reader.nextString())
                        "data" -> data = JsonParser().parse(reader).asJsonObject
                    }
                }
                reader.endObject()
                if (type == null){
                    throw IllegalArgumentException("Could not find Operation Type in Json!")
                }
                return when (type){
                    OperationType.MOVE -> PlayerMoveOperation(player,time).also { it.deserialize(data) }
                    OperationType.BLOCK -> PlayerBlockOperation(player,time).also { it.deserialize(data) }
                    OperationType.OPEN_INVENTORY -> PlayerOpenInventoryOperation(player, time).also { it.deserialize(data) }
                }
            }
            val format = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss")
        }
    }

    const val anyPlayer = "\$anyPlayer"
    private val recentFifty = ArrayList<PlayerOperation>()
    private val log: File = File("plugins${File.separatorChar}OperationFinder${File.separatorChar}log.txt").also {
        if (!it.parentFile.exists()) it.parentFile.mkdirs()
        if (!it.exists()) it.createNewFile()
    }
    private val reader
        get() = log.reader()
    fun append(operation: PlayerOperation){
        /**
         * An operaiton line looks like this:
         * {date:3141592653589793,player:"zhufucomcom",type:"OPEN_INVENTORY",data:{location:[0,0,0],inventoryType:"CHEST"}}
         */
        recentFifty.add(operation)
        if (recentFifty.size > 50){
            val first = recentFifty.first()
            log.appendText(first.toString() + System.lineSeparator())
            recentFifty.removeAt(0)
        }
    }
    private fun readLine(line: String): PlayerOperation? = try{ PlayerOperation.fromJson(JsonReader(StringReader(line))) } catch (e: Exception) { null }
    operator fun get(player: String): Array<PlayerOperation>{
        val result = ArrayList<PlayerOperation>()

        val localReader = reader
        var t = localReader.read()
        var line = StringBuilder()
        while (t != -1){
            line.append(t.toChar())
            if (t.toChar() == '\n'){
                val element = OperationChecker.readLine(line.toString())
                if (element != null) {
                    if (player == anyPlayer || element.player == player)
                        result.add(element)
                }
                line = StringBuilder()
            }
            t = localReader.read()
        }

        if (player != anyPlayer)
            recentFifty.forEach { if (it.player == player) result.add(it) }
        else result.addAll(recentFifty)

        return result.toTypedArray()
    }

    operator fun get(from: Date,to: Date): Array<PlayerOperation>{
        val r = ArrayList<PlayerOperation>()
        this.forEach {
            if ((from.time < to.time && it.time in from.time .. to.time) || (from.time > to.time && it.time in to.time .. from.time))
                r.add(it)
        }
        return r.toTypedArray()
    }

    fun forEach(l: (PlayerOperation) -> Unit) {
        val localReader = reader
        var t = localReader.read()
        var line = StringBuilder()
        while (t != -1){
            line.append(t.toChar())
            if (t.toChar() == '\n'){
                val element = OperationChecker.readLine(line.toString())
                if (element != null)
                    l.invoke(element)
                line = StringBuilder()
            }
            t = localReader.read()
        }
        localReader.close()

        recentFifty.forEach(l)
    }

    fun first(): PlayerOperation?{
        val localReader = reader
        var t = localReader.read()
        var line = StringBuilder()
        while (t != -1){
            line.append(t.toChar())
            if (t.toChar() == '\n'){
                val e = OperationChecker.readLine(line.toString())
                if (e != null)
                    return e
                line = StringBuilder()
            }
            t = localReader.read()
        }
        return null
    }

    fun last(): PlayerOperation = recentFifty.last()

    fun players(): Array<String>{
        val r = ArrayList<String>()
        this.forEach {
            if (!r.contains(it.player))
                r.add(it.player)
        }
        return r.toTypedArray()
    }

    fun save(){
        recentFifty.forEach { log.appendText(it.toString() + System.lineSeparator()) }
    }
}