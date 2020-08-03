package bukkit

import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent

import java.lang.reflect.Method

class ExtendedBlockConstructor implements Constructor<ExtendedBlockConstructor> {
    private String name, itemName

    String getName() { return name }

    String getItemName() { return itemName }

    private Class<ExtendedBlock> clazz

    Class getType() { return clazz }

    void name(String name) {
        this.name = name
    }

    void item(String name) {
        this.itemName = name
    }

    void existsAs(Class<ExtendedBlock> clazz) {
        this.clazz = clazz
    }

    private Listener mListener = new Listener() {}

    @Override
    void apply() {
        if (clazz == null) return
        ExtendedBlock.init()

        def itemConstructor = Content.definedItems.find { it.name == name }
        if (itemConstructor == null) return
        if (itemConstructor.material.isBlock()) {
            Server.listenEvent(BlockPlaceEvent.class, mListener, EventPriority.NORMAL) {
                if (itemConstructor.adapter.isThis(itemInHand))
                    ExtendedBlock.place(block.location, clazz.getConstructor().newInstance(), player)
            }
        } else {
            Server.listenEvent(PlayerInteractEvent.class, mListener, EventPriority.NORMAL) {
                if (action == Action.RIGHT_CLICK_BLOCK && item != null && itemConstructor.adapter.isThis(item)) {
                    ExtendedBlock.place(clickedBlock.getRelative(blockFace).location, clazz.getConstructor().newInstance(), player)
                    item.amount--
                }
            }
        }
    }

    @Override
    void unapply() {
        HandlerList.unregisterAll(mListener)
    }

    @Override
    void merge(ExtendedBlockConstructor other) {
        other.properties.forEach { String n, v ->
            if (v != null && v !instanceof Method && n != 'class')
                this[n] = v
        }
    }
}
