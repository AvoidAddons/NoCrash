package pl.olafcio.nocrash.crashpad.scanner.errors;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;

import java.util.regex.Matcher;

public final class RegistryObjectError implements IScanner {
    @Override
    public String regex() {
        return "Registry Object not present: (.+?):(.+?)";
    }

    @Override
    public Message message(Matcher matcher) {
        return new Message(
                MessageType.ERROR,
                MessageCategory.MOD_BROKEN,
                "Mod '%s' may be broken".formatted(matcher.group(1))
        );
    }
}
