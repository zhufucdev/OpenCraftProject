package bukkit

import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.ServerReloadEvent
import com.zhufu.opencraft.events.SSReloadEvent
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventException
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask

class Server {
    private static Listener mListener = new Listener() {}

    /**
     * Start a listening action via the public event listener.
     * @param clazz Type of bukkit event to listen.
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

    private static ArrayList<BukkitTask> tasks = new ArrayList<>()
    /**
     * Call the ticker every game tick(0.05s)
     * @param ticker The ticker.
     */
    static BukkitTask eachTick(Closure ticker) {
        return schedule.runTaskTimer(Scripting.plugin, ticker, 0, 1).tap { tasks.add(it) }
    }

    static BukkitTask delay(long ticks, Closure action) {
        return schedule.runTaskLater(Scripting.plugin, action, ticks).tap { tasks.add(it) }
    }

    static BukkitScheduler getSchedule() {
        return Bukkit.getScheduler()
    }

    static {
        listenEvent(SSReloadEvent.class) {
            HandlerList.unregisterAll(mListener)
            tasks.forEach {
                if (!it.cancelled)
                    it.cancel()
            }
        }
        listenEvent(ServerReloadEvent.class) {
            tasks.removeAll { it.cancelled }
        }
    }
}
