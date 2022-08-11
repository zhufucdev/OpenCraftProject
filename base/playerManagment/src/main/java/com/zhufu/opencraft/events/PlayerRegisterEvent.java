package com.zhufu.opencraft.events;

import com.zhufu.opencraft.data.Info;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerRegisterEvent extends Event {
    private final static HandlerList handlers = new HandlerList();
    private Info info;

    public PlayerRegisterEvent(Info who) {
        info = who;
    }

    public Info getInfo() {
        return info;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList(){
        return handlers;
    }
}
