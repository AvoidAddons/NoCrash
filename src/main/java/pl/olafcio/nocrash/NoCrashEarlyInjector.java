package pl.olafcio.nocrash;

import org.jetbrains.annotations.NotNull;
import pl.olafcio.avoid.api.Avoid;
import pl.olafcio.avoid.platform.AvoidWrappedLoader;
import pl.olafcio.avoid_loader.interfaces.IMod;
import pl.olafcio.avoid_loader.interfaces.IModFile;
import pl.olafcio.avoid_loader.interfaces.services.IModLoader;

import java.awt.*;
import java.io.IOException;
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
