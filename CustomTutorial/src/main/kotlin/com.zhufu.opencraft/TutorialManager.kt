package com.zhufu.opencraft

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.zhufu.opencraft.Base.Extend.appendToJson
import com.zhufu.opencraft.Base.Extend.fromJsonToLocation
import com.zhufu.opencraft.Base.TutorialUtil.tplock
import com.zhufu.opencraft.Base.TutorialUtil.linearMotion
import com.zhufu.opencraft.TutorialManager.Tutorial.TriggerMethod.*
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.StringReader
import java.util.*
import kotlin.collections.ArrayList

object TutorialManager {
    private val mList = ArrayList<Tutorial>()
    fun add(tutorial: Tutorial) {
        mList.add(tutorial.also { it.id = getNewID() })
        saveToFile(tutorial)
    }
    fun addAsDraft(tutorial: Tutorial) {
        mList.add(tutorial.also { it.id = getNewID() })
        saveToFile(tutorial)
    }
    fun del(id: Int) = mList.removeIf {
        deleteFile(it)
        it.id == id
    }

    operator fun get(id: Int): Tutorial? = mList.firstOrNull { it.id == id }
    operator fun get(player: Player): List<Tutorial> = mList.filter { it.creator == player.uniqueId.toString() }
    operator fun set(id: Int, value: Tutorial) {
        val index = mList.indexOfFirst { it.id == id }
        if (index != -1)
            mList[index] = value
    }

    fun everything() = mList.toList()

    var mPlugin: Plugin? = null
    fun init(plugin: Plugin) {
        mPlugin = plugin
    }

    private val id
        get() = mList.maxBy { it.id }.id

    fun getNewID(): Int {
        for (i in 0..id) {
            if (!mList.any { i == it.id }) {
                return i
            }
        }
        return id + 1
    }

