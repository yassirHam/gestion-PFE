package entities;

/**
 * Academic grade of a professor. Used for grade-based jury constraints
 * (e.g. require at least one PES/PH on each jury, prevent ranks below
 * VAC from chairing).
 */
public enum ProfesseurGrade {
    PES,    // Professeur Enseignement Supérieur
    PH,     // Professeur Habilité
    PA,     // Professeur Assistant
    MC,     // Maître de Conférences
    MA,     // Maître Assistant
    VAC,    // Vacataire
    EXTERNE,// External / industry guest
    AUTRE;

    public int getSeniorityScore() {
        switch (this) {
            case PES: return 100;
            case PH:  return 90;
            case MC:  return 80;
            case PA:  return 70;
            case MA:  return 60;
            case EXTERNE: return 50;
            case VAC: return 40;
            default:  return 10;
        }
    }

    public String getLabel() {
        switch (this) {
            case PES: return "PES — Professeur Enseignement Supérieur";
            case PH:  return "PH — Professeur Habilité";
            case PA:  return "PA — Professeur Assistant";
            case MC:  return "MC — Maître de Conférences";
            case MA:  return "MA — Maître Assistant";
            case VAC: return "VAC — Vacataire";
            case EXTERNE: return "Externe / Industriel";
            default:  return "Autre";
        }
    }

    public static ProfesseurGrade fromString(String value) {
        if (value == null) return AUTRE;
        try { return ProfesseurGrade.valueOf(value.trim().toUpperCase()); }
        catch (Exception e) { return AUTRE; }
    }
}
