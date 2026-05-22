package entities;

/**
 * Operational status of a single soutenance (independent from the
 * lifecycle of the planning version that hosts it). Allows administrators
 * to lock, cancel, postpone or replan individual sessions without
 * touching the rest of the planning.
 */
public enum SoutenanceStatus {
    PLANNED,
    LOCKED,
    CANCELLED,
    POSTPONED,
    COMPLETED,
    JURY_REPLACED;

    public boolean isLocked() {
        return this == LOCKED || this == COMPLETED;
    }

    public boolean isActionable() {
        return this == PLANNED || this == POSTPONED || this == JURY_REPLACED;
    }

    public String getBootstrap() {
        switch (this) {
            case PLANNED:       return "primary";
            case LOCKED:        return "warning";
            case CANCELLED:     return "danger";
            case POSTPONED:     return "info";
            case COMPLETED:     return "success";
            case JURY_REPLACED: return "secondary";
        }
        return "secondary";
    }

    public String getLabel() {
        switch (this) {
            case PLANNED:       return "Planifié";
            case LOCKED:        return "Verrouillé";
            case CANCELLED:     return "Annulé";
            case POSTPONED:     return "Reporté";
            case COMPLETED:     return "Terminé";
            case JURY_REPLACED: return "Jury remplacé";
        }
        return name();
    }
}
