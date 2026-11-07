package pl.olafcio.nocrash.crashpad.logs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Log {
    private final Path   path;
    private       String content;

    public Log(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public String getContent() throws IOException {
        if (content == null) {
            content = Files.readString(path, StandardCharsets.UTF_8);
            content = content.replaceAll("\\r\\n|\\r", "\n");
        }

        return content;
    }

    public FileInputStream openInputStream() throws FileNotFoundException {
        return new FileInputStream(path.toFile());
    }

    public String censor() throws IOException {
        return new LogCensor(getContent()).censor();
    }
}
