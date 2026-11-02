package pl.olafcio.nocrash.crashpad.message;

import java.io.IOException;
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
        Runnable autoFix,
        FixType autoFixType,
        Collection<Message> children
) {
    public Message(
            MessageType type,
            MessageCategory category,
            String text,
            Runnable autoFix,
            FixType autoFixType
    ) {
        this(type, category, text, autoFix, autoFixType, List.of());
    }

    public Message(
            MessageType type,
            MessageCategory category,
            String text
    ) {
        this(type, category, text, null, FixType.UNAVAILABLE);
    }

    public Message(
            MessageType type,
            MessageCategory category,
            String text,
            Collection<Message> children
    ) {
        this(type, category, text, null, FixType.UNAVAILABLE, children);
    }

    public Message(
            MessageType type,
            MessageCategory category,
            String text,
            Path badResource
    ) {
        this(type, category, text, () -> {
            try {
                Files.delete(badResource);
            } catch (IOException e) {
                throw new QuickFixFailed("Failed to delete mod: '%s'".formatted(badResource.toAbsolutePath().toString()), 2, e);
            }
        }, FixType.DELETE);
    }
}
