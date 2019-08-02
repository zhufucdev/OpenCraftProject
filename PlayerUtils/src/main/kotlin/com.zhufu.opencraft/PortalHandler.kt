package com.zhufu.opencraft

import com.zhufu.opencraft.Base.TutorialUtil.linearTo
import com.zhufu.opencraft.Everything.mPlugin
import com.zhufu.opencraft.Everything.near
import com.zhufu.opencraft.special_item.Portal
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import kotlin.math.pow

object PortalHandler : Listener {
    class FallbackInfo(val location: Location, val held: Int, val amount: Int)

    val portalMap = HashMap<Player, FallbackInfo>()

    private lateinit var timer: BukkitTask
    fun init(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        var i = 4
        timer = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (i <= 4) {
                i++
            } else {
                i = 0
            }
            Everything.cubes.filter { it.type == "TP" }.forEach {
                it.from.world!!.apply {
                    val particle =
                        if (!it.data.getBoolean("isCoolDown", false)) Particle.PORTAL else Particle.SMOKE_NORMAL
                    spawnParticle(particle, it.from.clone().add(0.5, 0.0, 0.5), 100, 0.3, 0.3, 0.3)
                    spawnParticle(particle, it.to.clone().add(0.5, 0.0, 0.5), 100, 0.3, 0.3, 0.3)

                    if (i >= 4) {
                        playSound(it.from, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 1f)
                        playSound(it.to, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 1f)
                    }
                }
            }
        }, 0, 30)
    }

    fun onServerClose() {
        timer.cancel()
        portalMap.values.forEach {
            it.location.block.type = Material.AIR
        }
    }

    private fun Player.owns(sth: Everything.Cube?) =
        sth != null && sth.savedData.has("owner") && sth.savedData["owner"].asString.let { it == name || (it == "op" && isOp) }

    private fun blockDistance(a: Location, b: Location) =
        (a.x.toFloat() - b.x.toFloat()).pow(2) + (a.y.toFloat() - b.y.toFloat()).pow(2) + (a.z.toFloat() - b.z.toFloat()).pow(
            2
        )

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        Bukkit.getScheduler().runTaskAsynchronously(Everything.mPlugin!!) { _ ->
            for (cube in Everything.cubes) {
                if (cube.type != "TP")
                    continue

                fun doTeleport(destation: Location) {
                    if (cube.data.getBoolean("isCoolDown", false)) {
                        event.player.sendActionText(TextUtil.error("传送门正在冷却"))
                        return
                    }
                    val dest = destation.clone().add(Vector(0.5, 1.0, 0.5))
                    cube.data.set("isCoolDown", true)
                    Bukkit.getScheduler().runTask(mPlugin!!) { _ ->
                        if (blockDistance(dest, event.player.location) <= 22500) {
                            val info = event.player.info()
                            if (info == null) {
                                event.player.error(Language.getDefault("player.error.unknown"))
                            } else {
                                info.inventory.create(DualInventory.NOTHING).load(inventoryOnly = true)
                                event.player.gameMode = GameMode.SPECTATOR
                                event.player.linearTo(
                                    location = dest,
                                    delay = 3600,
                                    done = {
                                        event.player.apply {
                                            info.inventory.last.apply {
                                                set("location", dest)
                                                load()
                                            }
                                            playSound(location, Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 1f)
                                        }
                                    }
                                )
                            }
                        } else {
                            event.player.apply {
                                dest.chunk.load(true)
                                info(getLang(this, "portal.loadChunk"))
                                teleport(dest)
                                playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
                            }
                        }
                        Bukkit.getScheduler().runTaskLater(mPlugin!!, { _ ->
                            cube.data.set("isCoolDown", false)
                        }, 10 * 20)
                    }
                }
                if (cube.from.near(event.to!!.clone().add(Vector(0, -1, 0)))) {
                    doTeleport(cube.to.clone().setDirection(event.player.location.direction))
                    break
                } else if (cube.to.near(event.to!!.clone().add(Vector(0, -1, 0)))) {
                    doTeleport(cube.from.clone().setDirection(event.player.location.direction))
                    break
                }
            }
        }

    }

    private fun blockBreak(location: Location, player: Player): Boolean {
        val game = Everything.index(location)
        val owner = player.owns(game) || player.isOp
        if (game?.type == "TP" && (game.from.near(location) || game.to.near(location))) {
            if (owner) {
                game.apply {
                    from.block.type = Material.AIR
                    to.block.type = Material.AIR
                }
                game.from.world!!.apply {
                    spawnParticle(
                        Particle.REDSTONE,
                        game.from.clone().add(0.5, 0.0, 0.5),
                        60,
                        0.3,
                        0.3,
                        0.3,
                        Particle.DustOptions(Color.FUCHSIA, 5f)
                    )
                    spawnParticle(
                        Particle.REDSTONE,
                        game.to.clone().add(0.5, 0.0, 0.5),
                        60,
                        0.3,
                        0.3,
                        0.3,
                        Particle.DustOptions(Color.FUCHSIA, 5f)
                    )
                }
                Everything.cubes.remove(game)

                player.warn(getLang(player, "portal.tip"))
                return true
            } else {
                player.error(getLang(player, "portal.notOwn"))
                return false
            }
        }

        val obs = portalMap[player]?.location
        if (portalMap.containsKey(player) && location.near(obs!!)) {
            obs.block.type = Material.AIR
            player.inventory.setItem(
                portalMap[player]!!.held,
                Portal(player.getter()).apply { amount = portalMap[player]!!.amount })
            portalMap.remove(player)
        }
        return true
    }

    @EventHandler
    fun onClick(event: PlayerInteractEvent) {
        if (
            event.action == Action.LEFT_CLICK_BLOCK
            && (event.player.inventory.itemInMainHand.type.name.contains("pickaxe", true)
                    || portalMap.containsKey(event.player))
        ) {
            blockBreak(event.clickedBlock!!.location, event.player)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        event.isCancelled = !blockBreak(event.block.location, event.player)
    }

    @EventHandler
    fun onSecondaryDrop(event: PlayerDropItemEvent) {
        if (Portal.isThis(event.itemDrop.itemStack) && portalMap.containsKey(event.player)) {
            event.isCancelled = true
            event.player.error(getLang(event.player, "portal.dropSecondary"))
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!event.canBuild())
            return
        if (Portal.isThis(event.itemInHand)) {
            val getter = event.player.getter()
            if (!portalMap.containsKey(event.player)) {
                portalMap[event.player] =
                    FallbackInfo(
                        location = event.blockPlaced.location,
                        held = event.player.inventory.heldItemSlot,
                        amount = event.player.inventory.itemInMainHand.amount
                    )
                event.player.inventory.setItemInMainHand(Portal(getter, true))
                event.player.tip(getter["portal.place"])
            } else {
                val first = portalMap[event.player]!!.location
                val second = event.blockPlaced.location
                Everything.createTP(first, second, event.player.name)

                event.player.apply {
                    success(getter["portal.spawned"])
                    val amount =
                        portalMap[event.player]!!.amount - if (event.player.gameMode == GameMode.CREATIVE) 0 else 1
                    inventory.setItemInMainHand(if (amount <= 0) ItemStack(Material.AIR) else Portal(getter()).apply {
                        this.amount = amount
                    })
                }

                portalMap.remove(event.player)
            }
        }
    }

    @EventHandler
    fun onSecondaryPortalClick(event: InventoryClickEvent) {
        if (Portal.isThis(event.currentItem) && portalMap.containsKey(Bukkit.getPlayer(event.whoClicked.uniqueId))) {
            event.whoClicked.error(getLang(event.whoClicked, "portal.mustPlaceFirst"))
            event.isCancelled = true
        }
    }
}