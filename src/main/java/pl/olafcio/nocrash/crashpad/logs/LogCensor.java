package pl.olafcio.nocrash.crashpad.logs;

public class LogCensor {
    private String logs;

    public LogCensor(String logs) {
        this.logs = logs;
    }

    public String censor() {
        this.hideAT();
        this.hideNick();
        this.hideIP();
        this.hideUser();
        this.hideEnv();

        return logs;
    }

    private void hideAT() {
        this.logs = this.logs.replaceAll("SignedJWT: ([a-zA-Z0-9-_+.]+?)", "SignedJWT: ??");
        this.logs = this.logs.replaceAll("ey((?! ).)+", "??");
    }

    private void hideNick() {
//        this.logs = this.logs.replaceAll();
        //TODO
    }

    private void hideIP() {
        this.logs = this.logs.replaceAll("192\\.168\\.[0-9]+\\.[0-9]*", "[Local IP]");
    }

    private void hideUser() {
        this.logs = this.logs.replaceAll("C:[/\\\\]Users[/\\\\](.+?)[/\\\\]", "%USERPROFILE%\\\\");
    }

    private void hideEnv() {
        var env = new String[]{ "APPDATA", "LOCALAPPDATA", "TEMP", "TMP" };

        for (var key : env) {
            var value = System.getenv(key);
            if (value == null || value.length() < 3)
                continue;

            this.logs = this.logs.replace(value.replace("\\", "/"), "%" + key + "%\\");
            this.logs = this.logs.replace(value, "%" + key + "%\\");
        }
    }
}
