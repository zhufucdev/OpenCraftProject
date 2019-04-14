package com.zhufu.opencraft.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PeriodEndEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();

    private int which;
    private String cause = "";
    public PeriodEndEvent(int which, String cause){
        this.which = which;
        this.cause = cause;
    }

    public int getWhich() {
        return which;
    }

    public String getCause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList(){
        return handlerList;
    }
}
