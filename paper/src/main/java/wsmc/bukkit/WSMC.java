package wsmc.bukkit;

import java.util.logging.Logger;

public class WSMC {
    public static final String MODID = "wsmc";
    public static Logger logger;
    public static boolean debug = Boolean.getBoolean("wsmc.debug");
    public static boolean dumpBytes = Boolean.getBoolean("wsmc.dumpBytes");

    public static void init(Logger pluginLogger) {
        logger = pluginLogger;
        debug = System.getProperty("wsmc.debug", "false").equalsIgnoreCase("true");
        dumpBytes = System.getProperty("wsmc.dumpBytes", "false").equalsIgnoreCase("true");
    }

    public static void debug(String msg) {
        if (!debug) return;
        if (logger != null) {
            logger.info("[DEBUG] " + msg);
        } else {
            System.out.println("[WSMC D] " + msg);
        }
    }

    public static void info(String msg) {
        if (logger != null) {
            logger.info(msg);
        } else {
            System.out.println("[WSMC I] " + msg);
        }
    }

    public static boolean debug() {
        return debug;
    }
}