    private fun getName(tutorial: Tutorial) = "${tutorial.id}[${if (tutorial.isDraft) "Draft" else "Release"}]"
    fun saveToFile(element: Tutorial) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val writer = gson.newJsonWriter(CustomTutorial.getFile(getName(element)).writer())
        element.appendToJson(writer)
        writer.flush()
    }

    fun deleteFile(element: Tutorial) = CustomTutorial.getFile(getName(element)).delete()

    fun loadFromFile() {
        CustomTutorial.saveDir.listFiles()?.forEach { file ->
            if (file.isHidden)
                return@forEach
            try {
                CustomTutorial.logger.info("Loading tutorial file ${file.name}")
                val reader = JsonReader(file.reader())
                val p = Tutorial.fromJson(reader) ?: throw IllegalStateException("skipped ${file.nameWithoutExtension}")
                if (mList.any { it.id == p.id }) {
                    p.id = getNewID()
                }
                mList.add(p)
            } catch (e: Exception) {
                CustomTutorial.logger.warning("Error while loading ${file.name}:")
                throw e
            }
        }
    }

    class Tutorial(val creator: String, var name: String) {
        enum class TriggerMethod {
            NONE, BLOCK, TERRITORY, ENTER_WORLD, REGISTER
        }

        var id = -1
        var isDraft = true
        var triggerMethod: TriggerMethod = NONE
        var triggerArgument = JsonObject()
        fun rebuildArguments() {
            triggerArgument = JsonObject()
        }

        private val played = ArrayList<String>()
        fun triggerOrNot(player: Player): Boolean =
            if (played.contains(player.name)) {
                false
            } else {
                when (triggerMethod) {
                    NONE -> false
                    BLOCK -> {
                        if (triggerArgument.has("x") && triggerArgument.has("y") && triggerArgument.has("z")) {
                            val x = triggerArgument["x"].asInt
                            val y = triggerArgument["y"].asInt
                            val z = triggerArgument["z"].asInt
                            val world = Bukkit.getWorld(UUID.fromString(triggerArgument["world"].asString))
                            if (world != null && player.world == world) {
                                world.getBlockAt(x, y, z).location.distance(player.location) < 1.5
                            } else false
                        } else false
                    }
                    TERRITORY -> {
                        if (triggerArgument.has("name")) {
                            BlockLockManager[player.location]?.name ?: false == triggerArgument["name"].asString
                        } else false
                    }
                    ENTER_WORLD -> {
                        if (triggerArgument.has("world")) {
                            val world = Bukkit.getWorld(UUID.fromString(triggerArgument["world"].asString))
                            player.world == world ?: false
                        } else false
                    }
                    else -> false
                }
            }

        private val steps = ArrayList<TutorialStep>()
        val size: Int
            get() = steps.size

        fun addNewStep(): TutorialStep {
            val r = TutorialStep(this)
            steps.add(r)
            return r
        }

        fun removeStep(index: Int) = steps.removeAt(index)
        fun sort(from: Int, to: Int) {
            val t = steps[from]
            steps[from] = steps[to]
            steps[to] = t
        }

        operator fun get(index: Int) = steps[index]
        operator fun set(index: Int, value: TutorialStep) {
            steps[index] = value
        }

        fun indexOf(element: TutorialStep): Int = steps.indexOf(element)
        fun forEachStep(l: (TutorialStep) -> Unit) = steps.forEach(l)

        fun play(to: Entity, includeInventory: Boolean = false): TutorialPlayer? {
            if (!played.contains(to.name)) {
                played.add(to.name)
                saveToFile(this)
            }
            var lastStatus: Info.GameStatus? = null
            val info =
                if (to is HumanEntity) {
                    if (includeInventory) {
                        val info = PlayerManager.findInfoByPlayer(to.uniqueId)
                        val getter = getLangGetter(info)
                        if (info == null) {
                            to.error(getter["player.error.unknown"])
                            return null
                        }
                        lastStatus = info.status
                        info.status = Info.GameStatus.InTutorial
                        info
                    } else {
                        null
                    }
                } else null
            to.info(getLangGetter(info)["tutorial.incoming", name])
            val player = object : TutorialPlayer {
                override var l: (() -> Unit)? = null
                override fun play(entity: Entity) {
                    Bukkit.getScheduler().runTaskAsynchronously(mPlugin!!) { _ ->
                        steps.forEach {
                            it.showTo(to)
                        }
                        super.play(entity)
                    }
                }
            }
            player.setPastPlayingListener {
                Bukkit.getScheduler().runTask(TutorialManager.mPlugin!!) { t ->
                    info?.inventory?.last
                        ?.also {
                            info.status = lastStatus!!
                        }
                        ?.load()
                }
            }

            Bukkit.getScheduler().runTaskLater(TutorialManager.mPlugin!!, { _ ->
                //Start playing
                info?.inventory?.create(DualInventory.RESET)?.load(inventoryOnly = true)
                if (to is HumanEntity) {
                    to.gameMode = GameMode.SPECTATOR
                }
                player.play(to)
            }, 40)
            return player
        }

        fun clone(): Tutorial {
            val r = Tutorial(creator, name)
            steps.forEach {
                r.steps.add(it.clone())
            }
            r.id = this.id
            return r
        }

        fun appendToJson(writer: JsonWriter) {
            writer.beginObject()
                .name("creator").value(creator)
                .name("name").value(name)
                .name("id").value(id)
                .name("triggerMethod").value(triggerMethod.name)
                .name("triggerArgs").jsonValue(triggerArgument.toString())
                .name("isDraft").value(isDraft)
            writer.name("steps").beginArray()
            forEachStep {
                it.appendToJson(writer)
            }
            writer.endArray()
                .name("played").beginArray()
            played.forEach {
                writer.value(it)
            }
            writer.endArray().endObject()
        }

        companion object {
            fun fromJson(reader: JsonReader): Tutorial? {
                var name = ""
                var creator = ""
                var id = -1
                var triggerMethod = NONE
                var triggerArgs = JsonObject()
                var isDraft = false
                val played = ArrayList<String>()

                val json = JsonParser().parse(reader).asJsonObject
                if (json.has("name"))
                    name = json["name"].asString
                if (json.has("creator"))
                    creator = json["creator"].asString
                if (json.has("id"))
                    id = json["id"].asInt
                if (json.has("triggerMethod"))
                    triggerMethod = valueOf(json["triggerMethod"].asString)
                if (json.has("triggerArgs"))
                    triggerArgs = json["triggerArgs"].asJsonObject
                if (json.has("isDraft"))
                    isDraft = json["isDraft"].asBoolean
                if (json.has("played"))
                    json["played"].asJsonArray.forEach { played.add(it.asString) }
                if (name.isEmpty() || creator.isEmpty() || id == -1)
                    return null
                val project = Tutorial(creator, name)
                project.id = id
                project.triggerMethod = triggerMethod
                project.triggerArgument = triggerArgs
                project.isDraft = isDraft
                project.played.addAll(played)

                val steps = json["steps"].asJsonArray
                steps.forEach {
                    project.steps.add(
                        TutorialStep.fromJson(
                            JsonReader(
                                StringReader(it.asJsonObject.toString())
                            ),
                            project
                        )
                    )
                }
                return project
            }
        }

        enum class TutorialSwitchType {
            Teleport, Linear
        }

        class TutorialStep(val project: Tutorial) {
            var to: Location? = null
            var type: TutorialSwitchType = TutorialSwitchType.Teleport
            var time: Long = 2 * 1000L
            var title = ""
            var subTitle = ""

            private fun playTitle(entity: Entity): Boolean {
                if (title.isEmpty() && subTitle.isEmpty())
                    return true
                if (entity is Player) {
                    entity.sendTitle(
                        TextUtil.getCustomizedText(title, entity.info()),
                        TextUtil.getCustomizedText(subTitle, entity.info()),
                        7,
                        (time / 1000.0 * 20).toInt(),
                        7
                    )
                    return true
                }
                return false
            }

            fun showTo(entity: Entity) {
                val to = this.to ?: entity.location
                if (type == TutorialSwitchType.Teleport) {
                    entity.tplock(to, time)
                    playTitle(entity)
                } else {
                    entity.linearMotion(to, time, 20)
                    playTitle(entity)
                }
                Thread.sleep(time + 100)
            }

            fun appendToJson(writer: JsonWriter) {
                writer.beginObject()
                    .name("location")
                to
                    ?.appendToJson(writer)
                    ?: writer.nullValue()
                writer.name("type").value(type.name)
                    .name("time").value(time)
                    .name("title").value(title)
                    .name("subtitle").value(subTitle)
                    .endObject()
            }

            companion object {
                fun fromJson(reader: JsonReader, parent: Tutorial): TutorialStep {
                    var location: Location? = null
                    var type = TutorialSwitchType.Teleport
                    var time = -1L
                    var title = ""
                    var subtitle = ""

                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "location" -> location = fromJsonToLocation(reader)
                            "type" -> type = TutorialSwitchType.valueOf(reader.nextString())
                            "time" -> time = reader.nextLong()
                            "title" -> title = reader.nextString()
                            "subtitle" -> subtitle = reader.nextString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()

                    val r = TutorialStep(parent)
                    r.to = location
                    r.type = type
                    r.time = time
                    r.title = title
                    r.subTitle = subtitle
                    return r
                }
            }

            fun clone(): TutorialStep {
                val r = TutorialStep(this.project)
                r.time = this.time
                r.subTitle = this.subTitle
                r.title = this.title
                r.to = this.to?.clone()
                r.type = this.type
                return r
            }

            override fun equals(other: Any?): Boolean =
                other is TutorialStep && other.to == this.to && other.type == this.type && other.type == this.type

            override fun hashCode(): Int {
                var result = to?.hashCode() ?: 1
                result = 31 * result + type.hashCode()
                return result
            }
        }
    }
}