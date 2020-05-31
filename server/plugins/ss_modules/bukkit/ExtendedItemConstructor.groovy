package bukkit

import com.zhufu.opencraft.GroovySpecialItemAdapter
import com.zhufu.opencraft.special_item.SpecialItemAdapter
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class ExtendedItemConstructor {
    private String name, langName
    private Material material
    private Closure make, deserialize, serialize, tick, isItem, isConfig, onRightClicked, onLeftClicked

    void name(@NotNull String name) {
        this.name = name
    }

    void langName(@NotNull String langName) {
        this.langName = langName
    }

    void type(@NotNull Material material) {
        this.material = material
    }

    void make(@NotNull Closure closure) {
        this.make = closure
    }

    void deserialize(@NotNull Closure closure) {
        this.deserialize = closure
    }

    void serialize(@NotNull Closure closure) {
        this.serialize = closure
    }

    void tick(@NotNull Closure closure) {
        this.tick = closure
    }

    void isItem(@NotNull Closure<Boolean> closure) {
        this.isItem = closure
    }

    void isConfig(@NotNull Closure<Boolean> closure) {
        this.isConfig = closure
    }

    void onRightClicked(@NotNull Closure closure) {
        this.onRightClicked = closure
    }

    void onLeftClicked(@NotNull Closure closure) {
        this.onLeftClicked = closure
    }

    void updateMeta(@NotNull ItemStack itemStack, @NotNull Closure closure) {
        def meta = itemStack.itemMeta
        meta.with(closure)
        itemStack.itemMeta = meta
    }

    void startListening() {
        if (onRightClicked != null) {
            Server.listenEvent(PlayerInteractEvent.class) {
                if (getAdapter().isThis(item)
                        && action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    def adapted = getAdapter().getAdaptItem(item, player)
                    adapted.inventoryPosition = player.inventory.getHeldItemSlot()
                    def old = adapted.itemMeta.clone()
                    onRightClicked.call(adapted)
                    updateForPlayer(adapted, old, player)
                }
            }
        }

        if (onLeftClicked != null) {
            Server.listenEvent(PlayerInteractEvent.class) {
                if (getAdapter().isThis(item)
                        && action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    def adapted = getAdapter().getAdaptItem(item, player)
                    adapted.inventoryPosition = player.inventory.getHeldItemSlot()
                    def old = adapted.itemMeta.clone()
                    onLeftClicked.call(adapted)
                    updateForPlayer(adapted, old, player)
                }
            }
        }
    }

    private SpecialItemAdapter mAdapter

    synchronized SpecialItemAdapter getAdapter() {
        if (mAdapter != null)
            return mAdapter
        assert name != null
        assert langName != null || isItem != null
        mAdapter = new GroovySpecialItemAdapter(name, langName, material, make, deserialize, serialize, isItem, isConfig, tick)
        return mAdapter
    }

    /**
     * Utilities
     */
    private static void updateForPlayer(SpecialItemAdapter.AdapterItem item, ItemMeta oldMeta, Player player) {
        if (!item.hasItemMeta() || item.itemMeta != oldMeta)
            player.inventory.setItem(item.inventoryPosition, item)
    }
}
