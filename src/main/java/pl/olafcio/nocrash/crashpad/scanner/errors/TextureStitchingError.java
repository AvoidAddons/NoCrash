package pl.olafcio.nocrash.crashpad.scanner.errors;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;

public final class TextureStitchingError implements IScanner {
    @Override
    public String regex() {
        return "Unable to fit: (.+?):(.+?) - size: ([0-9]+?)x([0-9]+?)";
    }

    @Override
    public Message message(Matcher matcher) {
        return new Message(
                MessageType.ERROR,
                MessageCategory.MOD_BROKEN,
                "Mod/resource pack '%s' is broken".formatted(matcher.group(1)),
                "Unable to fit: '%s:%s' - size: %sx%s".formatted(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4))
        );
    }
}
