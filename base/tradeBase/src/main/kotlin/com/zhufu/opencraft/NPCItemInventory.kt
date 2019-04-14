package com.zhufu.opencraft

import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.NPCRightClickEvent
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.HumanEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.util.*
import kotlin.collections.ArrayList

abstract class NPCItemInventory(var baseLocation: Location, private val item: ItemStack, val plugin: JavaPlugin): Listener {
    companion object {
        var id = 314
        val npcList = ArrayList<NPC>()
    }
    val id: Int = ++Companion.id
    lateinit var clickableNPC: NPC
    abstract var inventory: Inventory
    abstract var inventoryName: String
    private var isOpened = false
    init {
        initNPC()
        Bukkit.getScheduler().runTask(plugin) { _ ->
            Bukkit.getPluginManager().registerEvents(this,plugin)
        }
    }

    private fun initNPC(){
        baseLocation = baseLocation.block.location.clone()

        //Clean the old one
        npcList.firstOrNull { it.id == id }?.destroy()

        clickableNPC = CitizensAPI.getNPCRegistry().createNPC(EntityType.ARMOR_STAND, UUID.randomUUID(), id,id.toString())
        clickableNPC.spawn(baseLocation.clone().add(Vector(0.5,0.0,0.5)))
        (clickableNPC.entity as ArmorStand)
                .apply {
                    setItemInHand(item)
                }
        npcList.add(clickableNPC)

    }

    open fun cancel(player: HumanEntity?){
        inventory.clear()
        player?.closeInventory()
        clickableNPC.destroy()
        HandlerList.unregisterAll(this)
    }

    @EventHandler
    fun onNPCClick(event: NPCRightClickEvent){
        if (event.npc == clickableNPC){
            if (BuilderListener.isInBuilderMode(event.clicker)){
                event.clicker.sendMessage(TextUtil.error("抱歉，但您不能在此时购买或更改物品"))
                return
            }
            if (isOpened){
                event.clicker.sendMessage(TextUtil.error("该物品已经被其他玩家占用"))
                return
            }
            onInventoryOpen(event.clicker)
            event.clicker.openInventory(inventory)
            isOpened = true
        }
    }

    @EventHandler
    fun onPlayerClickItem(event: InventoryClickEvent){
        if (event.inventory.name == inventory.name){
            event.isCancelled = onItemClick(event)
        }
    }

    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent){
        if (!event.isCancelled && event.block.location == baseLocation){
            event.isDropItems = false
            this.cancel(event.player)
        }
    }

    @EventHandler
    fun onPlayerCloseInventory(event: InventoryCloseEvent){
        if (validateInventory(event.inventory)){
            isOpened = false
            onInventoryClose(event.player)
        }
    }

    fun validateInventory(inventory: Inventory): Boolean = inventory.name == this.inventoryName && inventory.location == null

    abstract fun onItemClick(event: InventoryClickEvent): Boolean
    open fun onInventoryOpen(player: HumanEntity){}
    open fun onInventoryClose(player: HumanEntity){}
}