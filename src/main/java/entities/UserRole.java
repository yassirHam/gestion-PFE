package entities;

/**
 * Role-based access. Each {@link AppUser} is assigned exactly one role.
 *
 * <ul>
 *   <li>{@link #ADMIN_PEDAGOGIQUE} – full access. Can create sessions, force
 *       overrides, freeze/publish, manage users.</li>
 *   <li>{@link #CHEF_DEPARTEMENT} – approves planning versions, validates
 *       affectations, can override but not configure tenant.</li>
 *   <li>{@link #COORDINATEUR_FILIERE} – owns one or more filières, can edit
 *       affectations and request validation.</li>
 *   <li>{@link #PROFESSEUR} – read access + can declare unavailability +
 *       respond to convocations.</li>
 *   <li>{@link #CONSULTATION} – read-only access (jury members of external
 *       institutions, secretaries, etc.).</li>
 * </ul>
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

    /** Read-write privileges on most operational data. */
    public boolean canEdit() {
        return this == ADMIN_PEDAGOGIQUE
                || this == CHEF_DEPARTEMENT
                || this == COORDINATEUR_FILIERE;
    }

    /** Right to validate / approve / reject the work of others. */
    public boolean canApprove() {
        return this == ADMIN_PEDAGOGIQUE || this == CHEF_DEPARTEMENT;
    }

    /** Right to publish (freeze) and archive a planning version. */
    public boolean canPublish() {
        return this == ADMIN_PEDAGOGIQUE || this == CHEF_DEPARTEMENT;
    }

    /** Right to manage users and global settings. */
    public boolean canManageTenant() {
        return this == ADMIN_PEDAGOGIQUE;
    }

    public boolean canDeclareUnavailability() {
        return this != CONSULTATION;
    }

    public static UserRole fromString(String value) {
        if (value == null) return CONSULTATION;
        try { return UserRole.valueOf(value.trim().toUpperCase()); }
        catch (Exception e) { return CONSULTATION; }
    }
}
