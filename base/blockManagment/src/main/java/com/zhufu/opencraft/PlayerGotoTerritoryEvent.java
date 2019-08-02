package com.zhufu.opencraft;

import com.zhufu.opencraft.BlockLockManager;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerGotoTerritoryEvent extends PlayerEvent {
    private static HandlerList handlerList = new HandlerList();
    private BlockLockManager.BlockInfo info;
    public PlayerGotoTerritoryEvent(Player who, BlockLockManager.BlockInfo info) {
        super(who);
        this.info = info;
    }

    public BlockLockManager.BlockInfo getInfo() {
        return info;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList(){
        return handlerList;
    }
}
