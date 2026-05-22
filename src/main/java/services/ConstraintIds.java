package services;

/**
 * Identifiers for all supported planning constraints.
 * Keep these constants in sync with {@link ConstraintSet#defaults()}.
 */
public final class ConstraintIds {

    private ConstraintIds() {}

    /** Minimum gap (in slots / hours) between two soutenances of the same professor. */
    public static final String PROF_REST_HOURS = "PROF_REST_HOURS";

    /** Minimum number of jury members whose discipline matches the dominant discipline. */
    public static final String MIN_JURY_SPECIALITY_MATCH = "MIN_JURY_SPECIALITY_MATCH";

    /** Dominant discipline expected in jury composition. */
    public static final String DOMINANT_DISCIPLINE = "DOMINANT_DISCIPLINE";

    /** Maximum acceptable gap of jury participations between most-loaded and least-loaded professor. */
    public static final String MAX_JURY_LOAD_GAP = "MAX_JURY_LOAD_GAP";

    /** Maximum number of soutenances a professor can be involved in (any role) in a single day. */
    public static final String MAX_SOUTENANCES_PER_PROF_PER_DAY = "MAX_SOUTENANCES_PER_PROF_PER_DAY";

    /** Maximum number of soutenances scheduled in one room in one day. */
    public static final String MAX_SOUTENANCES_PER_ROOM_PER_DAY = "MAX_SOUTENANCES_PER_ROOM_PER_DAY";

    /** Encadrant must be the president of the jury. */
    public static final String REQUIRE_ENCADRANT_AS_PRESIDENT = "REQUIRE_ENCADRANT_AS_PRESIDENT";

    /** Encadrant cannot be a rapporteur in the same project's jury. */
    public static final String FORBID_ENCADRANT_AS_RAPPORTEUR = "FORBID_ENCADRANT_AS_RAPPORTEUR";

    /** All jury members (3) must be different people. */
    public static final String MIN_DISTINCT_JURY_MEMBERS = "MIN_DISTINCT_JURY_MEMBERS";

    /** Include an English professor when the subject is detected as English. */
    public static final String RESPECT_ENGLISH_PROF = "RESPECT_ENGLISH_PROF";

    /** Skip Saturday and Sunday when generating the planning. */
    public static final String EXCLUDE_WEEKENDS = "EXCLUDE_WEEKENDS";

    /** User-supplied list of excluded dates (YYYY-MM-DD, comma separated). */
    public static final String CUSTOM_EXCLUDED_DATES = "CUSTOM_EXCLUDED_DATES";
}
