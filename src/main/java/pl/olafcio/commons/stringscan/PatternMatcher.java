package pl.olafcio.commons.stringscan;

import pl.olafcio.commons.functions.ThrowableSupplier;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Efficient multi-pattern matching algorithm.
 */
public class PatternMatcher<E extends Throwable> implements AutoCloseable {
    private final FileInputStream stream;
    private final PatternTree tree;
    private final ThrowableSupplier<FileInputStream, E> streamSupplier;

    public PatternMatcher(PatternSetup context, ThrowableSupplier<FileInputStream, E> streamSupplier) throws E {
        this.streamSupplier = streamSupplier;
        this.stream = streamSupplier.get();
        this.tree = new PatternTree();

        int i = 0;

        for (var pattern : context.getPatterns()) {
            tree.put(pattern.pattern(), new PatternTree(pattern, i++));
        }

        tree.reduce();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    //==========//
    //== SCAN ==//
    //==========//

    public void scan() throws IOException {
        while (find() != null);
    }

    //==========//
    //== FIND ==//
    //==========//

    public Integer find() throws IOException {
        return new Finder().find(tree, new byte[tree.getMinLength()], stream, 0);
    }

    private class Finder {
        private long total = 0;

        private Integer find(PatternTree tree, byte[] data, FileInputStream stream, int read) throws IOException {
            while (true) {
                if (read == data.length) {
                    var str = new String(data, StandardCharsets.UTF_8);

                    for (int offset = 0; offset < Math.max(1, str.length() - tree.getKeyLength()); offset++) {
                        var key = str.substring(offset, offset + tree.getKeyLength());
                        var nest = str.substring(offset + tree.getKeyLength());

                        var subtree = tree.get(key);
                        if (subtree != null) {
                            byte[] add = nest.getBytes(StandardCharsets.UTF_8);
                            byte[] copy = new byte[subtree.getMinLength()];

                            System.arraycopy(add, 0, copy, 0, Math.min(add.length, copy.length));

                            if (!subtree.isEmpty()) {
                                var value = new Finder().find(subtree, copy, newStream("Failed to supply stream for subtree (wrapped exception)"), add.length);
                                if (value != null)
                                    return value;
                            }

                            var pattern = subtree.getPattern();
                            if (pattern != null) {
                                pattern.action(() -> {
                                    return newStream("Failed to supply stream within Pattern#action(...) (wrapped exception)");
                                });

                                if (pattern.shouldReturnInFind())
                                    return subtree.getIndex();
                            }
                        }
                    }

                    read = 0;
                }

                int value = stream.read(data, read, data.length - read);
                if (value == -1)
                    break;

                if (data[data.length - 1] == '\r') {
                    var pos = stream.getChannel().position();
                    var next = stream.read();

                    if (next == '\n') {
                        data[data.length - 1] = '\n';

                        total++;
                    } else if (next != -1) {
                        stream.getChannel().position(pos);
                    }
                }

                read += value;
                total += value;
            }

            return null;
        }

        private FileInputStream newStream(String message) {
            try {
                var stream2 = streamSupplier.get();
                var left = total;

                while (left > 0)
                    left -= stream2.skip(left);

                return stream2;
            } catch (Throwable e) {
                throw new RuntimeException(message, e);
            }
        }
    }
}
