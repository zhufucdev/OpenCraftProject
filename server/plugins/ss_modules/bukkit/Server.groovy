package bukkit

import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.events.SSReloadEvent
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventException
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor

class Server {
    private static Listener mListener = new Listener() {}

    static <T extends Event> Listener listenEvent(Class<T> clazz, Closure executor) {
        return listenEvent(clazz, EventPriority.NORMAL, executor)
    }

    static <T extends Event> Listener listenEvent(Class<T> clazz, EventPriority priority, Closure executor) {
        listenEvent(clazz, mListener, priority, executor)
        return mListener
    };

    static <T extends Event> Listener listenEvent(Class<T> clazz, Listener listener, EventPriority priority, Closure executor) {
        Bukkit.getPluginManager().registerEvent(clazz, listener, priority,
                new EventExecutor() {
                    @Override
                    void execute(Listener l, Event event) throws EventException {
                        event.with(executor)
                    }
                }, Scripting.plugin)
        return listener
    };

    static {
        listenEvent(SSReloadEvent.class) {
            HandlerList.unregisterAll(mListener)
        }
    }
}
