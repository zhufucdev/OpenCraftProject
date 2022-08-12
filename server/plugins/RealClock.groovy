// Real Clock is a Demo Item which is not part of the server.

import bukkit.Content
import bukkit.ExtendedBlock
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import ss.Logger

import java.text.SimpleDateFormat

def updateDate = { ItemStack item ->
    def time = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date())
    item.setLore([time])
    return time
}

Content.defineItem {
    name 'RealClock'
    id UUID.fromString('E49BB519-670F-4A41-B4C3-EEAA35DD6320')
    make { item, getter ->
        updateMeta(item) {
            displayName = ChatColor.BLUE.toString() + "Real Clock"
        }
    }
    material Material.CLOCK
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
    tick { item, m, d, s, sort ->
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

class RealBlockClock extends ExtendedBlock {
    @Override
    void onCreate(@Nullable ConfigurationSection savedData, Player creator) {
        super.onCreate(savedData, creator)
        type = Material.GOLD_BLOCK
    }

    @Override
    void onBroken(Player player) {
        super.onBroken(player)
        Logger.info("[$location] onBroken")
        dropItem(Content.getDefinedItem("RealClock").newItemStack())
    }

    @Override
    void tick() {
        def time = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date())
        location.getNearbyPlayers(3D).each {
            it.sendActionBar(ChatColor.DARK_PURPLE.toString() + time)
        }
    }
}

Content.defineBlock {
    name 'RealClock'
    existsAs RealBlockClock.class
}