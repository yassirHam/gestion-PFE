package services;

/**
 * Describes one violation of a {@link Constraint} encountered during planning
 * generation or post-validation.
 */
public class ConstraintViolation {

    private final String constraintId;
    private final String constraintLabel;
    private final ConstraintPriority priority;
    private final String detail;
    private final String suggestion;

    public ConstraintViolation(String constraintId, String constraintLabel, ConstraintPriority priority,
                               String detail, String suggestion) {
        this.constraintId = constraintId;
        this.constraintLabel = constraintLabel;
        this.priority = priority == null ? ConstraintPriority.SOFT : priority;
        this.detail = detail;
        this.suggestion = suggestion;
    }

    public String getConstraintId() { return constraintId; }
    public String getConstraintLabel() { return constraintLabel; }
    public ConstraintPriority getPriority() { return priority; }
    public String getSeverity() { return priority == ConstraintPriority.HARD ? "CRITIQUE" : "ALERTE"; }
    public String getDetail() { return detail; }
    public String getSuggestion() { return suggestion; }
    public boolean isHard() { return priority == ConstraintPriority.HARD; }

    public String getBootstrapClass() {
        return priority == ConstraintPriority.HARD ? "danger" : "warning";
    }

    public String getIconClass() {
        return priority == ConstraintPriority.HARD ? "fa-circle-xmark" : "fa-circle-exclamation";
    }
}
