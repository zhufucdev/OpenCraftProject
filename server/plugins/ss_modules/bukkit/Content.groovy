package bukkit

import com.zhufu.opencraft.events.SSReloadEvent
import com.zhufu.opencraft.special_item.SpecialItem

class Content {
    private static ArrayList<ExtendedItemConstructor> definedItems = new ArrayList()
    /**
     * Define an ExtendedItem
     * @param closure Configuration with {ExtendedItemConstructor}
     */
    static void defineItem(@DelegatesTo(value = ExtendedItemConstructor.class) Closure closure) {
        def c = new ExtendedItemConstructor()
        c.with(closure)
        SpecialItem.registerAdapter(c.getAdapter())
        c.startListening()
        definedItems.add(c)
    }
    static {
        // Unregister listeners when reloading
        // => ExtendedItem
        Server.listenEvent(SSReloadEvent.class) {
            SpecialItem.unregisterAll()
            definedItems.forEach {
                it.stopListening()
            }
        }
    }
}
