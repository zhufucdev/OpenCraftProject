package com.zhufu.opencraft

import com.zhufu.opencraft.special_item.SpecialItemAdapter
import com.zhufu.opencraft.util.Language
import groovy.lang.Closure
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective
import java.util.UUID

class GroovySpecialItemAdapter(
    name: String,
    langName: String? = null,
    id: UUID,
    material: Material,
    make: Closure<*>?,
    deserialize: Closure<*>?,
    serialize: Closure<*>?,
    tick: Closure<*>?
) : SpecialItemAdapter(
    name,
    langName,
    id,
    material,
    make = { i: ItemStack, g: Language.LangGetter -> make!!.call(i, g); Unit }.takeIf { make != null },
    deserialize = { i: ItemStack, c: ConfigurationSection, g: Language.LangGetter -> deserialize!!.call(i, c, g); Unit }
        .takeIf { deserialize != null },
    serialize = { i: ItemStack, c: ConfigurationSection -> serialize!!.call(i, c); Unit }
        .takeIf { serialize != null },
    tick = { i: AdaptedItem, m: PlayerModifier, d: YamlConfiguration, s: Objective, o: Int -> tick!!.call(i, m, d, s, o); Unit }
        .takeIf { tick != null }
)