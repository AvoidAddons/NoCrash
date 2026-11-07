package pl.olafcio.nocrash.crashpad.scanner.errors;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;

import java.util.regex.Matcher;

public final class FMLConfigError implements IScanner {
    @Override
    public String regex() {
        return "ConfigLoadingException: Failed loading config file ([a-zA-Z0-9_- ]+) of type ([a-zA-Z0-9_-]+?) for modid ([a-zA-Z0-9_-]+?)";
    }

    @Override
    public Message message(Matcher matcher) {
        String modID = matcher.group(3);

        return new Message(
                MessageType.ERROR,
                MessageCategory.MOD_BROKEN,
                "Mod '%s' failed to load".formatted(modID),
                "Mod '%s' has failed to load the configuration file '%s'".formatted(modID, matcher.group(1))
        );
    }
}
