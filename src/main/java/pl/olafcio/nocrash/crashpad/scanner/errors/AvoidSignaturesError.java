package pl.olafcio.nocrash.crashpad.scanner.errors;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;

public final class AvoidSignaturesError implements IScanner {
    @Override
    public String regex() {
        return "Failed to verify Avoid mod signatures: (.+)";
    }

    @Override
    public Message message(Matcher matcher) {
        var path = matcher.group(1);

        return new Message(
                MessageType.ERROR,
                MessageCategory.MOD_BROKEN,
                "Mod '%s' is broken".formatted(Arrays.stream(path.replace("\\", "/").split("/")).toList().getLast()),
                "Mod '%s' has an invalid signatures file".formatted(Arrays.stream(path.replace("\\", "/").split("/")).toList().getLast()),
                Path.of(path)
        );
    }
}
