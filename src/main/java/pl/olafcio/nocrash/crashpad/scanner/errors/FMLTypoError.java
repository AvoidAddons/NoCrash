package pl.olafcio.nocrash.crashpad.scanner.errors;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

public final class FMLTypoError implements IScanner {
    @Override
    public String regex() {
        return "ModFileLoadingException: Mod candidate (.+?) contains broken toml, likely due to a typo.";
    }

    @Override
    public Message message(Matcher matcher) {
        String filename = matcher.group(1);
        Path path = null;

        finding:
        try (var files = Files.list(Path.of("mods"))) {
            var array = files.toList();

            for (var f : array) {
                if (f.getFileName().toString().equals(filename)) {
                    path = f;
                    break finding;
                }
            }

            for (var f : array) {
                if (f.getFileName().toString().equalsIgnoreCase(filename)) {
                    path = f;
                    break finding;
                }
            }
        } catch (IOException e) {
            new RuntimeException("Failed to find mod path of: '%s'".formatted(filename), e).printStackTrace();
        }

        return new Message(
                MessageType.ERROR,
                MessageCategory.MOD_BROKEN,
                "Mod '%s' is broken".formatted(filename),
                path
        );
    }
}
