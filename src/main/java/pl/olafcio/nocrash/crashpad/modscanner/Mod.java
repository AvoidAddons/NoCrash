package pl.olafcio.nocrash.crashpad.modscanner;

import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Mod(
        String id,

        String name,
        String description,
        String author,

        String version,

        JsonObject dependencies,

        Platform platform
) {}
