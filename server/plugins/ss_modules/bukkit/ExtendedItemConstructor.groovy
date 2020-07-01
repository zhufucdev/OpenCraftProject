package bukkit

import com.zhufu.opencraft.GroovySpecialItemAdapter
import com.zhufu.opencraft.special_item.SpecialItemAdapter
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import opencraft.Lang
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.Vector
import ss.Logger

/**
 * <p>Defines an extended item.</p>
 * <p>To achieve this, you need to declare some parameters and methods, which have relationships, or conflicts, with
 * each other. The mark before each annotation speaks.</p>
 * <p><strong>(*)</strong> means necessity. This parameter or method must be declared.</p>
 * <p><strong>[ ]</strong> means non-necessity. This parameter can be specified or simply ignored.</p>
 * <p><strong>[<span style="color: blue">X</span>]</strong> means selection. This parameter or any other one marked with
 * <span style="color: blue">X</span> is supposed to be taken.</p>
 * <p><strong>(<span style="color: blue">X</span>)</strong> means conflict. Only one of the
 * <span style="color: blue">X</span>-marked parameter should be taken.<p>
 */
class ExtendedItemConstructor {
    private String name, langName
    private Material material
    private Closure make, deserialize, serialize, tick, isItem, isConfig,
                    onRightClicked, onLeftClicked, onInventoryTouch
    private ArrayList<PatternedCondition> recipes = new ArrayList<>()

    /**
     * (*)Computer readable name of the item.
     * @param name The name.
     */
    void name(@NotNull String name) {
        this.name = name
    }

    /**
     * (A)[B]Human readable name of the item.
     * @param langName The language code name.
     * @see ExtendedItemConstructor
     */
    void langName(@NotNull String langName) {
        this.langName = langName
    }

    /**
     * (*)Minecraft type of the item.
     * @param material The type.
     * @see ExtendedItemConstructor
     */
    void type(@NotNull Material material) {
        this.material = material
    }

    /**
     * (A)Initialize the item. Called at the first time the item is shown to player.
     * @param closure Actions to initialize the item.
     * @see ExtendedItemConstructor
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
     * [ ]Load data stored as YAML. Called each time its inventory is applied to a player.
     * @param closure
     *
     * @see ExtendedItemConstructor
     */
    void deserialize(@NotNull Closure closure) {
        this.deserialize = closure
    }

    /**
     * [ ]Store data as YAML. Called each time its inventory is saved.
     * @param closure
     * @see ExtendedItemConstructor
     */
    void serialize(@NotNull Closure closure) {
        this.serialize = closure
    }

    /**
     * Called every game tick (0.05s) only when the item is taken by a player inventory.
     * @param closure Action to do in main thread. DON'T run heavy tasks, or game will be laggy.
     * @see ExtendedItemConstructor
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

    /**
     * [B]Determines whether an ItemStack is of this type of adapter.
     * @param closure Return true if the item satisfies all the conditions.
     * @see ExtendedItemConstructor
     */
    void isItem(@NotNull
                @ClosureParams(value = FromString.class, options = "org.bukkit.inventory.ItemStack")
                        Closure<Boolean> closure) {
        this.isItem = closure
    }

    /**
     * [ ]Determines whether an YAML configuration is of this type of adapter.
     * @see ExtendedItemConstructor* @param closure Return true if the configuration satisfies all the conditions.
     */
    void isConfig(@NotNull
                  @ClosureParams(value = FromString.class, options = "org.bukkit.configuration.ConfigurationSection")
                          Closure<Boolean> closure) {
        this.isConfig = closure
    }

    void recipe(@NotNull
                @DelegatesTo(value = RecipeConstructor.class)
                        Closure closure) {
        def r = new RecipeConstructor(this)
        r.with(closure)
        recipes.add(r)
    }

