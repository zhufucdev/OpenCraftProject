package opencraft

import com.zhufu.opencraft.util.Language
import org.bukkit.command.CommandSender
import org.bukkit.entity.HumanEntity

class Lang {
    static Language.LangGetter getter(CommandSender who) {
        return new Language.LangGetter(who instanceof HumanEntity ? PlayerManager.getInfo(who.uniqueId) : null)
    }
    static String getDefaultLangCode() {
        return Language.INSTANCE.defaultLangCode
    }
}
