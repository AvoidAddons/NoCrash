package pl.olafcio.nocrash.crashpad.modscanner.overviewer;

import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.modscanner.IModOverviewer;
import pl.olafcio.nocrash.crashpad.modscanner.ModFile;

import java.util.Collection;
import java.util.List;

public final class CreateAndStarlight implements IModOverviewer {
    @Override
    public Collection<Message> overview(List<ModFile> modFiles) {
        if (
                modFiles.stream().anyMatch(file -> file.mod().id().equals("create")) &&
                modFiles.stream().anyMatch(file -> file.mod().id().equals("starlight"))
        ) {
            return List.of(
                    new Message(
                            MessageType.INFO,
                            MessageCategory.MOD_INCOMPATIBILITY,
                            "Remove Starlight to fix incompatibility with Create",
                            "Create and Starlight are both present."
                    )
            );
        }

        return List.of();
    }
}
