package com.zhufu.opencraft

import com.zhufu.opencraft.inventory.BankInventory
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.NPCRightClickEvent
import net.citizensnpcs.api.event.NPCSpawnEvent
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*

object BankManager : Listener {
    private val bankers = arrayListOf<NPC>()
    private val banks = hashMapOf<String, Location>()
    fun isEmpty() = banks.isEmpty()

    fun bankNearest(location: Location): Location? {
        val values = banks.values.toList().filter { it.world == location.world }
        var nearest = values.firstOrNull() ?: return null
        var dis = nearest.distance(location)
        for (i in 1 until values.size) {
            val t = values[i]
            val d = t.distance(location)
            if (d < dis) {
                dis = d
                nearest = t
            }
        }
        return nearest
    }

    private lateinit var file: File
    private lateinit var config: YamlConfiguration
    private lateinit var mPlugin: Plugin
    fun init(plugin: Plugin) {
        mPlugin = plugin
        this.file = File(plugin.dataFolder, "bank.yml")
        if (!file.exists()) {
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            file.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(file)
        config.getConfigurationSection("banks")?.getKeys(false)?.forEach { key ->
            config.getSerializable("banks.$key", Location::class.java)?.let {
                banks[key] = it
            }
        }
        config.getConfigurationSection("bankers")?.getKeys(false)?.forEach { key ->
            config.getSerializable("bankers.$key", Location::class.java)?.let {
                createBanker(it)
            }
        }

        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun createBanker(location: Location) {
        bankers.add(
            with(CitizensAPI.getNPCRegistry()) {
                createNPC(EntityType.PLAYER, Language.getDefault("bank.bankerName").toInfoMessage()).apply {
                    spawn(location)
                    data()[NPC.PLAYER_SKIN_UUID_METADATA] = "Banker"
                }
            }
        )
    }

    fun createBank(location: Location, name: String) {
        banks[name] = location
    }

    fun removeBanker(near: Location): Boolean {
        bankers.forEach {
            if (it.storedLocation.distance(near) < 4) {
                it.destroy()
                bankers.remove(it)
                return true
            }
        }
        return false
    }

    fun removeBank(name: String): Boolean {
        return if (banks.containsKey(name)) {
            banks.remove(name)
            true
        } else {
            false
        }
    }

    fun forEach(l: (Pair<String, Location>) -> Unit) {
        banks.forEach {
            l(it.toPair())
        }
    }

    fun onClose() {
        bankers.forEachIndexed { index, npc ->
            config.set("bankers.$index", npc.storedLocation)
            npc.destroy()
        }
        banks.forEach { (t, u) ->
            config.set("banks.$t", u)
        }
        config.save(file)
    }

    @EventHandler
    fun onNPCClick(event: NPCRightClickEvent) {
        if (bankers.contains(event.npc)) {
            val info = event.clicker.info()
            if (info == null) {
                event.clicker.error(Language.getDefault("player.error.unknown"))
            } else {
                BankInventory(mPlugin, info).show()
            }
        }
    }
}