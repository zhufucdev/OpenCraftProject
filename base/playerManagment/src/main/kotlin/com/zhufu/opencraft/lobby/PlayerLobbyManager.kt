package com.zhufu.opencraft.lobby

import com.zhufu.opencraft.*
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.data.OfflineInfo
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.nio.file.Paths

object PlayerLobbyManager : Listener {
    private val mList = arrayListOf<PlayerLobby>()
    private val configFile = Paths.get("plugins", "lobbies", "config.yml").toFile()
    lateinit var boundary: Pair<Vector, Vector>
    lateinit var bedLocation: Vector
    fun init(plugin: Plugin) {
        val config = YamlConfiguration.loadConfiguration(configFile)
        val first = config.getVector("boundary.first")
        val last = config.getVector("boundary.last")
        if (first == null || last == null)
            Bukkit.getLogger().warning("[PlayerLobby] Boundary is not defined. Right click on a barrier to define.")
        else
            boundary = first to last

        val bed = config.getVector("bed")
        if (bed == null)
            Bukkit.getLogger().warning("[PlayerLobby] Default spawnpoint is not defined. Enter a bed to define.")
        else
            bedLocation = bed

        Bukkit.getPluginManager().registerEvents(this, plugin)

        OfflineInfo.forEach {
            mList.add(PlayerLobby(it))
        }
    }

    private var first: Vector? = null
    private var last: Vector? = null
    private var lastClick = 0L
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.player.world != Base.spawnWorld || !event.player.isOp) return
        if (System.currentTimeMillis() - lastClick <= 300) return
        lastClick = System.currentTimeMillis()
        val target = event.clickedBlock
        if (event.action == Action.RIGHT_CLICK_BLOCK && !event.hasItem() && target?.type == Material.BARRIER) {
            val getter = event.player.getter()
            if (first == null) {
                first = target.location.toVector()
                event.player.tip(getter["lobby.op.firstSet"])
            } else {
                last = target.location.toVector()
                boundary = first!!.clone() to last!!.clone()
                event.player.success(getter["lobby.op.lastSet", boundary.first.toString(), boundary.second.toString()])

                first = null
                last = null
            }
        }
    }

    @EventHandler
    fun onPlayerSleep(event: PlayerBedEnterEvent) {
        if (event.player.world != Base.spawnWorld || !event.player.isOp) return
        bedLocation = event.bed.location.toVector()
        val getter = event.player.getter()
        event.player.success(getter["lobby.spawnpointSet"])
    }

    fun saveAll() {
        if (::boundary.isInitialized)
            YamlConfiguration().apply {
                set("boundary.first", boundary.first)
                set("boundary.last", boundary.second)
                if (::bedLocation.isInitialized)
                    set("bed", bedLocation)
                save(configFile)
            }

        mList.forEach {
            it.save()
        }
    }

    operator fun get(owner: OfflineInfo): PlayerLobby {
        val index = mList.indexOfFirst { it.owner == owner }
        if (index != -1)
            return mList[index]
        val r = PlayerLobby(owner)
        mList.add(r)
        return r
    }

    fun list() = mList.toList()
    val targetMap = HashMap<Player, PlayerLobby>()
    fun targetOf(player: Player) = targetMap[player]
    fun isTargetOf(player: Player) = targetMap[player]?.contains(player.location) == true
    fun isInOwnLobby(info: Info) = get(info).contains(info.player.location)
}