package opencraft

import com.zhufu.opencraft.Language
import org.bukkit.command.CommandSender
import org.bukkit.entity.HumanEntity

class Lang {
    static Language.LangGetter getter(CommandSender who) {
        return new Language.LangGetter(who instanceof HumanEntity ? PlayerManager.getInfo(who.uniqueId) : null)
    }
}
