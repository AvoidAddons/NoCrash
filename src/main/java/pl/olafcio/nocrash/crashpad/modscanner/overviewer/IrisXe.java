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

public final class IrisXe implements IModOverviewer {
    @Override
    public Collection<Message> overview(List<ModFile> modFiles) {
        if (
                //FIXME Wtf is this if
                (State.minecraft.equals("1.20.1") || State.minecraft.equals("1.20.2") || State.minecraft.equals("1.20.3") ||
                 State.minecraft.equals("1.20.4") || State.minecraft.equals("1.20.5") || State.minecraft.equals("1.20.6")) &&
                modFiles.stream().anyMatch(file -> file.mod().id().equals("iris"))
        ) {
            return List.of(
                    new Message(
                            MessageType.INFO,
                            MessageCategory.DRIVER_ISSUE,
                            "Install https://modrinth.com/mod/twilight-iris-xe-fix to fix incompatibility between Iris and Intel Xe processors",
                            "Iris is broken on Intel Xe drivers. If you have them, the 'Twilight Iris Xe Fix' mod may solve this issue.",
                            URI.create("https://cdn.modrinth.com/data/TbnFK9Rf/versions/RcSXUL32/twilight_iris_xe_fix-1.0.0.jar?mr_download_reason=standalone")
                    )
            );
        }

        return List.of();
    }
}
