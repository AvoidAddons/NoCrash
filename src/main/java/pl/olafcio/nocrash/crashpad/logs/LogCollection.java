package pl.olafcio.nocrash.crashpad.logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class LogCollection {
    private final List<Log> files;
    private final Map<LogType, Log> types;

    public LogCollection() {
        this.files = new ArrayList<>();
        this.types = new HashMap<>();
    }

    public List<Log> getLogs() {
        return files;
    }

    public Map<LogType, Log> getLogTypes() {
        return types;
    }

    public boolean isLogPresent(LogType type) {
        return types.containsKey(type);
    }

    public Log getLog(LogType type) {
        return types.get(type);
    }

    public Log addLog(Path path) {
        var log = new Log(path);

        files.add(log);

        return log;
    }

    public Log addLog(Path path, UnaryOperator<String> additionalCensor) {
        var log = new Log(path) {
            @Override
            public String censor() throws IOException {
                return additionalCensor.apply(super.censor());
            }
        };

        files.add(log);

        return log;
    }

    public void putLog(LogType logType, Path path) {
        IO.println("[LogCollection/INFO] Adding log '%s' as %s".formatted(path.toAbsolutePath().toString(), logType.name()));
        types.put(logType, addLog(path));
    }

    public void putLog(LogType logType, Path path, UnaryOperator<String> additionalCensor) {
        types.put(logType, addLog(path, additionalCensor));
    }

    public void putLogIfExists(LogType logType, Path path) {
        if (!Files.isDirectory(path) && Files.exists(path)) {
            putLog(logType, path);
        }
    }

    public void forLogs(LogType[] logTypes, Consumer<Log> consumer) {
        for (var type : logTypes) {
            var log = getLog(type);
            if (log != null)
                consumer.accept(log);
        }
    }

    public <R> R forLogs(LogType[] logTypes, Function<Log, R> consumer) {
        for (var type : logTypes) {
            var log = getLog(type);
            if (log != null) {
                var ret = consumer.apply(log);
                if (ret != null)
                    return ret;
            }
        }

        return null;
    }
}
