package opencraft

import com.zhufu.opencraft.Game

class Global {
    static boolean isDebug() {
        return Game.env.getBoolean("debug", false)
    }

    static <T> T arg(String name, Class<T> tClass) {
        return Game.env.get(name) as T
    }
}
