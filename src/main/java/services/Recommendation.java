package services;

/**
 * A piece of feedback shown to the user before/after planning generation.
 * Represents either an information note, a warning, or a blocking error with
 * a suggested fix.
 */
public class Recommendation {

    public enum Type { INFO, WARNING, ERROR }

    private final Type type;
    private final String title;
    private final String message;
    private final String suggestion;

    public Recommendation(Type type, String title, String message, String suggestion) {
        this.type = type == null ? Type.INFO : type;
        this.title = title;
        this.message = message;
        this.suggestion = suggestion;
    }

    public static Recommendation info(String title, String message) {
        return new Recommendation(Type.INFO, title, message, null);
    }

    public static Recommendation warning(String title, String message, String suggestion) {
        return new Recommendation(Type.WARNING, title, message, suggestion);
    }

    public static Recommendation error(String title, String message, String suggestion) {
        return new Recommendation(Type.ERROR, title, message, suggestion);
    }

    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getSuggestion() { return suggestion; }

    public String getBootstrapClass() {
        switch (type) {
            case ERROR:   return "danger";
            case WARNING: return "warning";
            default:      return "info";
        }
    }

    public String getIconClass() {
        switch (type) {
            case ERROR:   return "fa-circle-xmark";
            case WARNING: return "fa-triangle-exclamation";
            default:      return "fa-circle-info";
        }
    }
}
