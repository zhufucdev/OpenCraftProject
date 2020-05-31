package bukkit

import com.zhufu.opencraft.events.SSReloadEvent
import com.zhufu.opencraft.special_item.SpecialItem

class Content {
    private static ArrayList<ExtendedItemConstructor> definedItems = new ArrayList()
    static void defineItem(Closure closure) {
        def c = new ExtendedItemConstructor()
        c.with(closure)
        SpecialItem.registerAdapter(c.getAdapter())
        c.startListening()
        definedItems.add(c)
    }
    static {
        Server.listenEvent(SSReloadEvent.class) {
            SpecialItem.unregisterAll()
        }
    }
}
