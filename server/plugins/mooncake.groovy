import bukkit.Server
import com.zhufu.opencraft.data.OfflineInfo
import com.zhufu.opencraft.special_item.SpecialItem
import com.zhufu.opencraft.util.Language
import de.tr7zw.nbtapi.NBTItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import opencraft.PlayerManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import ss.Logger

ItemStack getMooncakeFor(OfflineInfo info) {
    def siid = UUID.fromString("B3EB5611-3BD9-4734-B633-FCD892FC7F98")

    def r = new ItemStack(Material.PUMPKIN_PIE)
    r.editMeta {
        String displayName
        String lore
        if (info.targetLang == Language.LANG_EN) {
            displayName = "Mooncake"
            lore = "Gift for ${info.name} by Imaizumi_Kagerou"
        } else {
            displayName = "月饼"
            lore = "今泉影郎向${info.name}的馈赠"
        }
        it.displayName(Component.text(displayName).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
        it.lore([Component.text(lore).color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.ITALIC)])
    }
    def nbt = new NBTItem(r, true)
    nbt.setUUID(SpecialItem.KEY_SIID, siid)
    r
}

Server.runSync {
    def given = 0

    for (def player : Bukkit.getOfflinePlayers()) {
        def info = PlayerManager.getOfflineInfo(player.uniqueId)
        if (info == null) {
            continue
        }
        def cake = getMooncakeFor(info)
        def inv = info.inventory.getOrCreate("survivor")
        inv.addItem(cake)
        inv.update()
        given ++
    }

    Logger.info("$given were sent out.")
}
