package services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable, fully user-configurable planning configuration.
 *
 * <p>Time slots are computed dynamically from the morning/afternoon ranges and
 * the per-soutenance duration. Use the {@link Builder} to construct a config or
 * {@link #defaults()} to get the application defaults. The current
 * {@link ConstraintSet} is also part of the configuration.</p>
 */
public final class PlanningConfig {

    // ─── Time configuration ─────────────────────────────────────────────────
    private final int numberOfDays;
    private final LocalDate startDate;
    private final int startHourMorning;
    private final int endHourMorning;
    private final int startHourAfternoon;
    private final int endHourAfternoon;
    private final int soutenanceDurationMinutes;
    private final int breakBetweenMinutes;

    // ─── Misc ───────────────────────────────────────────────────────────────
    private final List<String> defaultRooms;
    private final List<String> professorColorPalette;
    private final ConstraintSet constraints;

    private PlanningConfig(Builder b) {
        this.numberOfDays = b.numberOfDays;
        this.startDate = b.startDate;
        this.startHourMorning = b.startHourMorning;
        this.endHourMorning = b.endHourMorning;
        this.startHourAfternoon = b.startHourAfternoon;
        this.endHourAfternoon = b.endHourAfternoon;
        this.soutenanceDurationMinutes = b.soutenanceDurationMinutes;
        this.breakBetweenMinutes = b.breakBetweenMinutes;
        this.defaultRooms = List.copyOf(b.defaultRooms);
        this.professorColorPalette = List.copyOf(b.professorColorPalette);
        this.constraints = Objects.requireNonNull(b.constraints, "constraints");
    }

    // ─── Defaults ────────────────────────────────────────────────────────────

    public static PlanningConfig defaults() {
        return new Builder().build();
    }

    public Builder toBuilder() {
        return new Builder()
                .numberOfDays(numberOfDays)
                .startDate(startDate)
                .startHourMorning(startHourMorning)
                .endHourMorning(endHourMorning)
                .startHourAfternoon(startHourAfternoon)
                .endHourAfternoon(endHourAfternoon)
                .soutenanceDurationMinutes(soutenanceDurationMinutes)
                .breakBetweenMinutes(breakBetweenMinutes)
                .defaultRooms(defaultRooms)
                .professorColorPalette(professorColorPalette)
                .constraints(constraints);
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public int getNumberOfDays() { return numberOfDays; }
    public LocalDate getStartDate() { return startDate; }
    public int getStartHourMorning() { return startHourMorning; }
    public int getEndHourMorning() { return endHourMorning; }
    public int getStartHourAfternoon() { return startHourAfternoon; }
    public int getEndHourAfternoon() { return endHourAfternoon; }
    public int getSoutenanceDurationMinutes() { return soutenanceDurationMinutes; }
    public int getBreakBetweenMinutes() { return breakBetweenMinutes; }
    public List<String> getDefaultRooms() { return defaultRooms; }
    public List<String> getProfessorColorPalette() { return professorColorPalette; }
    public ConstraintSet getConstraints() { return constraints; }

    /**
     * Effective stride between two slot starts (duration + break) in minutes.
     */
    public int getSlotStrideMinutes() {
        return Math.max(1, soutenanceDurationMinutes + Math.max(0, breakBetweenMinutes));
    }

    // ─── Slot computation ────────────────────────────────────────────────────

    /**
     * Hour-of-day strings ("9h00", "10h30") for every slot in one day.
     * Useful for display and for computing slot order.
     */
    public List<String> computeSlotLabels() {
        List<int[]> slots = computeSlotMinutes();
        List<String> out = new ArrayList<>(slots.size());
        for (int[] s : slots) {
            int h = s[0];
            int m = s[1];
            if (m == 0) {
                out.add(h + "h");
            } else {
                out.add(String.format(Locale.ROOT, "%dh%02d", h, m));
            }
        }
        return out;
    }

    /**
     * Returns one entry [hour, minute] per slot, ordered chronologically.
     */
    public List<int[]> computeSlotMinutes() {
        List<int[]> result = new ArrayList<>();
        addSlotsForRange(result, startHourMorning, endHourMorning);
        addSlotsForRange(result, startHourAfternoon, endHourAfternoon);
        return result;
    }

    /**
     * Backwards-compatible flat int array of slot start hours (truncated to
     * the hour). Two slots that share the same hour but different minutes will
     * appear twice in this array; prefer {@link #computeSlotLabels()} for
     * disambiguation.
     */
    public int[] getSlots() {
        List<int[]> slots = computeSlotMinutes();
        int[] out = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            out[i] = slots.get(i)[0];
        }
        return out;
    }

    /**
     * Number of slots per working day given the current configuration.
     */
    public int getSlotsPerDay() {
        return computeSlotMinutes().size();
    }

    private void addSlotsForRange(List<int[]> result, int startHour, int endHour) {
        if (startHour < 0 || endHour <= startHour) return;
        int durationMin = Math.max(15, soutenanceDurationMinutes);
        int stride = getSlotStrideMinutes();
        int currentMinutes = startHour * 60;
        int endMinutes = endHour * 60;
        while (currentMinutes + durationMin <= endMinutes) {
            int h = currentMinutes / 60;
            int m = currentMinutes % 60;
            result.add(new int[]{h, m});
            currentMinutes += stride;
        }
    }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static final class Builder {
        private int numberOfDays = 4;
        private LocalDate startDate = LocalDate.of(2026, 6, 23);
        private int startHourMorning = 9;
        private int endHourMorning = 12;
        private int startHourAfternoon = 14;
        private int endHourAfternoon = 18;
        private int soutenanceDurationMinutes = 60;
        private int breakBetweenMinutes = 0;
        private List<String> defaultRooms = new ArrayList<>();
        private List<String> professorColorPalette = Arrays.asList(
                "E74C3C", "3498DB", "2ECC71", "F39C12", "9B59B6",
                "1ABC9C", "E67E22", "2980B9", "27AE60", "8E44AD",
                "C0392B", "16A085", "D35400", "2C3E50", "F1C40F",
                "7F8C8D", "6C3483", "117A65", "784212", "1F618D");
        private ConstraintSet constraints = ConstraintSet.defaults();

        public Builder numberOfDays(int v) { this.numberOfDays = Math.max(1, v); return this; }
        public Builder startDate(LocalDate v) { if (v != null) this.startDate = v; return this; }

        /** Accepts "yyyy-MM-dd" or null/blank to keep the default. */
        public Builder startDate(String iso) {
            if (iso == null || iso.trim().isEmpty()) return this;
            try { this.startDate = LocalDate.parse(iso.trim()); } catch (Exception ignored) {}
            return this;
        }

        public Builder startHourMorning(int v) { this.startHourMorning = clampHour(v); return this; }
        public Builder endHourMorning(int v) { this.endHourMorning = clampHour(v); return this; }
        public Builder startHourAfternoon(int v) { this.startHourAfternoon = clampHour(v); return this; }
        public Builder endHourAfternoon(int v) { this.endHourAfternoon = clampHour(v); return this; }
        public Builder soutenanceDurationMinutes(int v) { this.soutenanceDurationMinutes = Math.max(15, v); return this; }
        public Builder breakBetweenMinutes(int v) { this.breakBetweenMinutes = Math.max(0, v); return this; }

        public Builder defaultRooms(List<String> v) {
            if (v != null) this.defaultRooms = new ArrayList<>(v);
            return this;
        }
        public Builder professorColorPalette(List<String> v) {
            if (v != null) this.professorColorPalette = new ArrayList<>(v);
            return this;
        }
        public Builder constraints(ConstraintSet v) {
            if (v != null) this.constraints = v;
            return this;
        }

        public PlanningConfig build() {
            // Sanity: ensure morning end <= afternoon start
            if (startHourMorning > endHourMorning) {
                int t = startHourMorning; startHourMorning = endHourMorning; endHourMorning = t;
            }
            if (startHourAfternoon > endHourAfternoon) {
                int t = startHourAfternoon; startHourAfternoon = endHourAfternoon; endHourAfternoon = t;
            }
            return new PlanningConfig(this);
        }

        private static int clampHour(int v) { return Math.max(0, Math.min(23, v)); }
    }

    // ─── Convenience for legacy code paths ──────────────────────────────────

    public int getDefaultStartYear() { return startDate.getYear(); }

    /** 0-based month index (compatible with java.util.Calendar). */
    public int getDefaultStartMonth() { return startDate.getMonthValue() - 1; }

    public int getDefaultStartDay() { return startDate.getDayOfMonth(); }

    public int getMaxDays() { return numberOfDays; }

    public int getMaxJuryLoadGap() { return constraints.getMaxJuryLoadGap(); }

    /**
     * Calendar instance pre-set to the configured start date at midnight.
     * Convenience for legacy code that still uses java.util.Calendar.
     */
    public Calendar startCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(getDefaultStartYear(), getDefaultStartMonth(), getDefaultStartDay(), 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }
}
