package pl.olafcio.nocrash.crashpad.modscanner;

import pl.olafcio.nocrash.crashpad.message.Message;

import java.util.Collection;
import java.util.List;

public interface IModOverviewer {
    Collection<Message> overview(List<ModFile> modFiles);
}
