package com.zhufu.opencraft

import com.zhufu.opencraft.Base.endWorld
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.Base.netherWorld
import com.zhufu.opencraft.Base.surviveWorld
import com.zhufu.opencraft.DualInventory.Companion.RESET
import com.zhufu.opencraft.Info.GameStatus.*
import com.zhufu.opencraft.events.PlayerLogoutEvent
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.io.File

@Suppress("unused")
class SurviveListener(private val plugin: JavaPlugin) : Listener {
    companion object {
        lateinit var INSTANCE: SurviveListener
        fun Player.solveSurvivorRequest(info: Info) {
            isInvulnerable = true
            if (!info.isSurveyPassed && info.remainingDemoTime <= 0) {
                PlayerManager.onPlayerOutOfDemo(info)
                return
            }
            val getter = getLangGetter(player?.info())
            if (!info.isLogin) {
                info.player.info(getter["user.login1"])
                return
            }

            fun randomSpawn(inventory: DualInventory) {
                gameMode = GameMode.SURVIVAL

                var bound = 20000
                bound -= (File("plugins${File.separatorChar}inventories").listFiles()?.size ?: 0) * 10
                if (bound <= 5000) bound = 5000
                var r = Base.getRandomLocation(surviveWorld, bound, y = 128)
                while (r.block.biome.name.contains("OCEAN"))
                    r = Base.getRandomLocation(surviveWorld, bound, y = 128)
                teleport(r)

                info.status = Surviving
                inventory.create("survivor").load()
            }

            if (info.isSurvivor) {
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
                        .sendMessage(
                            arrayOf(
                                TextUtil.error(getter["survival.compe.1"]),
                                TextUtil.info(getter["survival.compe.2"]),
                                TextUtil.tip(getter["survival.compe.3"])
                            )
                        )

                    randomSpawn(info.inventory)
                }

                val l = inventory.get<Location>("location")
                val s = info.tag.getSerializable("surviveSpawn", Location::class.java, null)
                val isLocationCorrect = l?.world == surviveWorld || l?.world == netherWorld || l?.world == endWorld
                val isSpawnCorrect = s?.world == surviveWorld
                if (!isLocationCorrect && isSpawnCorrect) {
                    sendMessage(
                        arrayOf(
                            TextUtil.error(getter["survival.saveNotFound.1"]),
                            TextUtil.info(getter["survival.saveNotFound.2"])
                        )
                    )
                    inventory.set("location", s!!)
                    teleport(s)
                } else if (isLocationCorrect && !isSpawnCorrect) {
                    sendMessage(
                        arrayOf(
                            TextUtil.error(getter["survival.spawnNotFound.1"]),
                            TextUtil.info(getter["survival.spawnNotFound.2"])
                        )
                    )
                    info.tag.set("surviveSpawn", l)
                } else if (!isLocationCorrect && !isSpawnCorrect) {
                    reset()
                    return
                }

                try {
                    if (!player!!.isOp && inventory.has("gameMode") && inventory.get<String>("gameMode") != GameMode.SURVIVAL.name) {
                        inventory.set("gameMode", GameMode.SURVIVAL.name)
                    }
                    inventory.load()
                    info.status = Surviving
                    Bukkit.getScheduler().runTaskLater(INSTANCE.plugin, { _ ->
                        info(getter["survival.loseProtect"])
                        isInvulnerable = false
                    }, 4 * 20)
                } catch (e: Exception) {
                    Bukkit.getLogger().warning("Unable to solve $name's survival request.")
                    e.printStackTrace()
                    sendMessage(TextUtil.printException(e))
                    info.apply {
                        this.inventory.create(RESET).load(false)
                        status = InLobby
                        isSurvivor = false
                    }
                }
            } else {
                randomSpawn(info.inventory)
            }
        }
    }

    init {
        INSTANCE = this
        ServerCaller["SolvePlayerSurvive"] = {
            val player = (it.firstOrNull()
                ?: throw IllegalArgumentException("This call must be give at least one Player parameter.")) as Player
            val info = player.info()
            if (info != null) {
                player.solveSurvivorRequest(info)
            } else {
                player.error(getLangGetter(info)["player.error.unknown"])
            }
        }
    }

    private fun validateInfo(info: Info?): Boolean {
        return info != null
    }

    private val coolDown = arrayListOf<Player>()
    @EventHandler
    fun onPlayerBootComputer(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            if (coolDown.contains(event.player))
                return
            coolDown.add(event.player)
            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                coolDown.remove(event.player)
            }, 3)

            val plank = event.clickedBlock!!.let {
                if (it.type == Material.STONE_BUTTON)
                    it.getRelative(BlockFace.DOWN)
                else
                    return
            }

            fun validateDirection(direction: BlockFace) =
                plank.getRelative(direction).type == Material.IRON_TRAPDOOR
                        && plank.getRelative(direction, 2).type == Material.PISTON
            if (
                plank.type.name.endsWith("_PLANKS")
                && (validateDirection(BlockFace.WEST)
                        || validateDirection(BlockFace.EAST)
                        || validateDirection(BlockFace.SOUTH)
                        || validateDirection(BlockFace.NORTH))
            ) {
                Bukkit.getLogger().info("Computer booted.")
                val info = event.player.info()
                val getter = info.getter()
                if (info == null) {
                    event.player.error(getter["player.error.unknown"])
                    return
                }
                if (!info.isLogin)
                    ServerCaller["SolvePlayerLogin"]!!(listOf(info))
                else {
                    info.logout()
                    with(Bukkit.getPluginManager()) {
                        callEvent(PlayerLogoutEvent(info, true))
                        callEvent(
                            PlayerTeleportedEvent(
                                event.player,
                                event.player.location,
                                PlayerLobbyManager[info].spawnPoint
                            )
                        )
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (!validateInfo(info))
            return
        when (info!!.status) {
            InLobby -> {
                info.inventory.create(RESET).load()
            }
            Surviving -> {
                event.respawnLocation =
                    info.tag.getSerializable("surviveSpawn", Location::class.java, null) ?: Base.lobby.spawnLocation
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
        val isSurvivor = info.isSurvivor
        if (!isSurvivor && event.to.world == surviveWorld && event.to.clone().add(
                Vector(
                    0,
                    -1,
                    0
                )
            ).block.type != Material.AIR
        ) {
            info.tag.set("isSurvivor", true)
            info.saveServerID()

            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                if (info.status != Surviving) return@runTaskLater
                val player = event.player
                val getter = getLangGetter(player.info())
                info.tag.set("surviveSpawn", player.location)
                info.inventory.create("survivor").save()
                player.isInvulnerable = false
                player.sendTitle(TextUtil.info(getter["survival.loseProtect"]), getter["survival.setSpawn"], 7, 70, 7)
            }, 3 * 20)
        }
        if (info.status == Surviving && !info.isSurveyPassed) {
            if (event.to.world != surviveWorld)
                return
            val distance = event.to.distance(info.tag.getSerializable("surviveSpawn", Location::class.java) ?: return)
            if (distance >= 100) {
                if (!info.tag.getBoolean("isOutOfSpawn", false)) {
                    info.tag.set("isOutOfSpawn", true)
                    event.player.info(getLang(info, "survey.outOfSpawn"))
                }
            } else {
                if (info.tag.getBoolean("isOutOfSpawn", false)) {
                    info.tag.set("isOutOfSpawn", false)
                    event.player.info(getLang(info, "survey.intoSpawn"))
                }
            }
        }
        if (info.status == InLobby && event.to.world == lobby && !PlayerLobbyManager.isTargetOf(event.player)) {
            val target = PlayerLobbyManager.targetOf(event.player) ?: return
            val to = event.to
            when {
                to.blockX < target.fromX -> {
                    event.player.teleport(to.clone().apply { x = target.fromX.toDouble() })
                }
                to.blockX > target.toX -> {
                    event.player.teleport(to.clone().apply { x = target.toX.toDouble() })
                }
                to.blockZ < target.fromZ -> {
                    event.player.teleport(to.clone().apply { z = target.fromZ.toDouble() })
                }
                to.blockZ > target.toZ -> {
                    event.player.teleport(to.clone().apply { z = target.toZ.toDouble() })
                }
            }
            ServerCaller["SolveLobbyVisitor"]!!(listOf(info))
        }
    }

    @EventHandler
    fun onPlayerSleep(event: PlayerBedEnterEvent) {
        val info = event.player.info()
        val getter = event.player.getter()
        if (info == null) {
            event.player.error(getter["player.error.unknown"])
            return
        }
        if (info.status == Surviving) {
            if (info.isSurveyPassed || !info.tag.getBoolean("isOutOfSpawn", false)) {
                event.player.success(getter["user.spawnpoint.saved"])
                info.tag.set("surviveSpawn", event.bed.location)
                event.player.bedSpawnLocation = event.bed.location
            } else {
                event.isCancelled = true
                sendPlayerOutOfSpawnMessage(event.player)
            }
        } else if (info.status == InLobby && event.player.world == lobby) {
            val own = PlayerLobbyManager[info]
            if (own.contains(event.bed.location)) {
                own.spawnPoint = event.bed.location
                event.player.success(getter["lobby.spawnpointSet"])
            }
        }
    }

    private fun sendPlayerOutOfSpawnMessage(player: HumanEntity) {
        val getter = getLangGetter(PlayerManager.findInfoByPlayer(player.uniqueId))
        player.sendMessage(
            arrayOf(
                TextUtil.error(getter["survey.outOfSpawn2"]),
                TextUtil.tip(getter["survey.toPlaceBlock"])
            )
        )
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (!validateInfo(info)) {
            return
        }
        if (info!!.player.location.world == surviveWorld && !info.isSurveyPassed && info.tag.getBoolean(
                "isOutOfSpawn",
                false
            )
        ) {
            event.isCancelled = true
            sendPlayerOutOfSpawnMessage(event.player)
        } else if (info.status == InLobby && event.block.world == lobby) {
            if (!PlayerLobbyManager.isInOwnLobby(info)) {
                event.isCancelled = true
                event.player.error(info.getter()["lobby.error.breakNotPermitted"])
            }
        }
    }

    @EventHandler
    fun onPlayerPlaceBlock(event: BlockPlaceEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (info == null) {
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }
        if (info.player.location.world == surviveWorld && !info.isSurveyPassed && info.tag.getBoolean(
                "isOutOfSpawn",
                false
            )
        ) {
            event.isCancelled = true
            sendPlayerOutOfSpawnMessage(event.player)
        } else if (info.status == InLobby && event.blockPlaced.world == lobby) {
            if (PlayerLobbyManager[info].contains(event.blockPlaced.location)) {
                if (listOf(
                        Material.ENDER_CHEST,
                        Material.TNT,
                        Material.TNT_MINECART
                    ).contains(event.blockPlaced.type)
                ) {
                    event.isCancelled = true
                }
            } else {
                event.isCancelled = true
                event.player.error(info.getter()["lobby.error.buildNotPermitted"])
            }
        }
    }

    @EventHandler
    fun onPlayerOpenInventory(event: InventoryOpenEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player.uniqueId)
        if (info == null) {
            event.isCancelled = true
            event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
            return
        }
        if (info.player.location.world == surviveWorld && !info.isSurveyPassed && info.tag.getBoolean(
                "isOutOfSpawn",
                false
            )
        ) {
            event.isCancelled = true
            sendPlayerOutOfSpawnMessage(event.player)
        }
    }

    @EventHandler
    fun onPlayerTeleported(event: PlayerTeleportedEvent) {
        when (event.to?.world) {
            lobby -> {
                if (event.from != null)
                    PlayerManager.findInfoByPlayer(event.player)
                        ?.also {
                            event.isCancelled = true
                            it.logout()
                            Bukkit.getPluginManager().callEvent(PlayerLogoutEvent(it, true))
                        }
                        ?.inventory?.create(RESET)?.load(savePresent = true)
            }
            surviveWorld -> {
                val info = PlayerManager.findInfoByPlayer(event.player)
                if (info == null) {
                    event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                    return
                }
                if (info.status != Surviving) {
                    event.isCancelled = true
                    event.player.error(getLang(info, "survival.teleport"))
                }
            }
            else -> {
                if (event.to?.world == endWorld || event.to?.world == surviveWorld) {
                    val info = PlayerManager.findInfoByPlayer(event.player)
                    if (info == null) {
                        event.isCancelled = true
                        event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                        event.player.sendMessage(Game.gameWarn)
                        return
                    }
                    if (!info.tag.getBoolean("isSurvivor", false)) {
                        event.player.sendMessage(
                            arrayOf(
                                TextUtil.error(getLang(info, "survival.notRegistered.1")),
                                TextUtil.tip(getLang(info, "survival.notRegistered.2"))
                            )
                        )
                    } else {
                        event.player.solveSurvivorRequest(info)
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerEnterPortal(event: PlayerPortalEvent) {
        var location = event.player.location
        val targets = ArrayList<Location>()
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    targets.add(location.clone().add(Vector(x, y, z)))
                }
            }
        }
        val isNetherPortal = targets.any { it.block.type == Material.NETHER_PORTAL }
        //event.useTravelAgent(isNetherPortal)
        print("${event.player.name} entered a ${if (isNetherPortal) "nether" else "end"} portal")
        when (event.player.world) {
            surviveWorld -> {
                if (isNetherPortal) {
                    location.x /= 8
                    location.z /= 8
                    location.world = netherWorld
                } else {
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
                if (info == null) {
                    event.player.sendMessage(TextUtil.error(Language.getDefault("player.error.unknown")))
                    return
                }
                location = info.tag.getSerializable("surviveSpawn", Location::class.java)!!
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