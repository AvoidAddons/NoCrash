package pl.olafcio.nocrash.crashpad;

import org.jetbrains.annotations.NotNull;
import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.modscanner.IModAnalyzer;
import pl.olafcio.nocrash.crashpad.modscanner.IModMapper;
import pl.olafcio.nocrash.crashpad.modscanner.IModOverviewer;
import pl.olafcio.nocrash.crashpad.modscanner.ModFile;
import pl.olafcio.nocrash.crashpad.modscanner.analyzer.NonLinkedAccessModifier;
import pl.olafcio.nocrash.crashpad.modscanner.analyzer.NonQuadTextures;
import pl.olafcio.nocrash.crashpad.modscanner.mapper.AvoidMS;
import pl.olafcio.nocrash.crashpad.modscanner.mapper.FabricMS;
import pl.olafcio.nocrash.crashpad.modscanner.overviewer.DuplicateModDetector;
import pl.olafcio.nocrash.crashpad.modscanner.overviewer.UnresolvedDependencyFinder;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;
import pl.olafcio.nocrash.crashpad.scanner.errors.*;
import pl.olafcio.nocrash.crashpad.scanner.warnings.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.jar.JarFile;
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
                            new FMLEntrypointError(),
                            new FMLTypoError(),
                            new RegistryObjectError(),
                            new ItemDoesNotExistError()
                   };

    private static final IModMapper[] mappers
                   = new IModMapper[]{
                            new FabricMS(),
                            new AvoidMS()
                   };

    private static final IModAnalyzer[] analyzers
                   = new IModAnalyzer[]{
                            new NonQuadTextures(),
                            new NonLinkedAccessModifier()
                   };

    private static final IModOverviewer[] overviewers
                   = new IModOverviewer[]{
                            new DuplicateModDetector(),
                            new UnresolvedDependencyFinder()
                   };

    static void main() {
        new NoCrashAction();
    }

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

        var window = new NoCrashWindow(logFile == null ? null : logFile.toFile(), logs);
        var ambient = new NoCrashAmbient();

        new Thread(ambient::play, "NoCrash/Ambient").start();

        scanLogs(window);
        mapMods(window);
        overviewMods(window);
        scanMods(window);
        hsErrPid(window);

        final int exrs = this.errors;

        window.exec(() -> {
            window.setState("Finished  ·  Errors found: %d".formatted(exrs));
            window.setProgress(1f);
        });
    }

    private void hsErrPid(NoCrashWindow window) {
        final int exrs = this.errors;

        window.exec(() -> {
            window.setState("Checking for hs_err_pid  ·  Errors found: %d".formatted(exrs));
            window.setProgress(1f / 2);
        });

        try (var files = Files.list(Path.of("."))) {
            files.forEach(path -> {
                try {
                    if (path.getFileName().toString().matches("hs_err_pid[0-9]+\\.log") && System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis() < 1000*16) {
                        errors++;

                        window.addMessage(new Message(
                                MessageType.CRITICAL,
                                MessageCategory.JVM_ISSUE,
                                "Your Minecraft might've not been assigned enough RAM (memory)",
                                "%s file present".formatted(path.getFileName().toString())
                        ));
                    }
                } catch (IOException e) {
                    new RuntimeException("Failed to scan hs_err_pid file: '%s'".formatted(path.getFileName().toString()), e).printStackTrace();
                }
            });
        } catch (IOException e) {
            new RuntimeException("Failed to scan for hs_err_pid files", e).printStackTrace();
        }
    }

    private void overviewMods(NoCrashWindow window) {
        int i = 0;

        for (var overviewer : overviewers) {
            final int g = i++;
            final int exrs = errors;

            window.exec(() -> {
                window.setState("Overviewing mods %d/%d  ·  Errors found: %d".formatted(g, modobjects.size(), exrs));
                window.setProgress((float)g / modobjects.size());
            });

            var out = overviewer.overview(modobjects);

            for (var msg : out) {
                window.addMessage(msg);

                if (msg.type() != MessageType.INFO)
                    errors++;
            }
        }
    }

    private int errors = 0;

    private void scanLogs(NoCrashWindow window) {
        int i = 1;

        for (var scanner : scanners) {
            final int g = i;
            final int exrs = errors;

            window.exec(() -> {
                window.setState("Scanning %d/%d  ·  Errors found: %d".formatted(g, scanners.length, exrs));
                window.setProgress((float)g / scanners.length);
            });

            var pattern = Pattern.compile(scanner.regex(), Pattern.MULTILINE);
            var matcher = pattern.matcher(this.logs);

            while (matcher.find()) {
                window.addMessage(scanner.message(matcher));

                errors++;

                final int exrs2 = errors;

                window.exec(() -> {
                    window.setState("Scanning %d/%d  ·  Errors found: %d".formatted(g, scanners.length, exrs2));
                    window.setProgress((float)g / scanners.length);
                });
            }

            i++;
        }
    }

    private final ArrayList<ModFile> modobjects
            = new ArrayList<>();

    @NotNull
    private ArrayList<Path> iterateMods(NoCrashWindow window) {
        var mods = new ArrayList<Path>();

        int i = 1;

        for (var folder : new String[]{ "mods", "avoidmods", "plugins", "avoidplugins", "resourcepacks", "avoidaddons" }) {
            final int g = i++;
            final int exrs = errors;

            window.exec(() -> {
                window.setState("Iterating mods %d/%d  ·  Errors found: %d".formatted(g, 6, exrs));
                window.setProgress((float) g / 6);
            });

            var path = Path.of(folder);
            if (!Files.isDirectory(path))
                continue;

            try (var modstream = Files.list(path)) {
                mods.addAll(modstream.toList());
            } catch (IOException e) {
                IO.println("[NoCrashAction/ERROR] Cannot scan directory '%s'; skipping".formatted(folder));

                errors++;

                window.addMessage(new Message(
                        MessageType.ERROR,
                        MessageCategory.OTHER,
                        "Unable to use directory: %s".formatted(folder),
                        "I/O error"
                ));
            }
        }
        return mods;
    }

    private void mapMods(NoCrashWindow window) {
        var mods = iterateMods(window);

        int i = 1;

        try {
            for (var mod : mods) {
                var jar = new JarFile(mod.toFile());

                for (var mapper : mappers) {
                    final int g = i;
                    final int exrs = errors;

                    window.exec(() -> {
                        window.setState("Mapping mods %d/%d  ·  Errors found: %d".formatted(g, mappers.length * mods.size(), exrs));
                        window.setProgress((float) g / (mappers.length * mods.size()));
                    });

                    i++;

                    try {
                        var map = mapper.map(jar);
                        if (map.length == 0)
                            continue;

                        for (var modN : map)
                            modobjects.add(new ModFile(modN, mod, jar));
                    } catch (Throwable ignored) {
                        continue;
                    }

                    IO.println("[NoCrashAction/INFO] Found mod '%s' with mapper '%s'".formatted(mod.toAbsolutePath().toString(), mapper.getClass().getSimpleName()));
                }
            }
        } catch (IOException e) {
            window.setState("Failed to map mods!  ·  Errors found: %d".formatted(errors));
        }
    }

    private void scanMods(NoCrashWindow window) {
        int i = 1;

        for (var mod : modobjects) {
            for (var analyzer : analyzers) {
                final int g = i;
                final int exrs = errors;

                window.exec(() -> {
                    window.setState("Analyzing mods %d/%d  ·  Errors found: %d".formatted(g, analyzers.length * modobjects.size(), exrs));
                    window.setProgress((float) g / (analyzers.length * modobjects.size()));
                });

                i++;

                analyzer.analyze(mod.jar(), mod.mod(), mod.path());
            }

            try {
                mod.jar().close();
            } catch (IOException e) {
                IO.println("[NoCrashAction/WARN] Failed to close '%s': %s (%s)".formatted(mod.path().toAbsolutePath().toString(), e.getMessage(), e.getClass().getSimpleName()));
            }
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
