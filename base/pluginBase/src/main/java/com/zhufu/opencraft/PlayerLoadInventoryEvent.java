package com.zhufu.opencraft;

import com.zhufu.opencraft.DualInventory.InventoryInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerLoadInventoryEvent extends PlayerEvent {
    private static HandlerList handlerList = new HandlerList();
    private InventoryInfo oldOne;
    private InventoryInfo newOne;
    public PlayerLoadInventoryEvent(Player who, InventoryInfo oldOne, InventoryInfo newOne) {
        super(who);
        this.oldOne = oldOne;
        this.newOne = newOne;
    }

    public InventoryInfo getOldOne(){
        return oldOne;
    }

    public InventoryInfo getNewOne() {
        return newOne;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
    public static HandlerList getHandlerList(){
        return handlerList;
    }
}
