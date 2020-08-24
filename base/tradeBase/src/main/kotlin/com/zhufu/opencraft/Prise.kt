package com.zhufu.opencraft

import com.zhufu.opencraft.special_item.CurrencyItem
import com.zhufu.opencraft.special_item.dynamic.SpecialItem
import com.zhufu.opencraft.special_item.static.WrappedItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType

abstract class Prise<T> : Comparable<Prise<*>> {
    override fun compareTo(other: Prise<*>): Int = (toGeneralPrise().value - other.toGeneralPrise().value).toInt()

    /**
     * Remove the items of the amount of currency of this object from the given [player]'s inventory.
     * @return true if this removal is successful.
     */
    abstract fun cost(player: Player): CostResult

    /**
     * Remove the amount of currency of this object from the given [info]'s deposit.
     */
    abstract fun cost(info: ServerPlayer): CostResult

    /**
     * Return a new [Prise] whose value is [x] times of this.
     */
    abstract operator fun times(x: Int): Prise<T>
    abstract operator fun div(x: Int): Prise<T>
    abstract operator fun div(other: Prise<*>): Int

    abstract operator fun rem(other: Prise<*>): Prise<T>

    abstract fun isZero(): Boolean

    /**
     * Return a new [Prise] whose value is worth [other] more than this.
     */
    abstract operator fun plus(other: Prise<*>): Prise<T>
    operator fun minus(other: Prise<*>): Prise<T> = plus(other * -1)

    /**
     * Return a [GeneralPrise] instance whose value is the same as this.
     */
    abstract fun toGeneralPrise(): GeneralPrise

    /**
     * Return a human-readable [String] representing this [Prise].
     */
    abstract fun toString(getter: Language.LangGetter): String

    /**
     * Generate stacks of items equal to this.
     */
    abstract fun generateItem(owner: Player): List<ItemStack>

    interface CostResult {
        val successful: Boolean
        fun undo()
    }

    companion object {
        val costFailure get() = object : CostResult {
            override val successful: Boolean
                get() = false

            override fun undo() {
            }
        }

        fun of(item: ItemStack, owner: Player? = null): Prise<*> {
            val si = WrappedItem[item, owner]
            return if (si is CurrencyItem<*>) {
                si.value
            } else {
                UndefinedPrise.of(item.type) * item.amount
            }
        }

        fun evaluate(item: ItemStack, owner: Player? = null): Prise<*> {
            val si = WrappedItem[item, owner]
            if (si is CurrencyItem<*>) {
                return si.value
            } else {
                fun l1(effect: PotionEffectType): Long = when (effect) {
                    PotionEffectType.HEAL, PotionEffectType.INCREASE_DAMAGE, PotionEffectType.HARM -> 80
                    PotionEffectType.REGENERATION, PotionEffectType.SPEED, PotionEffectType.DAMAGE_RESISTANCE -> 60
                    PotionEffectType.POISON -> 65
                    PotionEffectType.WITHER -> 75
                    PotionEffectType.SLOW -> 50
                    else -> 0
                }

                fun l2(effect: PotionEffectType): Long = when (effect) {
                    PotionEffectType.HEAL, PotionEffectType.INCREASE_DAMAGE, PotionEffectType.HARM,
                    PotionEffectType.POISON, PotionEffectType.WITHER -> 2
                    PotionEffectType.HEAL, PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.SPEED,
                    PotionEffectType.SLOW -> 1
                    else -> 0
                }
                return if (item.type != Material.POTION && item.type != Material.TIPPED_ARROW)
                    GeneralPrise(
                        when (item.type) {
                            Material.BIRCH_PLANKS, Material.ACACIA_PLANKS, Material.CRIMSON_PLANKS,
                            Material.DARK_OAK_PLANKS, Material.JUNGLE_PLANKS, Material.OAK_PLANKS,
                            Material.SPRUCE_PLANKS, Material.WARPED_PLANKS -> 2
                            Material.IRON_INGOT -> 100
                            Material.LEATHER -> 5
                            Material.NETHERITE_INGOT -> 5000
                            Material.COBBLESTONE -> 2
                            Material.REDSTONE_TORCH, Material.REDSTONE -> 4
                            Material.REPEATER, Material.COMPARATOR, Material.REDSTONE_LAMP, Material.DAYLIGHT_DETECTOR -> 10
                            Material.LEVER -> 3
                            Material.STONE_BUTTON -> 2
                            Material.PISTON -> 20
                            Material.STICKY_PISTON -> 25
                            Material.IRON_PICKAXE -> 400
                            Material.IRON_AXE -> 500
                            Material.IRON_HOE -> 300
                            Material.IRON_SHOVEL -> 200
                            Material.ARROW -> 20
                            Material.SPECTRAL_ARROW -> 22
                            else -> return UndefinedPrise.of(item.type) * item.amount
                        } * item.amount.toLong()
                    )
                else {
                    val meta = item.itemMeta as PotionMeta
                    var sum = 0L
                    if (meta.customEffects.all { it.amplifier > 1 }) {
                        meta.customEffects.forEach {
                            sum += l2(it.type)
                        }
                        sum.toInt().toUP()
                    } else {
                        meta.customEffects.forEach {
                            sum += if (it.amplifier > 1) l2(it.type).toGP().toGeneralPrise().value else l1(it.type)
                        }
                        sum.toGP()
                    }
                }
            }
        }

    }
}