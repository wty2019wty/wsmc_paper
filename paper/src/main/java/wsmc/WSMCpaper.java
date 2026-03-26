package wsmc;

public class WSMCpaper {
	public static final String MODID = "wsmc";
	private static boolean debug =
			System.getProperty("wsmc.debug", "false").equalsIgnoreCase("true");
	public static boolean dumpBytes =
			System.getProperty("wsmc.dumpBytes", "false").equalsIgnoreCase("true");

	private static Logger logger = new Logger() {
		@Override
		public void info(String msg) {
			System.out.println("[WSMC I] " + msg);
		}

		@Override
		public void debug(String msg) {
			if (!debug) return;
			System.out.println("[WSMC D] " + msg);
		}
	};

	public static void setLogger(Logger logger) {
		WSMCpaper.logger = logger;
	}

	public static void debug(String msg) {
		if (!debug) return;
		logger.debug(msg);
	}

	public static void info(String msg) {
		logger.info(msg);
	}

	public static boolean debug() {
		return debug;
	}
}
