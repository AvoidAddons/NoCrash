package pl.olafcio.nocrash.crashpad.scanner.errors;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;

import java.util.regex.Matcher;

public final class FMLModError implements IScanner {
    @Override
    public String regex() {
        return "ModLoadingException: (.+) \\((.+)\\) encountered an (.*)";
    }

    @Override
    public Message message(Matcher matcher) {
        return new Message(
                MessageType.ERROR,
                MessageCategory.MOD_BROKEN,
                "Mod '%s' (%s) is broken".formatted(matcher.group(1), matcher.group(2)),
                "Mod '%s' (%s) encountered an %s".formatted(matcher.group(1), matcher.group(2), matcher.group(3))
        );
    }
}
