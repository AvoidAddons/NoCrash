package pl.olafcio.nocrash.crashpad.modscanner.mapper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import pl.olafcio.nocrash.crashpad.modscanner.IModMapper;
import pl.olafcio.nocrash.crashpad.modscanner.Mod;
import pl.olafcio.nocrash.crashpad.modscanner.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;

public final class AvoidMS implements IModMapper {
    @Override
    public Mod[] map(JarFile jar) {
        var entry = jar.getEntry("avoid.mod.json");
        if (entry == null)
            return new Mod[]{};

        final JsonObject json;

        try (var stream = jar.getInputStream(entry)) {
            json = new Gson().fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze Avoid mod", e);
        }

        return new Mod[]{
                new Mod(
                        json.get("id").getAsString(),
                        Objects.requireNonNullElse(json.get("name"), new JsonPrimitive("---MISSING---")).getAsString(),
                        Objects.requireNonNullElse(json.get("description"), new JsonPrimitive("---MISSING---")).getAsString(),
                        Objects.requireNonNullElse(json.get("author"), new JsonPrimitive("---MISSING---")).getAsString(),
                        Objects.requireNonNullElse(json.get("version"), new JsonPrimitive("---MISSING---")).getAsString(),
                        Objects.requireNonNullElse(json.get("dependencies"), new JsonObject()).getAsJsonObject(),
                        Platform.AVOID,
                        List.of()  // no AW-s in Avoid yet :(
                )
        };
    }
}
