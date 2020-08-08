package bukkit

import com.zhufu.opencraft.special_item.base.SpecialItem
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import ss.Constructor
import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.Vector

/**
 * Defines an extended item.
 */
class ExtendedItemConstructor implements Constructor<ExtendedItemConstructor> {
    private String name, blockName
    private Class<SpecialItem> type
    private ArrayList<PatternedCondition> recipes = new ArrayList<>()

    /**
     * (*)Computer readable name of the item.
     * @param name The name.
     */
    void name(@NotNull String name) {
        this.name = name
    }

    String getName() {
        return name
    }

    void existsAs(Class<SpecialItem> clazz) {
        this.type = clazz
    }

    Class<SpecialItem> getItemType() {
        return type
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

    void block(@NotNull String name) {
        blockName = name
    }

    /**
     * Start external listeners in order for the settings to be applied.
     */
    @Override
    void apply() {
        unapply()
        // Extended Block
        Content.defineBlock {
            delegate.name(blockName)
            item(name)
        }
        // Recipe
        recipes.forEach { it.apply() }
    }

    @Override
    void unapply() {
        recipes.forEach { it.unapply() }
    }

    /**
     * Utilities
     */

    /**
     * Apply changes of itemMeta to the given ItemStack.
     * @param itemStack
     * @param closure
     */
    static void updateMeta(
            @NotNull ItemStack itemStack,
            @NotNull
            @DelegatesTo(value = ItemMeta.class) Closure closure) {
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
                        inventory.result = item.itemType.getConstructor().newInstance().itemStack
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
                    inventory.result = item.itemType.getConstructor().newInstance().itemStack
                }
            }
        }

        @Override
        void unapply() {
            HandlerList.unregisterAll(mListener)
        }
    }
}
