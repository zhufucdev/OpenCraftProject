package com.zhufu.opencraft

import com.zhufu.opencraft.Base.endWorld
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.Base.netherWorld
import com.zhufu.opencraft.Base.surviveWorld
import com.zhufu.opencraft.DualInventory.Companion.RESET
import com.zhufu.opencraft.Info.GameStatus.*
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.io.File

class SurviveListener(private val plugin: JavaPlugin) : Listener {
    companion object {
        lateinit var mInstance: SurviveListener

        fun getHelpDoc(lang: String): File = File("plugins/UserManager","help-$lang.txt").also {
            if (!it.parentFile.exists())
                it.parentFile.mkdirs()
        }
        fun showHelp(info: Info, force: Boolean){
            fun show(){
                info.player.sendMessage(TextUtil.info(">>${Language[info,"helpDoc.title"]}"))
                val helpDoc = getHelpDoc(info.userLanguage)
                if (!helpDoc.exists())
                    return
                helpDoc.forEachLine {
                    info.player.sendMessage(TextUtil.getCustomizedText(it))
                }
                info.player.sendMessage(TextUtil.info(">>END"))

                info.tag.set("isHelpDocShown",true)
            }
            val isShown = info.tag.getBoolean("isHelpDocShown",false)
            if (force){
                show()
            } else if (!isShown){
                show()
            }
        }
        fun Player.solveSurvivorRequest(info: Info) {
            isInvulnerable = true
            if (!info.isSurveyPassed && info.remainingDemoTime <= 0) {
                PlayerManager.onPlayerOutOfDemo(info)
                return
            }
            val getter = getLangGetter(player?.info())
            if (!info.isLogin){
                info.player.info(getter["user.login1"])
                return
            }

            fun randomSpawn(inventory: DualInventory) {
                gameMode = GameMode.SURVIVAL

                var bound = 20000
                bound -= (File("plugins${File.separatorChar}inventories").listFiles()?.size ?: 0) * 10
                if (bound <= 5000) bound = 5000
                var r = Base.getRandomLocation(Base.surviveWorld, bound, y = 128)
                while (r.block.biome.name.contains("OCEAN"))
                    r = Base.getRandomLocation(Base.surviveWorld, bound, y = 128)
                teleport(r)

                info.status = Surviving
                inventory.create("survivor").load()
            }

            if (info.tag.getBoolean("isSurvivor", false)) {
                if (info.status == Surviving) {
                    isInvulnerable = false
                    return
                }
                val inventory = info.inventory.create("survivor")

                fun reset() {
                    val config = info.inventory.create("survivor")
                    if (config.has("inventory"))
                        for (i in 0 until this.inventory.size) {
                            this.inventory.setItem(i, config.get<ItemStack>("inventory.$i") ?: continue)
                        }

                    info.tag.set("isSurvivor", false)
                    this
                            .also {
                                it.inventory.addItem(ItemStack(Material.DIAMOND, 2))
                            }
                            .sendMessage(arrayOf(
                                    TextUtil.error(getter["survival.compe.1"]),
                                    TextUtil.info(getter["survival.compe.2"]),
                                    TextUtil.tip(getter["survival.compe.3"])
                            ))

                    randomSpawn(info.inventory)
                }

                val l = inventory.get<Location>("location")
                val s = info.tag.getSerializable("surviveSpawn",Location::class.java,null)
                val isLocationCorrect = l?.world == surviveWorld || l?.world == netherWorld || l?.world == endWorld
                val isSpawnCorrect = s?.world == surviveWorld
                if (!isLocationCorrect && isSpawnCorrect) {
                    sendMessage(arrayOf(
                            TextUtil.error(getter["survival.saveNotFound.1"]),
                            TextUtil.info(getter["survival.saveNotFound.2"])
                    ))
                    inventory.set("location", s!!)
                    teleport(s)
                } else if (isLocationCorrect && !isSpawnCorrect) {
                    sendMessage(arrayOf(
                            TextUtil.error(getter["survival.spawnNotFound.1"]),
                            TextUtil.info(getter["survival.spawnNotFound.2"])
                    ))
                    info.tag.set("surviveSpawn", l)
                } else if (!isLocationCorrect && !isSpawnCorrect) {
                    reset()
                    return
                }

                try {
                    if (!player!!.isOp && inventory.has("gameMode") && inventory.get<String>("gameMode") != GameMode.SURVIVAL.name) {
                        inventory.set("gameMode", GameMode.SURVIVAL.name)
                    }
                    showHelp(info, false)
                    inventory.load()
                    info.status = Surviving
                    Bukkit.getScheduler().runTaskLater(mInstance.plugin, { _ ->
                        info(getter["survival.loseProtect"])
                        isInvulnerable = false
                    }, 4 * 20)
                } catch (e: Exception) {
                    e.printStackTrace()
                    sendMessage(TextUtil.printException(e))
                    teleport(Base.lobby.spawnLocation)
                    info.tag.set("isSurvivor", false)
                }
            } else {
                randomSpawn(info.inventory)
            }
        }
    }

