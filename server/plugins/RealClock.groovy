// Real Clock is a Demo Item which is not part of the server.

import bukkit.Content
import com.zhufu.opencraft.Language.LangGetter
import com.zhufu.opencraft.PlayerModifier
import com.zhufu.opencraft.special_item.SpecialItemAdapter
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Objective
import ss.Logger

import java.text.SimpleDateFormat

def updateDate = { ItemStack item ->
    def time = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date())
    item.setLore([time])
    return time
}

Content.defineItem {
    name 'RealClock'
    make { ItemStack item, LangGetter getter ->
        updateMeta(item) {
            displayName = ChatColor.BLUE.toString() + "Real Clock"
        }
    }
    isItem { item ->
        item.hasItemMeta() && item.itemMeta.displayName == ChatColor.BLUE.toString() + "Real Clock"
    }
    type Material.CLOCK
    recipe {
        pattern {
            firstLine 'XAX'
            secondLine 'XBX'
            thirdLine 'XXX'
        }
        where 'A', { type == Material.OBSERVER }
        where 'B', { type == Material.REDSTONE }
        where 'X', { type == Material.GOLD_INGOT }
    }
    disorderedRecipe {
        pattern {
            involve 'A', 1
            involve 'B', 1
        }
        where 'A', { type == Material.OBSERVER }
        where 'B', { type == Material.CLOCK }
    }
    tick { SpecialItemAdapter.AdapterItem item, PlayerModifier m, ConfigurationSection d, Objective s, int sort ->
        if (item.itemMeta.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)) {
            def time = updateDate(item)
            if (item.inventoryPosition >= 9)
                s.getScore(time).score = sort
        }
    }
    onLeftClicked { ItemStack item, Player player ->
        if (item.itemMeta.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)) {
            item.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE)
            item.setLore null
        } else
            item.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
    }
}