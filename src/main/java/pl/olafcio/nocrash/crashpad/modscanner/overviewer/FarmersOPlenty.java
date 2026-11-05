package pl.olafcio.nocrash.crashpad.modscanner.overviewer;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.modscanner.IModOverviewer;
import pl.olafcio.nocrash.crashpad.modscanner.ModFile;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public final class FarmersOPlenty implements IModOverviewer {
    @Override
    public Collection<Message> overview(List<ModFile> modFiles) {
        var farmersoplenty = modFiles.stream().filter(file -> file.mod().id().equals("farmersoplenty")).findAny();
        if (farmersoplenty.isPresent()) {
            return List.of(
                    new Message(
                            MessageType.INFO,
                            MessageCategory.MOD_INCOMPATIBILITY,
                            "Farmers O' Plenty is installed. You may need to change its version or remove it.",
                            "Farmers O' Plenty is known for notoriously causing issues.",
                            farmersoplenty.get().path()
                    )
            );
        }

        return List.of();
    }
}
