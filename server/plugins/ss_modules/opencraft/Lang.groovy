package opencraft

import com.zhufu.opencraft.Language
import org.bukkit.command.CommandSender
import org.bukkit.entity.HumanEntity

class Lang {
    static Language.LangGetter getter(CommandSender who) {
        return new Language.LangGetter(who instanceof HumanEntity ? PlayerManager.getInfo(who.uniqueId) : null)
    }

    static String getDefaultLangCode() {
        return Language.INSTANCE.defaultLangCode
    }

    static String[] getLanguages() {
        return Language.languages.collect { it.getString("info.code") }
    }

    static String getString(String codeName, String path, Object... args) {
        Language.INSTANCE.get(codeName, path, args)
    }
}
