package pl.olafcio.nocrash.crashpad;

import org.jetbrains.annotations.NotNull;
import pl.olafcio.commons.stringscan.Pattern;
import pl.olafcio.commons.stringscan.PatternSetup;
import pl.olafcio.nocrash.crashpad.logs.LogCollection;
import pl.olafcio.nocrash.crashpad.logs.LogType;
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
import pl.olafcio.nocrash.crashpad.modscanner.overviewer.*;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;
import pl.olafcio.nocrash.crashpad.scanner.errors.*;
import pl.olafcio.nocrash.crashpad.scanner.warnings.*;
import pl.olafcio.commons.stringscan.PatternMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.jar.JarFile;

public final class NoCrashAction {
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
                            new UnresolvedDependencyFinder(),
                            new CreateAndGalosphere(),
                            new CreateAndStarlight(),
                            new IrisXe(),
                            new FarmersOPlenty()
                   };

    static void main() {
        new NoCrashAction();
    }

    public NoCrashAction() {
        IO.println("[NoCrashAction/DEBUG] Setting up logs");

        logcol.putLogIfExists(LogType.LATEST, Path.of("logs/latest.log"));
        logcol.putLogIfExists(LogType.DEBUG, Path.of("logs/debug.log"));
        logcol.putLogIfExists(LogType.STDOUT, Path.of(".nocrash/stdout.log"));
        logcol.putLogIfExists(LogType.STDERR, Path.of(".nocrash/stderr.log"));

        IO.println("[NoCrashAction/DEBUG] Starting initial scan");

        var hasCrashed = checkAndHasCrashed();

        if (Boolean.FALSE.equals(hasCrashed))
            return;

        IO.println("[NoCrashAction/DEBUG] Finding additional log files");

        findFile(".", "hs_err_pid[0-9]+\\.log", 1000 * 16, LogType.HS_ERR_PID);
        findFile("crash-reports", ".*", 1000 * 16, LogType.CRASH_REPORT);

        IO.println("[NoCrashAction/DEBUG] Starting GUI");

        var window = new NoCrashWindow(logcol);
        var ambient = new NoCrashAmbient();

        new Thread(ambient::play, "NoCrash/Ambient").start();

        scanLogs(window);
        mapMods(window);
        overviewMods(window);
        scanMods(window);

        final int exrs = this.errors;

        window.race(() -> {
            window.setState("Finished  ·  Errors found: %d".formatted(exrs));
            window.setProgress(1f);
        });
    }

    private Boolean checkAndHasCrashed() {
        var context = new PatternSetup();

        context.addPattern("Stopping!\n");
        context.addPattern("[NoCrash] Preventing platform error GUI\n");

        context.addPattern(new ForgeMinecraftPattern());
        context.addPattern(new FabricMinecraftPattern());

        return logcol.forLogs(LogType.OUTPUT_LIKE, log -> {
            try (var reader = new PatternMatcher<>(context, log::openInputStream)) {
                boolean hadPrevention = false;
                boolean hadGraceful = false;

                while (true) {
                    var a = reader.find();
                    if (a == null)
                        break;
                    else if (a == 1)
                        hadPrevention = true;
                    else if (a == 0)
                        hadGraceful = true;
                }

                if (!hadPrevention && hadGraceful) {
                    IO.println("[NoCrashAction/INFO] Minecraft's window seems to have been shown");
                    return false;
                }
            } catch (Exception ignored) {
                // I don't even know what to say
                return null;
            }

            return null;
        });
    }

    private final LogCollection logcol
            = new LogCollection();

    private void findFile(String directory, String regex, int maximumTime, LogType logType) {
        var folder = Path.of(directory);

        if (!Files.isDirectory(folder))
            return;

        try (var files = Files.list(folder)) {
            files.forEach(path -> {
                try {
                    if (
                            !Files.isDirectory(path) &&
                            path.getFileName().toString().matches(regex) &&
                            System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis() < maximumTime
                    ) {
                        logcol.putLog(logType, path);
                    }
                } catch (IOException e) {
                    new RuntimeException("Failed to scan file: '%s'".formatted(path.getFileName().toString()), e).printStackTrace();
                }
            });
        } catch (IOException e) {
            new RuntimeException("Failed to scan for... files ('%s')".formatted(regex), e).printStackTrace();
        }
    }

    private void overviewMods(NoCrashWindow window) {
        int i = 0;

        for (var overviewer : overviewers) {
            final int g = i++;
            final int exrs = errors;

            window.race(() -> {
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
        final int exrs = errors;

        window.race(() -> {
            window.setState("Scanning logs with %d scanners  ·  Errors found: %d".formatted(scanners.length, exrs));
            window.setProgress(1F);
        });

        var context = new PatternSetup();

        for (var scanner : scanners) {
            context.addPattern(scanner.regex().split("[*(]", 2)[0], streamSupplier -> {
                final String segment;

                try (var stream = streamSupplier.get()) {
                    segment = new String(stream.readNBytes(100), StandardCharsets.UTF_8);
                }

                var pattern = java.util.regex.Pattern.compile(scanner.regex(), java.util.regex.Pattern.MULTILINE);
                var matcher = pattern.matcher(segment);

                while (matcher.find()) {
                    window.addMessage(scanner.message(matcher));
                    errors++;
                }
            });
        }

        logcol.forLogs(LogType.MINECRAFT, log -> {
            try (var reader = new PatternMatcher<>(context, log::openInputStream)) {
                reader.scan();
            } catch (Exception ignored) {
                // I don't even know what to say
            }
        });
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

            window.race(() -> {
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

                    window.race(() -> {
                        window.setState("Mapping mods %d/%d  ·  Errors found: %d".formatted(g, mappers.length * mods.size(), exrs));
                        window.setProgress((float) g / (mappers.length * mods.size()));
                    });

                    i++;

                    try {
                        var map = mapper.map(jar, mod);
                        if (map.isEmpty())
                            continue;

                        modobjects.addAll(map);
                    } catch (Throwable ignored) {
                        continue;
                    }

                    IO.println("[NoCrashAction/INFO] Found mod '%s' with mapper '%s'".formatted(mod.toAbsolutePath().toString(), mapper.getClass().getSimpleName()));
                }
            }
        } catch (IOException e) {
            window.race(() -> {
                window.setState("Failed to map mods!  ·  Errors found: %d".formatted(errors));
            });
        }
    }

    private void scanMods(NoCrashWindow window) {
        int i = 1;

        for (var mod : modobjects) {
            for (var analyzer : analyzers) {
                final int g = i;
                final int exrs = errors;

                window.race(() -> {
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

    private static class FabricMinecraftPattern extends Pattern {
        public FabricMinecraftPattern() {
            super("Loading Minecraft ");
        }

        @Override
        public void action(Supplier<InputStream> supplier) throws IOException {
            try (var stream = supplier.get()) {
                State.minecraft = new String(stream.readNBytes(16), StandardCharsets.UTF_8).trim().split(" ")[0];
            }
        }

        @Override
        public boolean shouldReturnInFind() {
            return false;
        }
    }

    private static class ForgeMinecraftPattern extends Pattern {
        public ForgeMinecraftPattern() {
            super("Launching target 'forge_client' with arguments [--version,");
        }

        @Override
        public void action(Supplier<InputStream> supplier) throws IOException {
            try (var stream = supplier.get()) {
                State.minecraft = new String(stream.readNBytes(16), StandardCharsets.UTF_8).split(",")[0].trim();
            }
        }

        @Override
        public boolean shouldReturnInFind() {
            return false;
        }
    }
}