    void disorderedRecipe(@NotNull
                          @DelegatesTo(value = DisorderedRecipeConstructor.class)
                                  Closure closure) {
        def r = new DisorderedRecipeConstructor(this)
        r.with(closure)
        recipes.add(r)
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
        // Recipe
        recipes.forEach { it.apply() }
        // Hand click
        def onHandClick = { List<Action> actions, Closure e ->
            return {
                if (item == null) return
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
        recipes.forEach { it.unapply() }
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

    trait PatternedCondition {
        Map<Character, Closure<Boolean>> conditions = new HashMap<>()

        void where(String letter,
                   @DelegatesTo(value = ItemStack.class)
                           Closure<Boolean> satisfies) {
            conditions[letter.charAt(0)] = satisfies
        }

        abstract void pattern(@NotNull Closure closure)

        abstract void apply()

        abstract void unapply()
    }

    class RecipeConstructor implements PatternedCondition {
        private class Pattern {
            private String first, second, third

            private void checkPattern(String s) {
                if (s.size() >= 4) throw new IllegalArgumentException("A pattern must't contain more than 3 chars.")
            }

            void firstLine(String pattern) {
                checkPattern(pattern)
                first = pattern
            }

            void secondLine(String pattern) {
                checkPattern(pattern)
                second = pattern
            }

            void thirdLine(String pattern) {
                checkPattern(pattern)
                third = pattern
            }
        }
        private ExtendedItemConstructor item

        RecipeConstructor(ExtendedItemConstructor item) {
            this.item = item
        }

        private Pattern pattern

        @Override
        void pattern(@NotNull
                     @DelegatesTo(value = Pattern.class) Closure closure) {
            def p = new Pattern()
            p.with(closure)
            this.pattern = p
        }

        private Listener mListener = new Listener() {}

        @Override
        void apply() {
            if (pattern == null) throw new IllegalArgumentException("Pattern mustn't be null.")
            Server.listenEvent(PrepareItemCraftEvent.class, mListener, EventPriority.NORMAL) {
                if (isRepair() || pattern == null) return

                def expect = new ArrayList<String>()
                if (pattern.first != null) {
                    expect.add(pattern.first)
                }
                if (pattern.second != null) {
                    expect.add(pattern.second)
                }
                if (pattern.third != null) {
                    expect.add(pattern.third)
                }
                if (expect.isEmpty()) throw new IllegalArgumentException("Pattern is empty.")

                // Inventory validation
                int firstItem = -1
                for (int i = 0; i < inventory.size; i++) {
                    if (inventory.getItem(i) != null) {
                        firstItem = i - 1
                        break
                    }
                }
                if (firstItem == -1) return

                // Compare with recipe
                boolean satisfies = true
                final tableSize = inventory.type == InventoryType.WORKBENCH ? 3 : 2
                def getY = { int index -> Math.floor(index / tableSize).toInteger() },
                    getX = { int index -> index - getY(index) * tableSize }
                Vector delta
                for (int y = 0; y < expect.size(); y++) {
                    def e = expect[y]
                    if (e.length() > tableSize) {
                        satisfies = false // If the table is smaller than expected
                        break
                    }
                    if (delta == null)
                        for (int x = 0; x < e.size(); x++) {
                            if (conditions.containsKey(e.charAt(x))) {
                                delta = new Vector(getX(firstItem) - x, getY(firstItem) - y, 0)
                                break
                            }
                        }
                }
                if (delta == null) satisfies = false
                if (satisfies) {
                    for (int i = 0; i < tableSize * tableSize; i++) {
                        def item = inventory.getItem(i + 1)
                        int x = getX(i) - delta.blockX, y = getY(i) - delta.blockY
                        def l = (y < 0 || y >= expect.size()) ? null
                                : ((x < 0 || x >= expect[y].size()) ? null : expect[y].charAt(x))
                        def satisfied =
                                (item == null && (l == null || !conditions.containsKey(l) || new ItemStack(Material.AIR).with(conditions[l])))
                                        || (item != null && l != null && conditions.containsKey(l) && item.with(conditions[l]))
                        if (!satisfied) {
                            satisfies = false
                            break
                        }
                    }

                    if (satisfies) {
                        inventory.result = new SpecialItemAdapter.AdapterItem(item.adapter, Lang.getter(view.player))
                    }
                }
            }
        }

        @Override
        void unapply() {
            HandlerList.unregisterAll(mListener)
        }
    }

    class DisorderedRecipeConstructor implements PatternedCondition {
        private class Pattern {
            private Map<Character, Integer> considerations = new HashMap<>()

            void involve(String letter, int counting = 1) {
                considerations[letter.charAt(0)] = counting
            }
        }
        private ExtendedItemConstructor item

        DisorderedRecipeConstructor(ExtendedItemConstructor item) {
            this.item = item
        }

        private Pattern pattern

        @Override
        void pattern(@NotNull
                     @DelegatesTo(value = Pattern.class) Closure closure) {
            def p = new Pattern()
            p.with(closure)
            pattern = p
        }

        private Listener mListener = new Listener() {}

        @Override
        void apply() {
            pattern.considerations.keySet().forEach {
                if (!conditions.containsKey(it)) throw new IllegalArgumentException("Pattern $it doesn't have any condition " +
                        "declared.")
            }
            Server.listenEvent(PrepareItemCraftEvent.class, mListener, EventPriority.NORMAL) {
                final tableSize = inventory.type == InventoryType.WORKBENCH ? 3 : 2
                final counted = new HashMap<Character, Integer>()
                for (int i = 0; i < tableSize * tableSize; i++) {
                    def item = inventory.getItem(i + 1)
                    if (item == null) item = new ItemStack(Material.AIR)
                    for (char l : conditions.keySet()) {
                        if (item.with(conditions[l])) {
                            if (!counted.containsKey(l)) {
                                counted[l] = 0
                            }
                            counted[l] = counted[l] + 1
                            break
                        }
                    }
                }
                if (counted == pattern.considerations) {
                    inventory.result = new SpecialItemAdapter.AdapterItem(adapter, Lang.getter(view.player))
                }
            }
        }

        @Override
        void unapply() {
            HandlerList.unregisterAll(mListener)
        }
    }
}
