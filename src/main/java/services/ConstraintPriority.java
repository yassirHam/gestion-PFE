package services;

/**
 * Priority level of a planning constraint.
 *
 * <ul>
 *   <li>{@link #HARD} : a violation prevents the planning from being saved.</li>
 *   <li>{@link #SOFT} : a violation is reported as a warning but the planning is still produced.</li>
 * </ul>
 */
public enum ConstraintPriority {
    HARD,
    SOFT;

    public static ConstraintPriority fromString(String value) {
        if (value == null) return SOFT;
        String normalized = value.trim().toUpperCase();
        if ("HARD".equals(normalized)) return HARD;
        return SOFT;
    }
}
