package pl.olafcio.nocrash.crashpad.scanner.warnings;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.scanner.IScanner;

import java.util.Arrays;
import java.util.regex.Matcher;

public final class FMLSuspectedMods implements IScanner {
    @Override
    public String regex() {
        return "Suspected Mods: *\\n(((\\t| ).*\\n)+)";
    }

    @Override
    public Message message(Matcher matcher) {
        var children = Arrays.stream(matcher.group(1)
                             .split("\\n[\\t ](?=[A-Za-z])"))
                             .map(str -> str.split(",")[0])
                             .map(str ->
                                     new Message(
                                            MessageType.WARNING,
                                            MessageCategory.OTHER,
                                            str,
                                            ""
                                    )
                             )
                             .toList();

        return new Message(
                MessageType.WARNING,
                MessageCategory.OTHER,
                "Forge/NeoForge suspects the following mods to be the culprit:",
                "Suspected mods",
                children
        );
    }
}
