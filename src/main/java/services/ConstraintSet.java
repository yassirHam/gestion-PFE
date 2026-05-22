package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ordered collection of {@link Constraint}s, keyed by id, with typed accessors
 * for all constraints used by the planning engine.
 *
 * The set always contains the full list of supported constraints (see
 * {@link #defaults()}). User edits update the existing entries in-place rather
 * than removing/recreating them.
 */
public class ConstraintSet {

    private final Map<String, Constraint> byId = new LinkedHashMap<>();

    public ConstraintSet() {}

    /**
     * Build a constraint set populated with the application-wide defaults.
     */
    public static ConstraintSet defaults() {
        ConstraintSet set = new ConstraintSet();
        set.put(new Constraint(ConstraintIds.PROF_REST_HOURS,
                "Repos minimum entre 2 soutenances (meme prof, en heures)",
                "1", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.MIN_JURY_SPECIALITY_MATCH,
                "Minimum de membres jury de la discipline dominante",
                "2", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.DOMINANT_DISCIPLINE,
                "Discipline dominante requise dans le jury",
                "Informatique", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.MAX_JURY_LOAD_GAP,
                "Ecart max de participations au jury",
                "2", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.MAX_SOUTENANCES_PER_PROF_PER_DAY,
                "Max soutenances par professeur et par jour",
                "4", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.MAX_SOUTENANCES_PER_ROOM_PER_DAY,
                "Max soutenances par salle et par jour",
                "7", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.REQUIRE_ENCADRANT_AS_PRESIDENT,
                "Encadrant = President du jury",
                "true", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.FORBID_ENCADRANT_AS_RAPPORTEUR,
                "Interdire l'encadrant comme rapporteur",
                "true", ConstraintPriority.HARD, true));
        set.put(new Constraint(ConstraintIds.MIN_DISTINCT_JURY_MEMBERS,
                "Membres jury distincts (>=)",
                "3", ConstraintPriority.HARD, true));
        set.put(new Constraint(ConstraintIds.RESPECT_ENGLISH_PROF,
                "Inclure un prof d'anglais si sujet en anglais",
                "true", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.EXCLUDE_WEEKENDS,
                "Exclure samedi et dimanche",
                "true", ConstraintPriority.HARD));
        set.put(new Constraint(ConstraintIds.CUSTOM_EXCLUDED_DATES,
                "Dates exclues (YYYY-MM-DD separees par des virgules)",
                "", ConstraintPriority.HARD));
        return set;
    }

    public void put(Constraint c) {
        byId.put(c.getId(), c);
    }

    public Constraint get(String id) {
        return byId.get(id);
    }

    public List<Constraint> asList() {
        return new ArrayList<>(byId.values());
    }

    /**
     * Update value and priority for a known constraint. Unknown ids are ignored.
     */
    public void update(String id, String value, ConstraintPriority priority) {
        Constraint c = byId.get(id);
        if (c == null) return;
        if (value != null) c.setValue(value);
        if (priority != null) c.setPriority(priority);
    }

    // ─── Typed accessors used by the planning engine ─────────────────────────

    public int getProfRestHours() {
        Constraint c = get(ConstraintIds.PROF_REST_HOURS);
        return c == null ? 1 : Math.max(0, c.asInt(1));
    }

    public boolean isProfRestHard() {
        Constraint c = get(ConstraintIds.PROF_REST_HOURS);
        return c != null && c.isHard();
    }

    public int getMinSpecialityMatch() {
        Constraint c = get(ConstraintIds.MIN_JURY_SPECIALITY_MATCH);
        return c == null ? 2 : Math.max(0, c.asInt(2));
    }

    public boolean isMinSpecialityMatchHard() {
        Constraint c = get(ConstraintIds.MIN_JURY_SPECIALITY_MATCH);
        return c != null && c.isHard();
    }

    public String getDominantDiscipline() {
        Constraint c = get(ConstraintIds.DOMINANT_DISCIPLINE);
        return c == null ? "Informatique" : c.asString("Informatique");
    }

    public int getMaxJuryLoadGap() {
        Constraint c = get(ConstraintIds.MAX_JURY_LOAD_GAP);
        return c == null ? 2 : Math.max(0, c.asInt(2));
    }

    public boolean isMaxJuryLoadGapHard() {
        Constraint c = get(ConstraintIds.MAX_JURY_LOAD_GAP);
        return c != null && c.isHard();
    }

    public int getMaxSoutenancesPerProfPerDay() {
        Constraint c = get(ConstraintIds.MAX_SOUTENANCES_PER_PROF_PER_DAY);
        return c == null ? 4 : Math.max(1, c.asInt(4));
    }

    public boolean isMaxSoutenancesPerProfPerDayHard() {
        Constraint c = get(ConstraintIds.MAX_SOUTENANCES_PER_PROF_PER_DAY);
        return c != null && c.isHard();
    }

    public int getMaxSoutenancesPerRoomPerDay() {
        Constraint c = get(ConstraintIds.MAX_SOUTENANCES_PER_ROOM_PER_DAY);
        return c == null ? 7 : Math.max(1, c.asInt(7));
    }

    public boolean isMaxSoutenancesPerRoomPerDayHard() {
        Constraint c = get(ConstraintIds.MAX_SOUTENANCES_PER_ROOM_PER_DAY);
        return c != null && c.isHard();
    }

    public boolean isEncadrantPresidentRequired() {
        Constraint c = get(ConstraintIds.REQUIRE_ENCADRANT_AS_PRESIDENT);
        return c == null || c.asBool(true);
    }

    public boolean isEncadrantPresidentHard() {
        Constraint c = get(ConstraintIds.REQUIRE_ENCADRANT_AS_PRESIDENT);
        return c == null || c.isHard();
    }

    public boolean isEncadrantRapporteurForbidden() {
        Constraint c = get(ConstraintIds.FORBID_ENCADRANT_AS_RAPPORTEUR);
        return c == null || c.asBool(true);
    }

    public int getMinDistinctJuryMembers() {
        Constraint c = get(ConstraintIds.MIN_DISTINCT_JURY_MEMBERS);
        return c == null ? 3 : Math.max(2, Math.min(3, c.asInt(3)));
    }

    public boolean isRespectEnglishProf() {
        Constraint c = get(ConstraintIds.RESPECT_ENGLISH_PROF);
        return c == null || c.asBool(true);
    }

    public boolean isRespectEnglishProfHard() {
        Constraint c = get(ConstraintIds.RESPECT_ENGLISH_PROF);
        return c != null && c.isHard();
    }

    public boolean isExcludeWeekends() {
        Constraint c = get(ConstraintIds.EXCLUDE_WEEKENDS);
        return c == null || c.asBool(true);
    }

    public List<String> getCustomExcludedDates() {
        Constraint c = get(ConstraintIds.CUSTOM_EXCLUDED_DATES);
        if (c == null || c.getValue() == null || c.getValue().trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String token : c.getValue().split("[,;\\s]+")) {
            String t = token.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    public boolean isExcludeWeekendsHard() {
        Constraint c = get(ConstraintIds.EXCLUDE_WEEKENDS);
        return c == null || c.isHard();
    }

    public ConstraintPriority priorityOf(String id) {
        Constraint c = get(id);
        return c == null ? ConstraintPriority.SOFT : c.getPriority();
    }

    public String labelOf(String id) {
        Constraint c = get(id);
        return c == null ? id : c.getLabel();
    }

    /**
     * Lower-case helper for substring matches between two free-text values.
     */
    static boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isEmpty()) return false;
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}
