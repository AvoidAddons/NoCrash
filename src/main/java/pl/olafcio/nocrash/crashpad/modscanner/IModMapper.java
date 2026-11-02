package pl.olafcio.nocrash.crashpad.modscanner;

import java.util.jar.JarFile;

public interface IModMapper {
    Mod[] map(JarFile jar);
}
