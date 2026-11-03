package pl.olafcio.nocrash;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

class NoCrashDeps {
    static Path download() {
        var gson = Path.of("gson-2.15.0.jar");

        if (!Files.isRegularFile(gson)) {
            final URLConnection conn;

            try {
                conn = URI.create("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.15.0/gson-2.15.0.jar")
                          .toURL()
                          .openConnection();
            } catch (IOException e) {
                throw new RuntimeException("Failed to download GSON for NoCrash crashpad (failed to open)", e);
            }

            conn.setDoInput(true);

            try {
                conn.connect();
            } catch (IOException e) {
                throw new RuntimeException("Failed to download GSON for NoCrash crashpad (failed to connect)", e);
            }

            try (var stream = new FileOutputStream(gson.toFile())) {
                conn.getInputStream().transferTo(stream);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to download GSON for NoCrash crashpad (failed to save)", e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to download GSON for NoCrash crashpad (failed to transfer)", e);
            }
        }

        return gson;
    }
}
