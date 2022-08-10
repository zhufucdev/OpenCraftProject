package com.zhufu.opencraft

import com.zhufu.opencraft.ui.ChartUI
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

object ChartHandler : Listener {
    private lateinit var timer: BukkitTask
    private lateinit var plugin: Plugin
    fun init(plugin: Plugin) {
        this.plugin = plugin

        if (Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            val chart = Game.dailyChart
            Everything.cubes.forEach {
                if (it.type == "CRT") {
                    spawnNPC(it, chart)
                }
            }

            timer = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                val newChart = Game.dailyChart
                Everything.cubes.forEach { cube ->
                    if (cube.type == "CRT") {
                        CitizensAPI.getNPCRegistry().toList().forEach { npc ->
                            if (npc.data().has("temp")
                                && cube.contains(npc.storedLocation.clone().add(Vector(0, -1, 0)))
                            ) {
                                val index = npc.data().get<Int>("position")
                                val newName = newChart[index].name
                                val oldName = npc.data().get<String>("name")
                                if (newName != oldName) {
                                    npc.destroy()
                                    updateFor(cube, index, newChart)
                                }
                            }
                        }
                    }
                }
            }, 60 * 20, 60 * 20)
        }

        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun spawnNPC(it: Everything.Cube, chart: List<ServerPlayer>) {
        CitizensAPI.getNPCRegistry().apply {
            if (chart.isNotEmpty()) {
                updateFor(it, 0, chart)
                if (chart.size >= 2) {
                    updateFor(it, 1, chart)
                    if (chart.size >= 3) {
                        updateFor(it, 2, chart)
                    }
                }
            }
        }
    }

    private fun updateFor(cube: Everything.Cube, index: Int, chart: List<ServerPlayer>) {
        val unknownName = Language.getDefault("player.unknownName")
        fun processLocation(l: Location) =
            l.add(Vector(0, 1, 0)).center.apply {
                yaw = cube.savedData["yaw"].asFloat
                pitch = 0F
            }

        val reg = CitizensAPI.getNPCRegistry()
        when (index) {
            0 -> {
                reg.createNPC(
                    EntityType.PLAYER,
                    TextUtil.getColoredText(
                        t = chart[index].name ?: unknownName,
                        color = TextUtil.TextColor.GOLD,
                        bold = true
                    )
                ).apply {
                    spawn(processLocation((cube.from to cube.to).center()))
                }
            }
            1 -> {
                reg.createNPC(
                    EntityType.PLAYER,
                    TextUtil.getColoredText(
                        t = chart[index].name ?: unknownName,
                        color = TextUtil.TextColor.YELLOW
                    )
                ).apply {
                    spawn(processLocation(cube.from.clone()))
                }
            }
            else -> {
                reg.createNPC(
                    EntityType.PLAYER,
                    TextUtil.getColoredText(
                        t = chart[index].name ?: unknownName,
                        color = TextUtil.TextColor.GREEN
                    )
                ).apply {
                    spawn(processLocation(cube.to.clone()))
                }
            }
        }.apply {
            data()["temp"] = true
            data()["position"] = index
            data()["name"] = chart[index].name
        }
    }

    fun remove(it: Everything.Cube) {
        it.fill(it.from.world!!, Material.AIR)
        CitizensAPI.getNPCRegistry().toList().forEach { npc ->
            if (npc.data().has("temp") && it.contains(npc.storedLocation.clone().add(Vector(0, -1, 0)))) {
                npc.destroy()
            }
        }
    }

    fun cleanUp() {
        if (Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            CitizensAPI.getNPCRegistry().toList().forEach {
                if (it.data().has("temp")) {
                    it.destroy()
                }
            }
            timer.cancel()
        }
    }

    @EventHandler
    fun onNPCClick(event: NPCRightClickEvent) {
        if (event.npc.data().has("temp")) {
            val info = event.clicker.info()
            if (info != null) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
                    event.clicker.sendActionBar(info.getter()["ui.chart.booting"].toInfoMessage())
                    val ui = ChartUI(plugin, info, null)
                    Bukkit.getScheduler().callSyncMethod(plugin) {
                        ui.show(event.clicker)
                    }
                }
            }
            else
                event.clicker.error(Language.getDefault("player.error.unknown"))
        }
    }
}