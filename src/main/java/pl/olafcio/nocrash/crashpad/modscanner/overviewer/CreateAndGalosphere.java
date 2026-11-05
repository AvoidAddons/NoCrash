package pl.olafcio.nocrash.crashpad.modscanner.overviewer;

import pl.olafcio.nocrash.crashpad.State;
import pl.olafcio.nocrash.crashpad.message.Message;
import pl.olafcio.nocrash.crashpad.message.MessageCategory;
import pl.olafcio.nocrash.crashpad.message.MessageType;
import pl.olafcio.nocrash.crashpad.modscanner.IModOverviewer;
import pl.olafcio.nocrash.crashpad.modscanner.ModFile;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public final class CreateAndGalosphere implements IModOverviewer {
    @Override
    public Collection<Message> overview(List<ModFile> modFiles) {
        if (
                State.minecraft.equals("1.20.1") &&
                modFiles.stream().anyMatch(file -> file.mod().id().equals("create")) &&
                modFiles.stream().anyMatch(file -> file.mod().id().equals("galosphere"))
        ) {
            return List.of(
                    new Message(
                            MessageType.INFO,
                            MessageCategory.MOD_INCOMPATIBILITY,
                            "Install https://modrinth.com/mod/create-repair to fix incompatibility between Create and Galosphere 1.5.x",
                            "Create and Galosphere are both present. The 'Create Repair' mod may solve this issue.",
                            URI.create("https://cdn.modrinth.com/data/G3K7UdxY/versions/vad6B14j/create_repair-fabric-1.20.1-1.3.0.jar?mr_download_reason=standalone")
                    )
            );
        }

        return List.of();
    }
}
