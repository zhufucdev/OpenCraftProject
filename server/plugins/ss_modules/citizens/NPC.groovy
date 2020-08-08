package citizens

import bukkit.Server
import com.zhufu.opencraft.events.SSReloadEvent

class NPC {
    private static ArrayList<NPCConstructor> existing = new ArrayList<>()

    static NPCConstructor define(@DelegatesTo(value = NPCConstructor.class) Closure closure) {
        def constructor = new NPCConstructor()
        constructor.with(closure)
        constructor.apply()
        existing.add(constructor)

        return constructor
    }

    static {
        Server.listenEvent(SSReloadEvent.class, {
            existing.forEach { it.unapply() }
        })
    }
}
