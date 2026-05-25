package entities;

/**
 * Operator category retained as audit/governance metadata.
 *
 * <p>These values are no longer used for authentication or access control.</p>
 */
public enum UserRole {
    ADMIN_PEDAGOGIQUE,
    CHEF_DEPARTEMENT,
    COORDINATEUR_FILIERE,
    PROFESSEUR,
    CONSULTATION;

    public String getLabel() {
        switch (this) {
            case ADMIN_PEDAGOGIQUE:    return "Administration pédagogique";
            case CHEF_DEPARTEMENT:     return "Chef de département";
            case COORDINATEUR_FILIERE: return "Coordinateur de filière";
            case PROFESSEUR:           return "Professeur";
            case CONSULTATION:         return "Consultation";
        }
        return name();
    }

    public static UserRole fromString(String value) {
        if (value == null) return CONSULTATION;
        try { return UserRole.valueOf(value.trim().toUpperCase()); }
        catch (Exception e) { return CONSULTATION; }
    }
}
