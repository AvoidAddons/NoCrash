package pl.olafcio.nocrash.crashpad.modscanner;

import org.jetbrains.annotations.Nullable;
import pl.olafcio.nocrash.crashpad.message.Message;

import java.nio.file.Path;
import java.util.jar.JarFile;

public interface IModAnalyzer {
    @Nullable
    Message analyze(JarFile jar, Mod mod, Path path);
}
