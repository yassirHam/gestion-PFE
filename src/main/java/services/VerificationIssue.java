package services;

public class VerificationIssue {
    private final String severity;
    private final String category;
    private final String title;
    private final String detail;

    public VerificationIssue(String severity, String category, String title, String detail) {
        this.severity = severity;
        this.category = category;
        this.title = title;
        this.detail = detail;
    }

    public String getSeverity() {
        return severity;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getBootstrapClass() {
        if ("CRITIQUE".equals(severity)) return "danger";
        if ("ALERTE".equals(severity)) return "warning";
        return "info";
    }

    public String getIconClass() {
        if ("CRITIQUE".equals(severity)) return "fa-triangle-exclamation";
        if ("ALERTE".equals(severity)) return "fa-circle-exclamation";
        return "fa-circle-info";
    }
}