    init {
        mInstance = this
    }

    private fun validateInfo(info: Info?): Boolean {
        return info != null
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (!validateInfo(info))
            return
        when (info!!.status){
            InLobby -> {
                info.inventory.create(RESET).load()
                event.respawnLocation = lobby.spawnLocation
            }
            Surviving -> {
                event.respawnLocation = info.tag.getSerializable("surviveSpawn",Location::class.java,null) ?: Base.lobby.spawnLocation
            }
            InTutorial -> {
                info.inventory.create(RESET).load()
            }
            else -> {
                event.respawnLocation = lobby.spawnLocation
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val info = PlayerManager.findInfoByPlayer(event.entity)
        if (validateInfo(info))
            info!!.tag
                    .apply {
                        if (info.status != Surviving)
                            return
                        set("lastDeath.location", event.entity.location)
                        set("lastDeath.reason", event.deathMessage)
                        set("lastDeath.time", System.currentTimeMillis())
                    }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (!validateInfo(info))
            return
        info!!
        val isSurvivor = info.tag.getBoolean("isSurvivor", false)
        if (event.to.world == lobby && event.to.y <= lobby.spawnLocation.y - 30) {
            event.player.solveSurvivorRequest(info)
        } else if (!isSurvivor && event.to.world == Base.surviveWorld && event.to.clone().add(Vector(0, -1, 0)).block.type != Material.AIR) {
            info.tag.set("isSurvivor", true)
            info.saveServerID()

            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                val player = event.player
                val getter = getLangGetter(player.info())
                info.tag.set("surviveSpawn", player.location)
                info.inventory.create("survivor").save()
                player.isInvulnerable = false
                player.sendTitle(TextUtil.info(getter["survival.loseProtect"]), getter["survival.setSpawn"], 7, 70, 7)

                showHelp(info, false)
            }, 3 * 20)
        }
        if (info.status == Surviving && !info.isSurveyPassed){
            if (event.to.world != surviveWorld)
                return
            val distance = event.to.distance(info.tag.getSerializable("surviveSpawn",Location::class.java)?:return)
            if (distance >= 100) {
                if (!info.tag.getBoolean("isOutOfSpawn",false)) {
                    info.tag.set("isOutOfSpawn", true)
                    event.player.info(getLang(info,"survey.outOfSpawn"))
                }
            }
            else{
                if (info.tag.getBoolean("isOutOfSpawn",false)) {
                    info.tag.set("isOutOfSpawn", false)
                    event.player.info(getLang(info,"survey.intoSpawn"))
                }
            }
        }
    }

    @EventHandler
    fun onPlayerSleep(event: PlayerBedEnterEvent){
        val info = event.player.info()
        val getter = event.player.lang()
        if (info == null){
            event.player.error(getter["player.error.unknown"])
            return
        }
        event.isCancelled = true
        if (info.isSurveyPassed || !info.tag.getBoolean("isOutOfSpawn",false)){
            event.player.success(getter["user.spawnpoint.saved"])
            info.tag.set("surviveSpawn",event.bed.location)
            event.player.bedSpawnLocation = event.bed.location
        } else sendPlayerOutOfSpawnMessage(event.player)
    }

