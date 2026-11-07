package pl.olafcio.commons.stringscan;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

public class Pattern {
    private final String pattern;

    public Pattern(String pattern) {
        this.pattern = pattern;
    }

    public String pattern() {
        return pattern;
    }

    public void action(Supplier<InputStream> supplier) throws IOException {}

    public boolean shouldReturnInFind() {
        return true;
    }
}
