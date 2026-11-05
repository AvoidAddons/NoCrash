package pl.olafcio.nocrash.crashpad.modscanner.analyzer;

import org.jetbrains.annotations.Nullable;
import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.modscanner.IModAnalyzer;
import pl.olafcio.nocrash.crashpad.modscanner.Mod;
import pl.olafcio.nocrash.crashpad.modscanner.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.jar.JarFile;

public final class NonLinkedAccessModifier implements IModAnalyzer {
    @Override
    @Nullable
    public Message analyze(JarFile jar, Mod mod, Path path) {
        if (mod.platform() == Platform.FABRIC) {
            var entries = jar.entries();
            var array = new ArrayList<String>();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var name = entry.getName();

                if (name.endsWith(".accesswidener") || name.endsWith(".classtweaker") || name.endsWith(".accesstransformer")) {
                    while (name.startsWith("/"))
                        name = name.substring(1);

                    array.add(name);
                }
            }

            if (
                    (!array.isEmpty() && mod.accessModifiers().stream().noneMatch(array::contains)) ||
                    (array.isEmpty() && !mod.accessModifiers().isEmpty())
            )
                return new Message(
                        MessageType.WARNING,
                        MessageCategory.MOD_BROKEN,
                        "Mod '%s' may be broken".formatted(mod.id()),
                        "Mod '%s' includes an AW/AT/CT without using it".formatted(mod.id()),
                        path
                );
        }

        return null;
    }
}
