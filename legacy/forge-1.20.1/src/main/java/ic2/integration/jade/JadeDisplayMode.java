package ic2.integration.jade;

public enum JadeDisplayMode {
    ALWAYS,
    SHIFT,
    NEVER;

    public boolean isVisible(boolean showDetails) {
        return switch (this) {
            case ALWAYS -> true;
            case SHIFT -> showDetails;
            case NEVER -> false;
        };
    }
}
