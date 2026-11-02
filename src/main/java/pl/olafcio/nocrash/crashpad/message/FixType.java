package pl.olafcio.nocrash.crashpad.message;

public enum FixType {
    UNAVAILABLE("A fix is not available"),
    DELETE("Delete", "This will delete the bad mod.", "Delete mod");

    private final String title;
    private final String desc;
    private final String tooltip;

    FixType(String title, String description, String tooltip) {
        this.title = title;
        this.desc = description;
        this.tooltip = tooltip;
    }

    FixType(String tooltip) {
        this.tooltip = tooltip;
        this.title = null;
        this.desc = null;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return desc;
    }

    public String getTooltip() {
        return tooltip;
    }
}
