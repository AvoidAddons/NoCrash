package pl.olafcio.nocrash.crashpad.modscanner;

import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

public interface IModMapper {
    List<ModFile> map(JarFile jar, Path mod);
}
