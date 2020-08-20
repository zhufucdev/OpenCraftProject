package com.zhufu.opencraft
// SurviveListener is used to rule the players in gameplay.
import com.zhufu.opencraft.Base.endWorld
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.Base.netherWorld
import com.zhufu.opencraft.Base.pluginCore
import com.zhufu.opencraft.Base.surviveWorld
import com.zhufu.opencraft.DualInventory.Companion.RESET
import com.zhufu.opencraft.Info.GameStatus.*
import com.zhufu.opencraft.events.PlayerLogoutEvent
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.rpg.Role
import com.zhufu.opencraft.special_item.dynamic.SpecialItem
import com.zhufu.opencraft.special_item.maxMP
import com.zhufu.opencraft.special_item.mp
import com.zhufu.opencraft.special_item.mpRecoverRate
import com.zhufu.opencraft.special_item.showMP
import com.zhufu.opencraft.task.Task
import com.zhufu.opencraft.ui.RoleSelectUI
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import org.reflections.Reflections
import java.lang.reflect.Modifier
import kotlin.math.roundToInt

@Suppress("unused")
class SurviveListener(private val plugin: JavaPlugin) : Listener {
    companion object {
        lateinit var INSTANCE: SurviveListener
        fun Player.solveSurvivorRequest(info: Info): Boolean {
            isInvulnerable = true
            if (!info.isSurveyPassed && info.remainingDemoTime <= 0) {
                PlayerManager.onPlayerOutOfDemo(info)
                return false
            }
            val getter = info.getter()
            if (!info.isLogin) {
                info.player.info(getter["user.login1"])
                return false
            }

            if (!info.isSurvivor) {
                val center = Game.env.getLocation("survivalCenter")!!
                info.inventory.create("survivor").apply {
                    set("spawnpoint", center)
                }
                teleport(center)
                info.isSurvivor = true
            }
            info.inventory.load("survivor")
            info.status = Surviving

            fun delay() {
                Bukkit.getScheduler().runTaskLater(pluginCore, { _ ->
                    info(getter["survival.loseProtect"])
                    isInvulnerable = false
                }, 4 * 20)
            }
            if (info.role == Role.NULL) {
                RoleSelectUI(pluginCore, info.getter()) {
                    info(getter["rpg.ui.select.done", getter[it.nameCode]])

                    info.role = it
                    RPGUtil.sendEquipmentsTo(this)
                    delay()
                }
                    .show(this)
            } else delay()
            return true
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
        // Register special items
        Reflections("com.zhufu.opencraft.special_item")
            .getSubTypesOf(SpecialItem::class.java)
            .forEach { if (!Modifier.isAbstract(it.modifiers)) SpecialItem.register(it) }
        // Register Tasks
        Reflections("com.zhufu.opencraft.task")
            .getSubTypesOf(Task::class.java)
            .forEach { if (!Modifier.isAbstract(it.modifiers)) Task.register(it) }

        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            PlayerManager.forEachPlayer {
                // MP Recovery
                if (it.mp > it.maxMP) it.mp = it.maxMP
                if (it.mp < it.maxMP && (it.status == Surviving || it.status == InTask)) {
                    it.mp += it.mpRecoverRate
                    it.showMP(false)
                }
                // Buff
                RPGUtil.addBuff(it)
            }
        }, 0, 20)
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
                    info.tag.getSerializable("spawnpoint", Location::class.java, null)
                        ?: Game.env.getLocation("survivalCenter")!!
            }
            InTutorial -> {
                info.inventory.create(RESET).load()
            }
            Building -> {
                event.respawnLocation = surviveWorld.spawnLocation
            }
            else -> {
                event.respawnLocation = lobby.spawnLocation
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val info = PlayerManager.findInfoByPlayer(event.entity)
        if (validateInfo(info)) {
            if (info!!.status == Surviving) {
                info.tag
                    .apply {
                        set("lastDeath.location", event.entity.location)
                        set("lastDeath.reason", event.deathMessage)
                        set("lastDeath.time", System.currentTimeMillis())
                    }

                var dropInPercentage = 1.0
//                event.entity.inventory.specialItems.forEach { item ->
//                    if (item is Insurance && item.player == event.entity.name) {
//                        dropInPercentage *= 0.2
//                        event.entity.inventory.clear(item.inventoryPosition)
//                    }
//                }
                if (dropInPercentage != 1.0) {
                    val indexDropItem = arrayListOf<Int>()
                    val inventory = event.entity.inventory
                    val amountDropItem = (inventory.count { it != null } * dropInPercentage).roundToInt()
                    event.drops.clear()
                    for (i in 0 until amountDropItem) {
                        fun newRandom() = Base.random.nextInt(inventory.size)
                        var index = newRandom()
                        while (indexDropItem.contains(index) || inventory.getItem(index) == null)
                            index = newRandom()
                        indexDropItem.add(index)

                        val item = inventory.getItem(index)!!
                        val location = event.entity.eyeLocation
                        Bukkit.getScheduler().callSyncMethod(plugin) {
                            location.chunk.apply {
                                isForceLoaded = true
                                load()
                                Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                                    isForceLoaded = false
                                }, 5 * 20 * 60)
                            }
                        }
                        event.entity.world.dropItemNaturally(event.entity.eyeLocation, item)
                        inventory.clear(index)
                    }

                    event.entity.info(getLang(event.entity, "insurance.applied"))
                    event.newTotalExp = (event.entity.totalExperience * dropInPercentage).roundToInt()
                    event.keepInventory = true
                } else {
                    event.keepInventory = false
                    event.newTotalExp = 0
                }
                event.keepLevel = false
            }
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (!validateInfo(info))
            return
        info!!
        val isSurvivor = info.isSurvivor
        if (!isSurvivor && event.to.world == surviveWorld
            && event.to.clone().add(Vector(0, -1, 0)).block.type != Material.AIR
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
        if (info!!.player.location.world == surviveWorld && !info.isSurveyPassed
            && info.tag.getBoolean("isOutOfSpawn", false)
        ) {
            event.isCancelled = true
            sendPlayerOutOfSpawnMessage(event.player)
        } else if (info.status == InLobby && event.block.world == lobby) {
            if (!PlayerLobbyManager.isInOwnLobby(info) && !event.player.isOp && PlayerLobbyManager.targetOf(event.player)
                    ?.canBuildBy(event.player) != true
            ) {
                event.isCancelled = true
                event.player.sendActionText(info.getter()["lobby.error.breakNotPermitted"].toErrorMessage())
                ServerCaller["SolveLobbyVisitor"]!!(listOf(info))
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
            } else if (!event.player.isOp && PlayerLobbyManager.targetOf(event.player)
                    ?.canBuildBy(event.player) != true
            ) {
                event.isCancelled = true
                event.player.sendActionText(info.getter()["lobby.error.buildNotPermitted"].toErrorMessage())
                ServerCaller["SolveLobbyVisitor"]!!(listOf(info))
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
                        event.isCancelled = !event.player.solveSurvivorRequest(info)
                    }
                }
            }
        }
        if (!event.isCancelled) {
            event.player.isInvulnerable = true
            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                event.player.isInvulnerable = false
            }, 20)
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
        event.to = location
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        if (!event.isCancelled) {
            val damager = event.damager
            fun handlePlayer(damager: Player) {
                damager.info()?.apply {
                    if (status == Surviving || status == MiniGaming) {
                        statics?.let {
                            it.damageToday += event.finalDamage
                        }
                        damageDone += event.finalDamage
                    }
                }
            }

            if (damager is Player) {
                handlePlayer(damager)
            } else if (damager is Projectile) {
                val shooter = damager.shooter
                if (shooter is Player) {
                    handlePlayer(shooter)
                }
            }
        }
    }
}