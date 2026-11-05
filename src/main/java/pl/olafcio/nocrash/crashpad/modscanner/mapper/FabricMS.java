package pl.olafcio.nocrash.crashpad.modscanner.mapper;

import com.google.gson.*;
import pl.olafcio.nocrash.crashpad.modscanner.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;

public final class FabricMS implements IModMapper {
    private static final Path EXPORT_DIRECTORY
                       = Path.of(".nocrash/jar_in_jar/");

    {
        try {
            Files.createDirectories(EXPORT_DIRECTORY);
        } catch (IOException ignored) {
            // I guess we'll fuck all fabric mods then
        }
    }

    @Override
    public List<ModFile> map(JarFile jar, Path mod) {
        var entry = jar.getEntry("fabric.mod.json");
        if (entry == null)
            return List.of();

        final JsonObject json;

        try (var stream = jar.getInputStream(entry)) {
            json = new Gson().fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to analyze Fabric mod", e);
        }

        String aw = null;

        if (json.has("accessWidener"))
            aw = json.get("accessWidener").getAsString();

        if (json.has("classTweaker"))
            aw = json.get("classTweaker").getAsString();

        var mods = new ArrayList<ModFile>();

        mods.add(new ModFile(new Mod(
                json.get("id").getAsString(),
                Objects.requireNonNullElse(json.get("name"), new JsonPrimitive("---MISSING---")).getAsString(),
                Objects.requireNonNullElse(json.get("description"), new JsonPrimitive("---MISSING---")).getAsString(),
                Objects.requireNonNullElse(json.get("author"), new JsonPrimitive("---MISSING---")).getAsString(),
                Objects.requireNonNullElse(json.get("version"), new JsonPrimitive("---MISSING---")).getAsString(),
                Objects.requireNonNullElse(json.get("depends"), new JsonObject()).getAsJsonObject(),
                Platform.FABRIC,
                aw == null ? List.of() : List.of(aw)
        ), mod, jar));

        var jarsp = json.get("jars");
        if (jarsp instanceof JsonArray jars) {
            for (var object : jars) {
                try {
                    if (object.isJsonObject())
                        mods.addAll(map(nest(jar, object.getAsJsonObject().get("file").getAsString()), mod));
                    else if (object.isJsonPrimitive())
                        mods.addAll(map(nest(jar, object.getAsString()), mod));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return mods;
    }

    private JarFile nest(JarFile parent, String path) {
        var entry = parent.getJarEntry(path);
        var disk = EXPORT_DIRECTORY.resolve(path.substring(path.lastIndexOf("/") + 1) + JIJState.jij++ + ".jar").toFile();

        try (var stream = parent.getInputStream(entry)) {
            try (var out = new FileOutputStream(disk)) {
                stream.transferTo(out);
            }

            return new JarFile(disk);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export JIJ of mod: '%s' (nested: '%s')  /IOException".formatted(parent, path), e);
        }
    }
}
