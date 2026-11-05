package pl.olafcio.nocrash.crashpad.modscanner;

import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public record Mod(
        String id,

        String name,
        String description,
        String author,

        String version,

        JsonObject dependencies,

        Platform platform,

        List<String> accessModifiers
) {}
