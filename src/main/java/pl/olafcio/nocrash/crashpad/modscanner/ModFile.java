package pl.olafcio.nocrash.crashpad.modscanner;

import java.nio.file.Path;
import java.util.jar.JarFile;

public record ModFile(Mod mod, Path path, JarFile jar) {}
