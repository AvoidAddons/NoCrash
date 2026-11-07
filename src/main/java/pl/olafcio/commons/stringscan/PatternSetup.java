package pl.olafcio.commons.stringscan;

import pl.olafcio.commons.functions.ThrowableConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PatternSetup {
    private final List<Pattern> patterns
       = new ArrayList<>();

    public void addPattern(Pattern pattern) {
        patterns.add(pattern);
    }

    public void addPattern(String pattern) {
        patterns.add(new Pattern(pattern));
    }

    public void addPattern(String pattern, ThrowableConsumer<Supplier<InputStream>, IOException> action) {
        patterns.add(new Pattern(pattern) {
            @Override
            public void action(Supplier<InputStream> supplier) throws IOException {
                action.accept(supplier);
            }
        });
    }

    List<Pattern> getPatterns() {
        return patterns;
    }
}
