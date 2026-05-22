package entities;

/**
 * Operational exceptions that can disturb a published planning. Each
 * declared incident is stored as a {@link SoutenanceException} record so
 * the team can track it and the {@code ExceptionManagementService} can
 * propose a resolution (jury replacement, slot postponement, room swap).
 */
public enum ExceptionType {
    STUDENT_ABSENT,
    PROFESSOR_ABSENT,
    JURY_REPLACEMENT_NEEDED,
    ROOM_UNAVAILABLE,
    SUBJECT_CHANGED,
    DELAY,
    CANCELLED,
    OTHER;

    public String getLabel() {
        switch (this) {
            case STUDENT_ABSENT:          return "Étudiant absent";
            case PROFESSOR_ABSENT:        return "Professeur absent";
            case JURY_REPLACEMENT_NEEDED: return "Remplacement de jury demandé";
            case ROOM_UNAVAILABLE:        return "Salle indisponible";
            case SUBJECT_CHANGED:         return "Sujet modifié";
            case DELAY:                   return "Soutenance retardée";
            case CANCELLED:               return "Soutenance annulée";
            default:                      return "Autre";
        }
    }
}
