package pl.olafcio.nocrash;

import org.jetbrains.annotations.NotNull;
import pl.olafcio.avoid.api.Avoid;
import pl.olafcio.avoid.platform.AvoidWrappedLoader;
import pl.olafcio.avoid_loader.interfaces.IMod;
import pl.olafcio.avoid_loader.interfaces.IModFile;
import pl.olafcio.avoid_loader.interfaces.services.IModLoader;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class NoCrashEarlyInjector implements IModLoader {
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

        System.setOut(proxyStream(System.out, "out"));
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

    private static PrintStream proxyStream(PrintStream out, String type) {
        try {
            var proxyout = new FileOutputStream(Path.of(".nocrash/std%s.log".formatted(type)).toFile());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try                   { proxyout.close();              }
                catch (IOException e) { throw new RuntimeException(e); }
            }));

            //TODO Improve this code's looks. It looks like shit
            return new PrintStream(out) {
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
        } catch (FileNotFoundException e) {
            IO.println("[NoCrash] Failed to attach proxy stream for std" + type);
        }

        return out;
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
