package pl.olafcio.nocrash.crashpad.message;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 *
 * @param autoFix nullable
 */
public record Message(
        MessageType type,
        MessageCategory category,
        String text,
        String technical,
        Runnable autoFix,
        FixType autoFixType,
        Collection<Message> children
) {
    public Message(
            MessageType type,
            MessageCategory category,
            String text,
            String technical,
            Runnable autoFix,
            FixType autoFixType
    ) {
        this(type, category, text, technical, autoFix, autoFixType, List.of());
    }

    public Message(
            MessageType type,
            MessageCategory category,
            String text,
            String technical
    ) {
        this(type, category, text, technical, null, FixType.UNAVAILABLE);
    }

    public Message(
            MessageType type,
            MessageCategory category,
            String text,
            String technical,
            Collection<Message> children
    ) {
        this(type, category, text, technical, null, FixType.UNAVAILABLE, children);
    }

    public Message(
            MessageType type,
            MessageCategory category,
            String text,
            String technical,
            Path badResource
    ) {
        this(type, category, text, technical, () -> {
            try {
                Files.delete(badResource);
            } catch (IOException e) {
                throw new QuickFixFailed("Failed to delete mod: '%s'".formatted(badResource.toAbsolutePath().toString()), 2, e);
            }
        }, FixType.DELETE);
    }

    public Message(
            MessageType type,
            MessageCategory category,
            String text,
            String technical,
            URI toDownload
    ) {
        this(type, category, text, technical, () -> {
            try {
                var conn = toDownload.toURL()
                                     .openConnection();

                conn.setDoInput(true);
                conn.connect();

                try (var out = new FileOutputStream(Path.of("./mods/create_repair-fabric-1.20.1-1.3.0.jar").toFile())) {
                    conn.getInputStream().transferTo(out);
                }
            } catch (IOException e) {
                throw new QuickFixFailed("Failed to download 'Create Repair' mod", 3, e);
            }
        }, FixType.DOWNLOAD);
    }
}
