package entities;

/**
 * Event type captured by the audit trail. Every state-changing operation
 * goes through {@code services.AuditService.record(...)}.
 */
public enum AuditAction {

    // Legacy access events retained so older audit rows can still be read.
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,

    // Legacy operator-management events retained for historical audit rows.
    USER_CREATED,
    USER_UPDATED,
    USER_DEACTIVATED,
    USER_PASSWORD_CHANGED,

    // Sessions / versions
    SESSION_CREATED,
    SESSION_ACTIVATED,
    SESSION_CLOSED,
    VERSION_CREATED,
    VERSION_SUBMITTED,
    VERSION_VALIDATED,
    VERSION_PUBLISHED,
    VERSION_ARCHIVED,
    VERSION_REJECTED,
    VERSION_FROZEN,

    // Affectations
    AFFECTATION_CREATED,
    AFFECTATION_FORCED,
    AFFECTATION_DELETED,
    AFFECTATION_LOCKED,
    AFFECTATION_VALIDATED,

    // Soutenances / juries
    SOUTENANCE_GENERATED,
    SOUTENANCE_LOCKED,
    SOUTENANCE_UNLOCKED,
    SOUTENANCE_CANCELLED,
    SOUTENANCE_POSTPONED,
    SOUTENANCE_REPLANNED,
    SALLE_REPLACED,
    JURY_MEMBER_SWAPPED,
    JURY_REPLACED,

    // Exceptions
    EXCEPTION_REPORTED,
    EXCEPTION_RESOLVED,

    // Communication
    NOTIFICATION_SENT,
    REMINDER_SENT,

    // Governance
    DEPARTMENT_CREATED,
    DEPARTMENT_UPDATED,
    PROF_AVAILABILITY_DECLARED,
    PROF_EXCLUDED,
    PROF_REINSTATED,

    // Storage / config
    SETTINGS_CHANGED,
    DATA_IMPORTED,
    DATA_DELETED,
    HISTORY_RESTORED;

    public String getLabel() {
        return name().toLowerCase().replace('_', ' ');
    }
}
