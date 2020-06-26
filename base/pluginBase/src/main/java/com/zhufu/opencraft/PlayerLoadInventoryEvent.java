package com.zhufu.opencraft;

import com.zhufu.opencraft.DualInventory.InventoryInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerLoadInventoryEvent extends PlayerEvent {
    private static final HandlerList handlerList = new HandlerList();
    private final InventoryInfo oldOne, newOne;
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

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
    public static HandlerList getHandlerList(){
        return handlerList;
    }
}
