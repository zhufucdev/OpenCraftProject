package com.zhufu.opencraft

import com.zhufu.opencraft.rpg.Role.*
import com.zhufu.opencraft.special_item.base.SpecialItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object RPGUtil {
    fun sendEquipmentsTo(player: Player) {
        val info = player.info()
        val getter = getLangGetter(info)
        if (info == null) {
            player.error(getter["player.error.unknown"])
            return
        }

        when (info.role) {
            FIGHTER -> {
                val sword = SpecialItem.make("StoneSword", 1, player)!!
                val axe = SpecialItem.make("StoneAxe", 1, player)!!
                val shield = ItemStack(Material.SHIELD)
                player.inventory.addItem(sword.itemLocation.itemStack, axe.itemLocation.itemStack)
                player.inventory.setItem(EquipmentSlot.OFF_HAND, shield)
            }
            MAGICIAN -> {
                val wand = SpecialItem.make("Wand", 1, player)!!
                val book = SpecialItem.make("MagicBook", 1, player)!!
                player.inventory.addItem(wand.itemLocation.itemStack, book.itemLocation.itemStack)
                info.tag.set("mp", 40)
            }
            RANGER -> TODO()
            PRIEST -> TODO()
            SUMMONER -> {
                val spawner = SpecialItem.make("EggSpawner", 1, player)!!
                val control = SpecialItem.make("Control", 1, player)!!
                player.inventory.addItem(spawner.itemLocation.itemStack, control.itemLocation.itemStack)
            }
            else -> {}
        }
    }

    fun addBuff(it: Info) {
        when (it.role) {
            FIGHTER -> {
                when (it.level) {
                    1 -> it.player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.DAMAGE_RESISTANCE,
                            40,
                            1,
                            true,
                            false,
                            true
                        )
                    )
                    2 -> it.player.addPotionEffects(listOf(
                        PotionEffect(
                            PotionEffectType.DAMAGE_RESISTANCE,
                            40,
                            1,
                            true,
                            false,
                            true
                        ),
                        PotionEffect(
                            PotionEffectType.INCREASE_DAMAGE,
                            40,
                            1,
                            true,
                            false,
                            true
                        )
                    ))
                    3 -> it.player.addPotionEffects(listOf(
                        PotionEffect(
                            PotionEffectType.DAMAGE_RESISTANCE,
                            40,
                            2,
                            true,
                            false,
                            true
                        ),
                        PotionEffect(
                            PotionEffectType.INCREASE_DAMAGE,
                            40,
                            2,
                            true,
                            false,
                            true
                        )
                    ))
                    4 -> it.player.addPotionEffects(listOf(
                        PotionEffect(
                            PotionEffectType.DAMAGE_RESISTANCE,
                            40,
                            2,
                            true,
                            false,
                            true
                        ),
                        PotionEffect(
                            PotionEffectType.INCREASE_DAMAGE,
                            40,
                            2,
                            true,
                            false,
                            true
                        ),
                        PotionEffect(
                            PotionEffectType.HEALTH_BOOST,
                            40,
                            1,
                            true,
                            false,
                            true
                        )
                    ))
                }
            }
        }
    }

}

var ServerPlayer.level: Int
    get() = tag.getInt("level", 1)
    set(value) = tag.set("level", value)
var ServerPlayer.exp: Int
    get() = tag.getInt("exp", 0)
    set(value) = tag.set("exp", value)
