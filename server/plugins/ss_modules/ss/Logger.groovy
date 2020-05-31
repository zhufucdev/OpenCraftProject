package ss

import org.bukkit.Bukkit

class Logger {
    private static String getCallerName(Thread thread) {
        def stack = thread.stackTrace
        def substr = { name -> return name.substring(0, name.lastIndexOf('.')) }
        def lastScript = null
        for (def i = 1; i < stack.size(); i++) {
            def name = stack[i].fileName
            if (name != null && name.endsWith(".groovy")) {
                lastScript = substr(name)
            }
        }
        if (lastScript != null)
            return lastScript
        else if (stack.size() > 0)
            return substr(stack[0].fileName)
        else
            return null
    }

    static void info(String text) {
        def caller = getCallerName(Thread.currentThread())
        Bukkit.logger.info((caller == null ? " " : ("[" + caller + "] ")) + text)
    }

    static void warning(String text) {
        def caller = getCallerName(Thread.currentThread())
        Bukkit.logger.warning((caller == null ? " " : ("[" + caller + "] ")) + text)
    }
}
