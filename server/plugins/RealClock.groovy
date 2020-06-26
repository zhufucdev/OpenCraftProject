// Real Clock is a Demo Item which is not part of the server.

import bukkit.Content
import com.zhufu.opencraft.Language.LangGetter
import com.zhufu.opencraft.PlayerModifier
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
    item.setLore([new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date())])
}

Content.defineItem {
    name 'RealClock'
    make { ItemStack item, LangGetter getter ->
        updateMeta(item) {
            displayName = ChatColor.BLUE.toString() + "Real Clock"
        }
    }
    isItem { item ->
        item.itemMeta.displayName == ChatColor.BLUE.toString() + "Real Clock"
    }
    type Material.CLOCK
    tick { ItemStack item, PlayerModifier m, ConfigurationSection d, Objective s, int sort ->
        if (item.itemMeta.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE))
            updateDate(item)
    }
    onLeftClicked { ItemStack item, Player player ->
        if (item.itemMeta.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)) {
            item.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE)
            item.setLore null
        } else
            item.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
    }
}