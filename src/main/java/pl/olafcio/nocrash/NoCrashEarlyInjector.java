package pl.olafcio.nocrash;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.DefaultErrorHandler;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import pl.olafcio.avoid.api.Avoid;
import pl.olafcio.avoid.platform.AvoidWrappedLoader;
import pl.olafcio.avoid_loader.interfaces.IMod;
import pl.olafcio.avoid_loader.interfaces.IModFile;
import pl.olafcio.avoid_loader.interfaces.services.IModLoader;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class NoCrashEarlyInjector implements IModLoader {
    static final String APPENDER_NAME = "NoCrash";

    @Override
    public void load() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                throw new RuntimeException();
            }
        } catch (Throwable e) {
            IO.println("NoCrash cannot run in a headless environment! Skipping.");
            return;
        }

        try {
            var outF = new FileOutputStream(Path.of(".nocrash/stdout.log").toFile());

            System.setOut(proxyStream(System.out, outF));

            proxyLogging(outF);
        } catch (FileNotFoundException e) {
            IO.println("[NoCrash] Failed to attach proxy stream for stdout and log4j");
        }

        System.setErr(proxyStream(System.err, "err"));

        try {
            var nc_logs = AvoidWrappedLoader.getGameDir().resolve("logs/nocrash.log").toFile();
            var nc_jar = getJarPath();

            var minecraftProcess = ProcessHandle.current();
            var modloader = AvoidWrappedLoader.isForge()    ? "FORGE"    :
                            AvoidWrappedLoader.isNeoForge() ? "NEOFORGE" :
                            AvoidWrappedLoader.isFabric()   ? "FABRIC"   :
                            AvoidWrappedLoader.isSponge()   ? "SPONGE"   :
                            AvoidWrappedLoader.isPaper()    ? "PAPER"    :
                                                              null;

            new ProcessBuilder()
                    .command(minecraftProcess.info().command().orElseThrow(), "-Xmx256M", "-Xms256M", "-cp", NoCrashDeps.download().toAbsolutePath() + ";" + nc_jar, "pl.olafcio.nocrash.crashpad.NoCrashMain", String.valueOf(minecraftProcess.pid()), modloader, Avoid.getBrand(), Avoid.getVersion())
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(nc_logs)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Why tf do I have to do this.
    private static void proxyLogging(OutputStream out) {
        var configuration = LoggerContext.getContext().getConfiguration();

        configuration.addAppender(new Appender() {
            @Override
            public void append(LogEvent event) {
                try {
                    out.write(("(" + event.getLoggerName() + ") " + event.getMessage().getFormattedMessage() + "\n").getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                    // Tf am i going to do
                }
            }

            @Override
            public String getName() {
                return APPENDER_NAME;
            }

            @Override
            public Layout<? extends Serializable> getLayout() {
                return null;
            }

            @Override
            public boolean ignoreExceptions() {
                return false;
            }

            @Override
            public ErrorHandler getHandler() {
                return new DefaultErrorHandler(this);
            }

            @Override
            public void setHandler(ErrorHandler handler) {

            }

            @Override
            public State getState() {
                return State.STARTED;
            }

            @Override
            public void initialize() {

            }

            @Override
            public void start() {

            }

            @Override
            public void stop() {

            }

            @Override
            public boolean isStarted() {
                return true;
            }

            @Override
            public boolean isStopped() {
                return false;
            }
        });

        configuration
                .getRootLogger()
                .addAppender(configuration.getAppender(APPENDER_NAME), Level.INFO, null);

        Configurator.reconfigure(configuration);
    }

    private static PrintStream proxyStream(PrintStream in, String type) {
        try {
            return proxyStream(in, new FileOutputStream(Path.of(".nocrash/std%s.log".formatted(type)).toFile()));
        } catch (FileNotFoundException e) {
            IO.println("[NoCrash] Failed to attach proxy stream for std" + type);

            return in;
        }
    }

    private static PrintStream proxyStream(PrintStream in, OutputStream proxyout) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try                   { proxyout.close();              }
            catch (IOException e) { throw new RuntimeException(e); }
        }));

        //TODO Improve this code's looks. It looks like shit
        return new PrintStream(in) {
            @Override
            public void write(int b) {
                super.write(b);

                try {
                    proxyout.write(b);
                } catch (IOException ignored) {
                    // I suppose
                }
            }

            @Override
            public void write(@NotNull byte[] buf) throws IOException {
                super.write(buf);

                try {
                    proxyout.write(buf);
                } catch (IOException ignored) {
                    // I suppose
                }
            }

            @Override
            public void write(@NotNull byte[] buf, int off, int len) {
                super.write(buf, off, len);

                try {
                    proxyout.write(buf, off, len);
                } catch (IOException ignored) {
                    // I suppose
                }
            }

            @Override
            public void writeBytes(@NotNull byte[] buf) {
                super.writeBytes(buf);

                try {
                    proxyout.write(buf);
                } catch (IOException ignored) {
                    // I suppose
                }
            }

            @Override
            public void flush() {
                super.flush();

                try {
                    proxyout.flush();
                } catch (IOException ignored) {
                    // I suppose
                }
            }

            @Override
            public void close() {
                super.close();

                try {
                    proxyout.close();
                } catch (IOException ignored) {
                    // I suppose
                }
            }
        };
    }

    /**
     * This will be replaced by JarManager.getJarPath in Avoid v2.1.
     */
    @NotNull
    private String getJarPath() {
        var pkg = "pl/olafcio/nocrash";
        var selfJAR = this.getClass().getClassLoader().getResource(pkg).getPath().split("!")[0].substring(6).replace("%20", " ");

        return selfJAR;
    }

    @Override
    public Collection<IMod> process(IModFile iModFile) {
        return List.of();
    }
}
