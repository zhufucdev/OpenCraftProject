package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.npc.NPCHelper
import com.zhufu.opencraft.special_item.dynamic.BindItem
import net.citizensnpcs.api.CitizensAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.meta.ItemMeta
import java.util.*
import java.util.function.Consumer

class Control: BindItem() {
    override val material: Material
        get() = Material.COMPASS

    override fun onCreate(owner: Player, vararg args: Any) {
        super.onCreate(owner, *args)
        itemLocation.itemStack.updateItemMeta<ItemMeta> {
            val getter = owner.getter()
            setDisplayName(getter["rpg.control.name"].toInfoMessage())
            lore = TextUtil.formatLore(getter["rpg.control.subtitle"].toTipMessage())
        }
    }

    override fun onPostInit() {
        super.onPostInit()
        listenOther(EntityDamageByEntityEvent::class.java, Consumer {
            if (it.damager.uniqueId != owner.uuid || !it.entity.hasMetadata("NPC")) return@Consumer
            if (getSIID((it.damager as HumanEntity).inventory.itemInMainHand) != SIID) return@Consumer
            val npc = CitizensAPI.getNamedNPCRegistry("temp").getNPC(it.entity)
            if (npc.data().get<UUID?>("owner") != owner.uuid) return@Consumer

            it.isCancelled = true
            val control = NPCHelper.control(npc, owner.onlinePlayerInfo!!, Base.pluginCore)
            Bukkit.getScheduler().runTask(Base.pluginCore) { _ ->
                owner.onlinePlayerInfo?.player?.apply {
                    sendTitle(null, getter()["rpg.control.exit"].toTipMessage(), 8, 20, 10)
                }
                control.start()
            }
        })
    }
}