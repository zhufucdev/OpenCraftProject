package com.zhufu.opencraft.listener

import com.zhufu.opencraft.*
import com.zhufu.opencraft.Base.endWorld
import com.zhufu.opencraft.Base.lobby
import com.zhufu.opencraft.Base.netherWorld
import com.zhufu.opencraft.Base.random
import com.zhufu.opencraft.Base.surviveWorld
import com.zhufu.opencraft.Base.tradeWorld
import com.zhufu.opencraft.api.ServerCaller
import com.zhufu.opencraft.data.*
import com.zhufu.opencraft.data.DualInventory.Companion.RESET
import com.zhufu.opencraft.data.Info.GameStatus.*
import com.zhufu.opencraft.events.UserLogoutEvent
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.lobby.PlayerLobby
import com.zhufu.opencraft.lobby.PlayerLobbyManager
import com.zhufu.opencraft.special_item.Coin
import com.zhufu.opencraft.special_item.Insurance
import com.zhufu.opencraft.special_item.SpecialItem
import com.zhufu.opencraft.util.*
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityPortalEnterEvent
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
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
            val getter = getLangGetter(player?.info())
            if (!info.isLogin) {
                info.player.info(getter["user.login1"])
                return false
            }

            fun randomSpawn(inventory: DualInventory) {
                gameMode = GameMode.SURVIVAL

                var bound = 20000
                bound -= OfflineInfo.count.toInt() * 10
                if (bound <= 5000) bound = 5000
                var r = Base.getRandomLocation(surviveWorld, bound, y = 128)
                var originallyLoaded = r.isChunkLoaded
                while (r.block.biome.name.contains("OCEAN")) {
                    if (!originallyLoaded)
                        r.chunk.unload(false)
                    r = Base.getRandomLocation(surviveWorld, bound, y = 128)
                    originallyLoaded = r.isChunkLoaded
                }
                teleport(r)

                info.status = Surviving
                inventory.getOrCreate("survivor").load()
            }

            if (info.isSurvivor) {
                if (info.status == Surviving) {
                    isInvulnerable = false
                    return true
                }
                val inventory = info.inventory.getOrCreate("survivor")

                fun reset() {
                    info.inventory.getOrCreate("survivor")

                    info.isSurvivor = false
                    inventory.addItem(ItemStack(Material.DIAMOND, 2))
                    error(getter["survival.compe.1"])
                    info(getter["survival.compe.2"])
                    tip(getter["survival.compe.3"])

                    randomSpawn(info.inventory)
                }

                val l = inventory.location
                val s = info.survivalSpawn
                val isLocationCorrect =
                    l != null && (l.world == surviveWorld || l.world == netherWorld || l.world == endWorld)
                val isSpawnCorrect = s?.world == surviveWorld
                if (!isLocationCorrect && isSpawnCorrect) {
                    error(getter["survival.saveNotFound.1"])
                    info(getter["survival.saveNotFound.2"])
                    inventory.set("location", s!!)
                    teleport(s)
                } else if (isLocationCorrect && !isSpawnCorrect) {
                    error(getter["survival.spawnNotFound.1"])
                    info(getter["survival.spawnNotFound.2"])
                    info.survivalSpawn = l
                } else if (!isLocationCorrect) {
                    reset()
                    return true
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
                    sendMessage(*TextUtil.printException(e))
                    info.apply {
                        this.inventory.getOrCreate(RESET).load(false)
                        status = InLobby
                        isSurvivor = false
                    }
                }
            } else {
                randomSpawn(info.inventory)
            }
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
                player.error(getLangGetter(null)["player.error.unknown"])
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
                Bukkit.getLogger().info("Computer was booted by ${event.player.name}.")
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
                        callEvent(UserLogoutEvent(info, true))
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
                val target = PlayerLobbyManager.targetOf(event.player) ?: PlayerLobbyManager[info]
                event.respawnLocation = target.spawnPoint ?: PlayerLobby.defaultSpawnpoint
            }

            Surviving -> {
                event.respawnLocation =
                    info.survivalSpawn ?: lobby.spawnLocation
            }

            InTutorial -> {
                info.inventory.getOrCreate(RESET).load()
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
                info.lastDeath = DeathInfo(
                    System.currentTimeMillis(),
                    event.deathMessage ?: "",
                    event.entity.location
                )

                var dropInPercentage = 1.0
                event.entity.inventory.specialItems.forEach { item ->
                    if (item is Insurance && item.player == event.entity.name) {
                        dropInPercentage *= 0.2
                        event.entity.inventory.clear(item.inventoryPosition)
                    }
                }
                if (dropInPercentage != 1.0) {
                    val indexDropItem = arrayListOf<Int>()
                    val inventory = event.entity.inventory
                    val amountDropItem = (inventory.count { it != null } * dropInPercentage).roundToInt()
                    event.drops.clear()
                    for (i in 0 until amountDropItem) {
                        fun newRandom() = random.nextInt(inventory.size)
                        var index = newRandom()
                        while (indexDropItem.contains(index) || inventory.getItem(index) == null)
                            index = newRandom()
                        indexDropItem.add(index)

                        val item = inventory.getItem(index)!!
                        inventory.setItem(index, null)

                        event.drops.add(item)
                    }
                    val location = event.entity.eyeLocation
                    Bukkit.getScheduler().callSyncMethod(plugin) {
                        location.chunk.apply {
                            addPluginChunkTicket(plugin)
                            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                                removePluginChunkTicket(plugin)
                            }, 5 * 20 * 60)
                        }
                    }

                    event.entity.info(getLang(event.entity, "insurance.applied"))
                    event.newTotalExp = (event.entity.totalExperience * dropInPercentage).roundToInt()
                    event.keepInventory = true
                } else {
                    if (!info.isInsuranceAdShown) {
                        event.entity.tip(getLang(event.entity, "insurance.tip"))
                        info.isInsuranceAdShown = true
                    }

                    event.keepInventory = false
                    event.newTotalExp = 0
                }
                event.keepLevel = false
            } else if (info.status == InLobby) {
                event.keepInventory = true
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
        if (!isSurvivor && event.to.world == surviveWorld && event.to.clone().add(
                Vector(
                    0,
                    -1,
                    0
                )
            ).block.type != Material.AIR
        ) {
            info.isSurvivor = true

            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                if (info.status != Surviving) return@runTaskLater
                val player = event.player
                val getter = getLangGetter(player.info())
                info.survivalSpawn = player.location
                info.inventory.getOrCreate("survivor").load()
                player.isInvulnerable = false
                val title = Title.title(
                    getter["survival.loseProtect"].toInfoMessage(),
                    getter["survival.setSpawn"].toComponent()
                )
                player.showTitle(title)
            }, 3 * 20)
        }
        if (info.status == Surviving && !info.isSurveyPassed) {
            if (event.to.world != surviveWorld)
                return
            val distance = event.to.distance(info.survivalSpawn ?: return)
            if (distance >= 100) {
                if (!info.outOfSpawn) {
                    info.outOfSpawn = true
                    event.player.info(getLang(info, "survey.outOfSpawn"))
                }
            } else {
                if (info.outOfSpawn) {
                    info.outOfSpawn = false
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
            if (info.isSurveyPassed || !info.outOfSpawn) {
                event.player.success(getter["user.spawnpoint.saved"])
                info.survivalSpawn = event.bed.location
                event.player.bedSpawnLocation = event.bed.location
            } else {
                event.isCancelled = true
                sendPlayerOutOfSpawnMessage(event.player)
            }
        } else if (info.status == InLobby && event.player.world == lobby) {
            val own = PlayerLobbyManager[info]
            if (own.contains(event.bed.location)) {
                own.spawnPoint = event.bed.location
                event.isCancelled = true
                event.player.success(getter["lobby.spawnpointSet"])
            }
        }
    }

    @EventHandler
    fun onSICrafted(event: CraftItemEvent) {
        if (event.inventory.matrix.any { SpecialItem.isSpecial(it ?: return@any false) }) {
            event.isCancelled = true
        }
    }

    private fun sendPlayerOutOfSpawnMessage(player: HumanEntity) {
        val info = player.info()
        val getter = info.getter()
        player.sendActionBar(getter["survey.outOfSpawn2"].toErrorMessage())
        if (info?.isSurveyRequestShown != true) {
            player.tip(getter["survey.toPlaceBlock"])
            info?.isSurveyRequestShown = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (!validateInfo(info)) {
            return
        }
        if (info!!.player.location.world == surviveWorld && !info.isSurveyPassed && info.outOfSpawn) {
            event.isCancelled = true
            sendPlayerOutOfSpawnMessage(event.player)
        } else if (info.status == InLobby && event.block.world == lobby) {
            if (!PlayerLobbyManager.isInOwnLobby(info) && !event.player.isOp && PlayerLobbyManager.targetOf(event.player)
                    ?.isPartner(event.player) != true
            ) {
                event.isCancelled = true
                event.player.sendActionBar(info.getter()["lobby.error.breakNotPermitted"].toErrorMessage())
                ServerCaller["SolveLobbyVisitor"]!!(listOf(info))
            }
        }
    }

    @EventHandler
    fun onPlayerPlaceBlock(event: BlockPlaceEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player)
        if (info == null) {
            event.isCancelled = true
            event.player.error(Language.getDefault("player.error.unknown"))
            return
        }
        if (info.player.location.world == surviveWorld && !info.isSurveyPassed && info.outOfSpawn
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
                    ?.isPartner(event.player) != true
            ) {
                event.isCancelled = true
                event.player.sendActionBar(info.getter()["lobby.error.buildNotPermitted"].toErrorMessage())
                ServerCaller["SolveLobbyVisitor"]!!(listOf(info))
            }
        }
    }

    @EventHandler
    fun onPlayerOpenInventory(event: InventoryOpenEvent) {
        val info = PlayerManager.findInfoByPlayer(event.player.uniqueId)
        if (info == null) {
            event.isCancelled = true
            event.player.sendMessage(Language.getDefault("player.error.unknown").toErrorMessage())
            return
        }
        if (info.player.location.world == surviveWorld && !info.isSurveyPassed && info.outOfSpawn
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
                            Bukkit.getPluginManager().callEvent(
                                UserLogoutEvent(
                                    it,
                                    true
                                )
                            )
                            it.logout()
                        }
            }

            surviveWorld -> {
                val info = PlayerManager.findInfoByPlayer(event.player)
                if (info == null) {
                    event.player.error(Language.getDefault("player.error.unknown"))
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
                        event.player.error(Language.getDefault("player.error.unknown"))
                        return
                    }
                    if (!info.isSurvivor) {
                        event.player.error(getLang(info, "survival.notRegistered.1"))
                        event.player.tip(getLang(info, "survival.notRegistered.2"))
                    } else {
                        event.isCancelled = !event.player.solveSurvivorRequest(info)
                    }
                }
            }
        }
        // invulnerable cooldown
        if (!event.isCancelled) {
            event.player.isInvulnerable = true

            if (event.to?.world == tradeWorld) {
                return
            }
            Bukkit.getScheduler().runTaskLater(plugin, { _ ->
                event.player.isInvulnerable = false
            }, 40)
        }
    }

    private fun handlePortal(entity: Entity, location: Location): Location? {
        val targets = ArrayList<Location>()
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    targets.add(location.clone().add(Vector(x, y, z)))
                }
            }
        }
        val isNetherPortal = targets.any { it.block.type == Material.NETHER_PORTAL }
        return when (location.world) {
            surviveWorld -> {
                if (isNetherPortal) {
                    location.toVector().divide(Vector(8, 8, 8)).toLocation(netherWorld)
                } else {
                    endWorld.spawnLocation
                }
            }

            netherWorld -> {
                location.toVector().multiply(8).toLocation(surviveWorld)
            }

            endWorld -> {
                if (entity is Player) {
                    val info = PlayerManager.findInfoByPlayer(entity)
                    if (info == null) {
                        entity.error(Language.getDefault("player.error.unknown"))
                        return null
                    }
                    info.survivalSpawn!!
                } else {
                    null
                }
            }

            else -> {
                return null
            }
        }
    }

    @EventHandler
    fun onPlayerEnterPortal(event: PlayerPortalEvent) {
        val dest = handlePortal(event.player, event.from)
        if (dest == null) {
            event.isCancelled = true
        } else {
            event.to = dest
        }
    }

    fun entityEnterPortal(event: EntityPortalEvent) {
        val dest = handlePortal(event.entity, event.from)
        if (dest == null) {
            event.isCancelled = true
        } else {
            event.to = dest
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        if (!event.isCancelled) {
            val damager = event.damager
            fun handlePlayer(damager: Player) {
                damager.info()?.apply {
                    if (status == Surviving || status == MiniGaming) {
                        statics.damageToday += event.finalDamage
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

    @EventHandler
    fun onCreeperExplode(event: EntityExplodeEvent) {
        if (event.entity is Creeper || event.entity is Wither) {
            event.blockList().clear()
        }
    }

    /**
     * 30% chance to drop coins ranging from 2 to 11 evenly randomized
     * from a monster
     */
    @EventHandler
    fun onMonsterDeath(event: EntityDeathEvent) {
        val whitelist = listOf(
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.SKELETON, EntityType.WITHER_SKELETON,
            EntityType.CREEPER, EntityType.ENDERMAN, EntityType.HUSK, EntityType.DROWNED
        )
        if (!whitelist.contains(event.entity.type)) {
            return
        }
        if (Base.trueByPercentages(0.7F)) {
            return
        }
        val amount = random.nextInt(10) + 2
        event.drops.add(Coin(Language.LangGetter.default).apply { setAmount(amount) })
    }

    @EventHandler
    fun onPlayerPickupSI(event: PlayerAttemptPickupItemEvent) {
        val item = event.item.itemStack
        val si = SpecialItem[item] ?: return
        si.updateMeta(event.player.getter())
        event.item.itemStack = si
    }
}