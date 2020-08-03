package bukkit

import com.zhufu.opencraft.events.SSReloadEvent
import com.zhufu.opencraft.special_item.base.SpecialItem
import groovyjarjarantlr4.v4.runtime.misc.Nullable

class Content {
    private static ArrayList<ExtendedItemConstructor> definedItems = new ArrayList<>()

    static List<ExtendedItemConstructor> getDefinedItems() {
        return definedItems.clone() as List
    }

    static @Nullable ExtendedItemConstructor getDefinedItem(String name) {
        return definedItems.find { it.name == name }
    }

    private static ArrayList<ExtendedBlockConstructor> definedBlocks = new ArrayList<>()

    static List<ExtendedBlockConstructor> getDefinedBlocks() {
        return definedBlocks.clone() as List
    }

    static @Nullable ExtendedBlockConstructor getDefinedBlock(String name) {
        return definedBlocks.find {it.name == name}
    }
    /**
     * Define an ExtendedItem.
     * @param closure Configuration with {@link ExtendedItemConstructor}
     * @see ExtendedItemConstructor
     */
    static ExtendedItemConstructor defineItem(@DelegatesTo(value = ExtendedItemConstructor.class) Closure closure) {
        def c = new ExtendedItemConstructor()
        c.with(closure)
        def existing = definedItems.find { it.name == c.name }
        if (existing != null) {
            existing.merge(c)
            existing.apply()
        } else {
            SpecialItem.register(c.itemType)
            definedItems.add(c)
            c.apply()
        }
        return c
    }

    static ExtendedBlockConstructor defineBlock(
            Class<ExtendedBlock> clazz,
            @DelegatesTo(value = ExtendedBlockConstructor.class) Closure closure) {
        return defineBlock(closure).with { existsAs(clazz) }
    }

    static ExtendedBlockConstructor defineBlock(@DelegatesTo(value = ExtendedBlockConstructor.class) Closure closure) {
        def c = new ExtendedBlockConstructor()
        c.with(closure)
        def existing = definedBlocks.find { it.name == c.name }
        if (existing != null) {
            existing.merge(c)
            existing.apply()
        } else {
            definedBlocks.add(c)
            c.apply()
        }
        return c
    }

    static {
        // Unregister listeners when reloading
        Server.listenEvent(SSReloadEvent.class) {
            SpecialItem.unregisterAll()
            // => ExtendedItem
            definedItems.forEach {
                it.unapply()
            }
            // => ExtendedBlock
            definedBlocks.forEach {
                it.unapply()
            }
        }
    }
}
