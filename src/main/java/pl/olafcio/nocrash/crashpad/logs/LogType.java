package pl.olafcio.nocrash.crashpad.logs;

public enum LogType {
    LATEST,
    DEBUG,
    STDOUT,
    STDERR,
    CRASH_REPORT,
    HS_ERR_PID {
        @Override
        public String additionalCensor(String data) {
            return data.split("------")[0];
        }
    };

    public String additionalCensor(String data) {
        return data;
    }

    public static final LogType[] OUTPUT_LIKE
                  = new LogType[]{ LATEST, DEBUG, STDOUT };

    public static final LogType[] MINECRAFT
                  = new LogType[]{ LATEST, DEBUG, STDOUT, STDERR, CRASH_REPORT };
}
