package bukkit

import com.zhufu.opencraft.GroovySpecialItemAdapter
import com.zhufu.opencraft.special_item.SpecialItemAdapter
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import ss.Logger

class ExtendedItemConstructor {
    private String name, langName
    private Material material
    private Closure make, deserialize, serialize, tick, isItem, isConfig,
                    onRightClicked, onLeftClicked, onInventoryTouch

    /**
     * (*)Computer readable name of the item.
     * @param name The name.
     */
    void name(@NotNull String name) {
        this.name = name
    }

    /**
     * [A]Human readable name of the item.
     * @param langName The language code name.
     */
    void langName(@NotNull String langName) {
        this.langName = langName
    }

    /**
     * (*)Minecraft type of the item.
     * @param material The type.
     */
    void type(@NotNull Material material) {
        this.material = material
    }

    /**
     * [A]Initialize the item. Called at the first time the item is shown to player.
     * @param closure Actions to initialize the item.
     */
    void make(@NotNull
              @ClosureParams(value = FromString.class, options = [
                      "org.bukkit.inventory.ItemStack",
                      "com.zhufu.opencraft.Language.LangGetter"
              ])
                      Closure closure) {
        this.make = closure
    }
    /**
     *
     * @param closure
     */
    void deserialize(@NotNull Closure closure) {
        this.deserialize = closure
    }

    void serialize(@NotNull Closure closure) {
        this.serialize = closure
    }

    /**
     * Called every game tick (0.05s) only when the item is taken by a player inventory.
     * @param closure Action to do in main thread. DON'T run heavy tasks, or game will be laggy.
     */
    void tick(@NotNull
              @ClosureParams(value = FromString.class, options = [
                      "org.bukkit.inventory.ItemStack",
                      "com.zhufu.opencraft.PlayerModifier",
                      "org.bukkit.configuration.ConfigurationSection",
                      "org.bukkit.scoreboard.Objective",
                      "int"
              ])
                      Closure closure) {
        this.tick = closure
    }

    void isItem(@NotNull
                @ClosureParams(value = FromString.class, options = "org.bukkit.inventory.ItemStack")
                        Closure<Boolean> closure) {
        this.isItem = closure
    }

    void isConfig(@NotNull
                  @ClosureParams(value = FromString.class, options = "org.bukkit.configuration.ConfigurationSection")
                          Closure<Boolean> closure) {
        this.isConfig = closure
    }

    void onRightClicked(
            @ClosureParams(value = FromString.class, options = [
                    "org.bukkit.inventory.ItemStack",
                    "org.bukkit.entity.Player"
            ])
                    Closure closure) {
        this.onRightClicked = closure
    }

    void onLeftClicked(@NotNull
                       @ClosureParams(value = FromString.class, options = [
                               "org.bukkit.inventory.ItemStack",
                               "org.bukkit.entity.Player"
                       ])
                               Closure closure) {
        this.onLeftClicked = closure
    }

    void onInventoryTouch(
            @NotNull
            @ClosureParams(value = FromString.class, options = "org.bukkit.event.inventory.InventoryClickEvent")
                    Closure closure
    ) {
        this.onInventoryTouch = closure
    }

    private mListener = new Listener() {}
    /**
     * Start external listeners in order for the settings to be applied.
     */
    void startListening() {
        stopListening()
        // Hand click
        def onHandClick = { List<Action> actions, Closure e ->
            return {
                if (getAdapter().isThis(item)
                        && actions.contains(action)) {
                    final adapted = getAdapter().getAdaptItem(item, player)
                    adapted.inventoryPosition = player.inventory.getHeldItemSlot()
                    final old = adapted.itemMeta.clone()
                    e.call(adapted, player)
                    updateForPlayer(adapted, old, player)
                }
            }
        }

        if (onLeftClicked != null) {
            Server.listenEvent(PlayerInteractEvent.class, mListener, EventPriority.NORMAL,
                    onHandClick([Action.LEFT_CLICK_BLOCK, Action.LEFT_CLICK_AIR], onLeftClicked))
        }
        if (onRightClicked != null) {
            Server.listenEvent(PlayerInteractEvent.class, mListener, EventPriority.NORMAL,
                    onHandClick([Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR], onRightClicked))
        }
        // Inventory Click
        if (onInventoryTouch != null) {
            Server.listenEvent(InventoryClickEvent.class, mListener, EventPriority.NORMAL) {
                if (adapter.isThis(getCurrentItem())) this.with(onInventoryTouch)
            }
        }
    }

    void stopListening() {
        HandlerList.unregisterAll(mListener)
    }

    private SpecialItemAdapter mAdapter
    /**
     * @return An proxy adapter for Kotlin use.
     */
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
    /**
     * Upload the AdapterItem to its owner player if the itemMeta has changed.
     * @param item
     * @param oldMeta
     * @param player
     */
    private static void updateForPlayer(SpecialItemAdapter.AdapterItem item, ItemMeta oldMeta, Player player) {
        if (!item.hasItemMeta() || item.itemMeta != oldMeta)
            player.inventory.setItem(item.inventoryPosition, item)
    }

    /**
     * Apply changes of itemMeta to the given ItemStack.
     * @param itemStack
     * @param closure
     */
    static void updateMeta(
            @NotNull
            @DelegatesTo.Target("self") ItemStack itemStack,
            @NotNull
            @DelegatesTo(value = ItemStack.class, target = "self") Closure closure) {
        def meta = itemStack.itemMeta
        meta.with(closure)
        itemStack.itemMeta = meta
    }
}
