package com.zhufu.opencraft

import com.zhufu.opencraft.special_item.SpecialItemAdapter
import groovy.lang.Closure
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective
import org.codehaus.groovy.runtime.DefaultGroovyMethods

class GroovySpecialItemAdapter(
    name: String,
    langName: String? = null,
    material: Material,
    make: Closure<*>?,
    deserialize: Closure<*>?,
    serialize: Closure<*>?,
    judgeFromItem: Closure<Boolean>?,
    judgeFromConfig: Closure<Boolean>?,
    tick: Closure<*>?
) : SpecialItemAdapter(
    name,
    langName,
    material,
    make = { i: ItemStack, g: Language.LangGetter -> make!!.call(i, g); Unit }.takeIf { make != null },
    deserialize = { i: ItemStack, c: ConfigurationSection, g: Language.LangGetter -> deserialize!!.call(i, c, g); Unit }
        .takeIf { deserialize != null },
    serialize = { i: ItemStack, c: ConfigurationSection -> serialize!!.call(i, c); Unit }
        .takeIf { serialize != null },
    judgeFromItem = { i: ItemStack -> judgeFromItem!!.call(i) }
        .takeIf { judgeFromItem != null },
    judgeFromConfig = { c: ConfigurationSection -> judgeFromConfig!!.call(c) }
        .takeIf { judgeFromConfig != null },
    tick = { i: AdapterItem, m: PlayerModifier, d: YamlConfiguration, s: Objective, o: Int -> tick!!.call(i, m, d, s, o); Unit }
        .takeIf { tick != null }
)