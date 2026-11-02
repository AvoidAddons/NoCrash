package pl.olafcio.nocrash.crashpad;

import pl.olafcio.nocrash.crashpad.scanner.IScanner;
import pl.olafcio.nocrash.crashpad.scanner.errors.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class NoCrashAction {
    private String  logs;
    private LogType logType;
    private Path    logFile;

    private static final IScanner[] scanners
                   = new IScanner[]{
                            new ClassTweakerError(),
                            new AvoidSignaturesError(),
                            new MixinRuntimeError(),
                            new TextureStitchingError(),
                            new FMLModError(),
                            new FMLSuspectedMods(),
                            new RegistryObjectError()
                   };

    public NoCrashAction() {
        IO.println("[NoCrashAction/DEBUG] Reading logs");
        readDebugLogs();

        IO.println("[NoCrashAction/DEBUG] Normalizing logs");
        this.logs = this.logs.replaceAll("\\r\\n|\\r", "\n");

        IO.println("[NoCrashAction/DEBUG] Scanning");
        if (this.logs.contains("Stopping!\n") && !this.logs.contains("[NoCrash] Preventing platform error GUI\n")) {
            IO.println("[NoCrashAction/INFO] Minecraft's window seems to have been shown");
            return;
        }

        var window = new NoCrashWindow(logFile == null ? null : logFile.toFile());
        var ambient = new NoCrashAmbient();

        new Thread(ambient::play, "NoCrash/Ambient").start();

        int i = 1;
        int errors = 0;

        for (var scanner : scanners) {
            final int g = i;
            final int exrs = errors;

            window.exec(() -> {
                window.setState("Scanning %d/%d  ==  Errors found: %d".formatted(g, scanners.length, exrs));
                window.setProgress((float)g / scanners.length);
            });

            var pattern = Pattern.compile(scanner.regex(), Pattern.MULTILINE);
            var matcher = pattern.matcher(this.logs);

            while (matcher.find()) {
                window.addMessage(scanner.message(matcher));

                errors++;

                final int exrs2 = errors;

                window.exec(() -> {
                    window.setState("Scanning %d/%d  ==  Errors found: %d".formatted(g, scanners.length, exrs2));
                    window.setProgress((float)g / scanners.length);
                });
            }

            i++;
        }
    }

    //===============//
    // FETCHING LOGS //
    //===============//

    private void readDebugLogs() {
        this.logType = LogType.DEBUG;

        try {
            this.logFile = Path.of("./logs/debug.log");
            this.logs = Files.readString(logFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            IO.println("[NoCrashAction/ERROR] unable to read debug.log  /" + e.getClass().getName());
            IO.println("                      message:       " + e.getMessage());
            IO.println("                      message/local: " + e.getLocalizedMessage());
            IO.println("");

            readLogs();
        }
    }

    private void readLogs() {
        this.logType = LogType.LATEST;

        try {
            this.logFile = Path.of("./logs/latest.log");
            this.logs = Files.readString(logFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            IO.println("[NoCrashAction/ERROR] unable to read latest.log  /" + e.getClass().getName());
            IO.println("                      message:       " + e.getMessage());
            IO.println("                      message/local: " + e.getLocalizedMessage());
            IO.println("");

            readDummy();
        }
    }

    private void readDummy() {
        this.logType = LogType.NONE;
        this.logFile = null;
        this.logs = "";
    }

    //=========//
    // GETTERS //
    //=========//

    public String getLogs() {
        return logs;
    }

    public LogType getLogType() {
        return logType;
    }
}
