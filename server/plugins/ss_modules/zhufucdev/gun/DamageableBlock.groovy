package zhufucdev.gun

import bukkit.ExtendedBlock
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DamageableBlock extends ExtendedBlock {
    private double mDurability = 20000

    double getDurability() {
        return mDurability
    }

    @Override
    void onCreate(@Nullable ConfigurationSection savedData, @Nullable Player creator) {
        super.onCreate(savedData, creator)
        if (savedData == null) {
            if (type.isSolid()) mDurability += 5000
            if (!type.isBurnable()) mDurability += 2000
        } else {
            mDurability = savedData.getDouble("durability")
        }
    }

    @Override
    void saveStatus(@NotNull ConfigurationSection configuration) {
        configuration.set("durability", mDurability)
    }
    /**
     * Damages this block.
     * @param strength The measure of damage(kilogram block per square second block, or J).
     */
    void damage(double strength) {
        mDurability -= strength
        def l = location.toCenterLocation()
        def t = type
        if (mDurability <= 0) {
            l.world.spawnParticle(Particle.BLOCK_CRACK, l, 100, l.block.blockData)
            type = Material.AIR
            destroy(this)
        }
        l.world.spawnParticle(Particle.ITEM_CRACK, l, 60, 1.2, 1.2, 1.2, new ItemStack(t))
    }
}
