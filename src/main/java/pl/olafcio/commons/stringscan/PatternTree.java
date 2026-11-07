package pl.olafcio.commons.stringscan;

import java.util.ArrayList;
import java.util.HashMap;

class PatternTree extends HashMap<String, PatternTree> {
    private Pattern pattern;
    private final Integer index;

    public PatternTree() {
        this.pattern = null;
        this.index = null;
    }

    public PatternTree(Pattern pattern) {
        this.pattern = pattern;
        this.index = null;
    }

    public PatternTree(Pattern pattern, Integer index) {
        this.pattern = pattern;
        this.index = index;
    }

    //===========//
    //== INDEX ==//
    //===========//

    public Integer getIndex() {
        return index;
    }

    //=============//
    //== PATTERN ==//
    //=============//

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        if (this.pattern != null)
            throw new UnsupportedOperationException("Cannot redefine pattern");

        this.pattern = pattern;
    }

    //===============//
    //== MINLENGTH ==//
    //===============//

    @Override
    public PatternTree put(String key, PatternTree value) {
        var oldValue = super.put(key, value);

        this.calculateMinLength();

        return oldValue;
    }

    private int minLength = 0;
    private int minKey = 0;

    /**
     * Returns the recursive minimum length required to perform a lookup for any {@linkplain PatternTree} with a set {@linkplain Pattern}.
     */
    public int getMinLength() {
        return minLength;
    }

    /**
     * Returns the non-recursive minimum length of a key in the current state.
     */
    public int getKeyLength() {
        return minKey;
    }

    private void calculateMinLength() {
        this.minKey = keySet().stream().mapToInt(String::length).min().orElse(0);
        this.minLength = entrySet().stream().mapToInt(entry -> {
            int value = entry.getKey().length();

            if (entry.getValue().pattern == null) {
                value += entry.getValue().getMinLength();
            }

            return value;
        }).min().orElse(0);
    }

    //==========//
    //== MISC ==//
    //==========//

    private record ReducingEntry(PatternTree tree, ArrayList<Entry<String, PatternTree>> entries) {}

    /**
     * Optimizes the tree.
     */
    public void reduce() {
        if (isEmpty())
            return;

        while (true) {
            var newPatterns = new HashMap<String, ReducingEntry>();
            var anything = false;

            for (var entry : this.entrySet()) {
                var ch = entry.getKey().substring(0, 1);

                if (newPatterns.containsKey(ch)) {
                    newPatterns.get(ch).entries.add(entry);
                    anything = true;
                } else {
                    var entries = new ArrayList<Entry<String, PatternTree>>();

                    entries.add(entry);

                    newPatterns.put(ch, new ReducingEntry(new PatternTree(), entries));
                }
            }

            if (!anything)
                break;

            for (var fork : newPatterns.entrySet()) {
                var ch = fork.getKey();
                var map2 = fork.getValue().tree();
                var entryList = fork.getValue().entries();

                for (var entry : entryList) {
                    var key = entry.getKey();

                    this.remove(key);

                    if (key.length() == 1) {
                        map2.setPattern(entry.getValue().getPattern());
                    } else {
                        map2.put(key.substring(1), entry.getValue());
                    }
                }

                map2.reduce();

                super.put(ch, map2);
            }
        }

        this.calculateMinLength();
    }
}
