package services;

/**
 * Identifiers for all supported planning constraints.
 * Keep these constants in sync with {@link ConstraintSet#defaults()}.
 */
public final class ConstraintIds {

    private ConstraintIds() {}

    // ─── Original constraints ──────────────────────────────────────────────

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

    // ─── New operational constraints ───────────────────────────────────────

    /**
     * Minimum grade required for a jury president (e.g. PES, PH).
     * Stored as the upper-case enum name; empty string disables the rule.
     */
    public static final String MIN_PRESIDENT_GRADE = "MIN_PRESIDENT_GRADE";

    /**
     * If set to {@code true}, every jury must include at least one external
     * (non-internal) member. Useful for capstone defenses requiring industry
     * representation.
     */
    public static final String REQUIRE_EXTERNAL_MEMBER = "REQUIRE_EXTERNAL_MEMBER";

    /**
     * Maximum number of soutenances a professor can be involved in during a
     * single half-day. Tighter than MAX_SOUTENANCES_PER_PROF_PER_DAY.
     */
    public static final String MAX_SOUTENANCES_PER_HALFDAY = "MAX_SOUTENANCES_PER_HALFDAY";

    /**
     * Forbid the same jury (same 3 professors) from sitting back-to-back on
     * the same half-day if not strictly necessary. Reported as SOFT.
     */
    public static final String AVOID_CONSECUTIVE_DEFENSES = "AVOID_CONSECUTIVE_DEFENSES";

    /**
     * Maximum number of times the exact same jury (P + R1 + R2) can be
     * reused across the planning.
     */
    public static final String MAX_JURY_REPETITION = "MAX_JURY_REPETITION";

    /**
     * If set to {@code true}, planning honors per-professor unavailability
     * declarations (ProfesseurAvailability records) as HARD constraints.
     */
    public static final String RESPECT_PROF_UNAVAILABILITY = "RESPECT_PROF_UNAVAILABILITY";

    /**
     * If set to {@code true}, the project subject's language (from NLP) must
     * match at least one jury member's declared {@code languages}.
     */
    public static final String RESPECT_LANGUAGE_REQUIREMENT = "RESPECT_LANGUAGE_REQUIREMENT";

    /**
     * Prefer the encadrant's specialty / discipline when picking rapporteurs
     * (specialty compatibility). Reported as SOFT.
     */
    public static final String SPECIALTY_COMPATIBILITY = "SPECIALTY_COMPATIBILITY";

    /**
     * Penalize / forbid scheduling beyond the session's deadline date.
     * Stored as a boolean; HARD = block, SOFT = warn.
     */
    public static final String RESPECT_SESSION_DEADLINE = "RESPECT_SESSION_DEADLINE";

    /**
     * Disallow planning a professor on consecutive slots (same person,
     * back-to-back even with the rest gap).
     */
    public static final String FORBID_CONSECUTIVE_SLOTS = "FORBID_CONSECUTIVE_SLOTS";
}