    private fun sendPlayerOutOfSpawnMessage(player: HumanEntity){
        val getter = getLangGetter(PlayerManager.findInfoByPlayer(player.uniqueId))
        player.sendMessage(arrayOf(
                TextUtil.error(getter["survey.outOfSpawn2"]),
                TextUtil.tip(getter["survey.toPlaceBlock"])
        ))
    }
    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent){
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (!validateInfo(info)){
            return
        }
        if (info!!.player.location.world == surviveWorld && !info.isSurveyPassed && info.tag.getBoolean("isOutOfSpawn",false)){
            event.isCancelled = true
            sendPlayerOutOfSpawnMessage(event.player)
        }
    }
    @EventHandler
    fun onPlayerPlaceBlock(event: BlockPlaceEvent){
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (info == null){
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }
        if (info.player.location.world == surviveWorld && !info.isSurveyPassed && info.tag.getBoolean("isOutOfSpawn",false)){
            event.isCancelled = true
            sendPlayerOutOfSpawnMessage(event.player)
        }
    }
    @EventHandler
    fun onPlayerOpenInventory(event: InventoryOpenEvent){
        val info = PlayerManager.findInfoByPlayer(event.player.uniqueId)
        if (info == null){
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }
        if (info.player.location.world == surviveWorld && !info.isSurveyPassed && info.tag.getBoolean("isOutOfSpawn",false)){
            event.isCancelled = true
            sendPlayerOutOfSpawnMessage(event.player)
        }
    }

    @EventHandler
    fun onPlayerTeleported(event: PlayerTeleportedEvent) {
        when(event.to.world){
            Base.lobby -> {
                PlayerManager.findInfoByPlayer(event.player)
                        ?.also {
                            it.status = InLobby
                        }
                        ?.inventory?.create(RESET)?.load(savePresent = true)
            }
            Base.surviveWorld -> {
                val info = PlayerManager.findInfoByPlayer(event.player)
                if (info == null){
                    event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                    return
                }
                if (info.status != Surviving) {
                    event.isCancelled = true
                    event.player.error(getLang(info,"survival.teleport"))
                }
            }
            else -> {
                if (event.to.world!!.name == "world_the_end" || event.to.world!!.name == "world_nether") {
                    val info = PlayerManager.findInfoByPlayer(event.player)
                    if (info == null) {
                        event.isCancelled = true
                        event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                        event.player.sendMessage(Game.gameWarn)
                        return
                    }
                    if (!info.tag.getBoolean("isSurvivor", false)) {
                        event.player.sendMessage(arrayOf(
                                TextUtil.error(getLang(info,"survival.notRegistered.1")),
                                TextUtil.tip(getLang(info,"survival.notRegistered.2"))
                        ))
                    } else {
                        event.player.solveSurvivorRequest(info)
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerEnterPortal(event: PlayerPortalEvent){
        var location = event.player.location
        val targets = ArrayList<Location>()
        for (x in -1 .. 1){
            for (y in -1 .. 1){
                for (z in -1 .. 1){
                    targets.add(location.clone().add(Vector(x, y, z)))
                }
            }
        }
        val isNetherPortal = targets.any { it.block.type == Material.NETHER_PORTAL }
        //event.useTravelAgent(isNetherPortal)
        print("${event.player.name} entered a ${if (isNetherPortal) "nether" else "end"} portal")
        when (event.player.world){
            surviveWorld -> {
                if (isNetherPortal) {
                    location.x /= 8
                    location.z /= 8
                    location.world = netherWorld
                }
                else{
                    location = endWorld.spawnLocation
                }
            }
            netherWorld -> {
                location.x *= 8
                location.z *= 8
                location.world = surviveWorld
            }
            endWorld -> {
                val info = PlayerManager.findInfoByPlayer(event.player)
                if (info == null){
                    event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                    return
                }
                location = info.tag.getSerializable("surviveSpawn",Location::class.java)!!
            }
            lobby -> {
                event.isCancelled = true
                return
            }
        }

        //if (isNetherPortal)
        //    location = event.portalTravelAgent.findOrCreate(location)
        event.setTo(location)
    }
}