package entities;

/**
 * Validation lifecycle applicable to affectations and planning versions.
 *
 * <p>Transitions are governed by {@code services.ApprovalWorkflowService}:</p>
 * <pre>
 *   DRAFT -> PENDING_VALIDATION -> VALIDATED -> PUBLISHED -> ARCHIVED
 *           \-----> REJECTED (back to DRAFT) <----/
 * </pre>
 */
public enum LifecycleState {
    DRAFT,
    PENDING_VALIDATION,
    VALIDATED,
    PUBLISHED,
    ARCHIVED,
    REJECTED;

    public boolean isEditable() {
        return this == DRAFT || this == REJECTED;
    }

    /** Once published, the artefact is frozen (no jury swap, no re-plan, no delete). */
    public boolean isFrozen() {
        return this == PUBLISHED || this == ARCHIVED;
    }

    public String getBootstrap() {
        switch (this) {
            case DRAFT:              return "secondary";
            case PENDING_VALIDATION: return "info";
            case VALIDATED:          return "primary";
            case PUBLISHED:          return "success";
            case ARCHIVED:           return "dark";
            case REJECTED:           return "danger";
        }
        return "secondary";
    }

    public String getLabel() {
        switch (this) {
            case DRAFT:              return "Brouillon";
            case PENDING_VALIDATION: return "En attente de validation";
            case VALIDATED:          return "Validé";
            case PUBLISHED:          return "Publié";
            case ARCHIVED:           return "Archivé";
            case REJECTED:           return "Rejeté";
        }
        return name();
    }
}
