package com.zhufu.opencraft.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerObserveEvent extends PlayerEvent {
    private static HandlerList handlerList = new HandlerList();
    private Player onObserver;
    public PlayerObserveEvent(Player who, Player observerOn) {
        super(who);
        onObserver = observerOn;
    }

    public Player getOnObserver() {
        return onObserver;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList(){
        return handlerList;
    }
}
