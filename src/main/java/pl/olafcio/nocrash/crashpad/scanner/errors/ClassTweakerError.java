package pl.olafcio.nocrash.crashpad.scanner.errors;

import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;
import pl.olafcio.nocrash.crashpad.message.Message;

import java.util.regex.Matcher;

public final class ClassTweakerError implements IScanner {
    @Override
    public String regex() {
        return "Failed to read classTweaker file from mod (.+)";
    }

    @Override
    public Message message(Matcher matcher) {
        return new Message(
                MessageType.ERROR,
                MessageCategory.MOD_INCOMPATIBILITY,
                "Mod '%s' is incompatible with your Minecraft version".formatted(matcher.group(1)),
                () -> {}
        );
    }
}
