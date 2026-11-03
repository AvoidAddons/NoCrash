package pl.olafcio.nocrash.crashpad.modscanner.analyzer;

import org.jetbrains.annotations.Nullable;
import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.modscanner.IModAnalyzer;
import pl.olafcio.nocrash.crashpad.modscanner.Mod;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;

public final class NonQuadTextures implements IModAnalyzer {

    private static final ImageObserver DUMMY = new ImageObserver() {
        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            return false;
        }
    };

    @Override
    @Nullable
    public Message analyze(JarFile jar, Mod mod, Path path) {
        var entries = jar.entries();

        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (entry.getName().endsWith(".png")) {
                byte[] imageData;

                try (var stream = jar.getInputStream(entry)) {
                    imageData = stream.readAllBytes();
                } catch (IOException e) {
                    return new Message(
                            MessageType.WARNING,
                            MessageCategory.MOD_INCOMPATIBILITY,
                            "Mod '%s' uses textures that cannot be loaded".formatted(mod.id()),
                            path
                    );
                }

                var image = Toolkit.getDefaultToolkit().createImage(imageData);

                var width = image.getWidth(DUMMY);
                var height = image.getHeight(DUMMY);

                if (width == -1 || height == -1)
                    continue;

                // Is this overcomplicated?
                if (
                        Math.floor(Math.sqrt(width)) != Math.ceil(Math.sqrt(width)) ||
                        Math.floor(Math.sqrt(height)) != Math.ceil(Math.sqrt(height))
                )
                    return new Message(
                            MessageType.WARNING,
                            MessageCategory.MOD_INCOMPATIBILITY,
                            "Mod '%s' uses textures that the size of is not a multiplication of 2".formatted(mod.id()),
                            path
                    );
            }
        }

        return null;
    }
}
