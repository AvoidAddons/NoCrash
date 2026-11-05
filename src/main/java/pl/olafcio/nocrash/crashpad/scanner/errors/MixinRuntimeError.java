package pl.olafcio.nocrash.crashpad.scanner.errors;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MixinRuntimeError implements IScanner {
    @Override
    public String regex() {
        return "Unreported exception thrown!\\n*(.*?)\\n((\\tat .*?\\n)+)";
    }

    @Override
    public Message message(Matcher matcher) {
        String[] stack = matcher.group(2).split("\n");
        Pattern pattern = Pattern.compile("handler\\$(.+?)\\$(.+)\\$");

        var matches = Arrays.stream(stack)
                            .map(pattern::matcher)
                            .filter(Matcher::find)
                            .toList();

        var children = new HashMap<String, Message>();

        for (int i = 1; i < matches.size(); i++) {
            var match = matches.get(i);
            var modID = match.group(2);

            children.put(modID, new Message(
                    MessageType.WARNING,
                    MessageCategory.MOD_INCOMPATIBILITY,
                    "Mod '%s' has been mentioned lower".formatted(modID),
                    match.group(1)
            ));
        }

        return new Message(
                MessageType.ERROR,
                MessageCategory.MOD_INCOMPATIBILITY,
                "Mod '%s' has crashed within a mixin".formatted(Optional.ofNullable(matches.getFirst())
                                                                        .orElseThrow(() -> new RuntimeException("Failed to find mods in the stacktrace of a mixin issue"))
                                                                        .group(2)),
                matches.getFirst().group(1),
                children.values()
        );
    }
}
