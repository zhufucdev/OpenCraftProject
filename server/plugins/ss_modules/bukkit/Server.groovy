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

    /**
     * Start a listening action via the public event listener.
     * @param clazz Type of bukkit event to listen to.
     * @param executor Called when the event is triggered.
     * @return The public listener.
     */
    static <T extends Event> Listener listenEvent(
            @DelegatesTo.Target Class<T> clazz,
            @DelegatesTo Closure executor) {
        return listenEvent(clazz, EventPriority.NORMAL, executor)
    }

    /**
     * Start a listening action via the public event listener.
     * @param priority Determines the order where is executor is called.
     * @param clazz Type of bukkit event to listen.
     * @param executor Called when the event is triggered.
     * @return The public listener.
     */
    static <T extends Event> Listener listenEvent(Class<T> clazz, EventPriority priority, Closure executor) {
        listenEvent(clazz, mListener, priority, executor)
        return mListener
    };

    /**
     * Start a listening action via a given event listener in order for which can be unregistered separately.
     * @param listener Listener to listen the event.
     * @param priority Determines the order where is executor is called.
     * @param clazz Type of bukkit event to listen.
     * @param executor Called when the event is triggered.
     * @return The given listener.
     */
    static <T extends Event> Listener listenEvent(
            @DelegatesTo.Target Class<T> clazz, Listener listener, EventPriority priority,
            @DelegatesTo Closure executor) {
        Bukkit.getPluginManager().registerEvent(clazz, listener, priority,
                new EventExecutor() {
                    @Override
                    void execute(Listener l, Event event) throws EventException {
                        event.with(executor)
                    }
                }, Scripting.plugin)
        return listener
    };

    /**
     * Call the ticker every game tick(0.05s)
     * @param ticker The ticker.
     */
    static void eachTick(Closure ticker) {
        Bukkit.getScheduler().runTaskTimer(Scripting.plugin, ticker, 0, 1)
    }

    static {
        listenEvent(SSReloadEvent.class) {
            HandlerList.unregisterAll(mListener)
        }
    }
}
