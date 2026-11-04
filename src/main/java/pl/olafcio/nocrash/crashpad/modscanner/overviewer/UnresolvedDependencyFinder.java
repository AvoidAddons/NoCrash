package pl.olafcio.nocrash.crashpad.modscanner.overviewer;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.modscanner.IModOverviewer;
import pl.olafcio.nocrash.crashpad.modscanner.ModFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class UnresolvedDependencyFinder implements IModOverviewer {
    @Override
    public Collection<Message> overview(List<ModFile> modFiles) {
        var ids = new ArrayList<String>();
        var out = new ArrayList<Message>();

        ids.add("java");
        ids.add("minecraft");
        ids.add("fabricloader");
        ids.add("neoforge");
        ids.add("forge");
        ids.add("spongeapi");

        for (var file : modFiles)
            ids.add(file.mod().id());

        for (var mod : modFiles) {
            for (var depend : mod.mod().dependencies().keySet())
                if (!ids.contains(depend)) {
                    out.add(new Message(
                            MessageType.CRITICAL,
                            MessageCategory.OTHER,
                            "You have a missing dependency: '%s'".formatted(depend)
                    ));
                }
        }

        return out;
    }
}
