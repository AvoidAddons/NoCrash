package pl.olafcio.nocrash.crashpad.modscanner.overviewer;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.modscanner.IModOverviewer;
import pl.olafcio.nocrash.crashpad.modscanner.ModFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public final class DuplicateModDetector implements IModOverviewer {
    @Override
    public Collection<Message> overview(List<ModFile> modFiles) {
        var set = new HashSet<String>();
        var out = new ArrayList<Message>();

        for (var mod : modFiles) {
            if (!set.add(mod.mod().id())) {
                out.add(new Message(
                        MessageType.CRITICAL,
                        MessageCategory.OTHER,
                        "You have a duplicate mod: '%s' (%s)".formatted(mod.path().getFileName().toString(), mod.mod().id()),
                        "",
                        mod.path()
                ));
            }
        }

        return out;
    }
}
