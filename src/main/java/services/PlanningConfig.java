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
 * <p>Supports two independent half-day ranges (morning and afternoon), each
 * of which can be individually enabled or disabled. When both are enabled the
 * slot list is the union of the two ranges. When only one is enabled the day
 * is a single continuous block. The {@link ConstraintSet} is also part of the
 * configuration.</p>
 */
public final class PlanningConfig {

    // ─── Time configuration ─────────────────────────────────────────────────
    private final int numberOfDays;
    private final LocalDate startDate;

    private final boolean morningEnabled;
    private final int startHourMorning;
    private final int endHourMorning;

    private final boolean afternoonEnabled;
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
        this.morningEnabled = b.morningEnabled;
        this.startHourMorning = b.startHourMorning;
        this.endHourMorning = b.endHourMorning;
        this.afternoonEnabled = b.afternoonEnabled;
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
                .morningEnabled(morningEnabled)
                .startHourMorning(startHourMorning)
                .endHourMorning(endHourMorning)
                .afternoonEnabled(afternoonEnabled)
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
    public boolean isMorningEnabled() { return morningEnabled; }
    public int getStartHourMorning() { return startHourMorning; }
    public int getEndHourMorning() { return endHourMorning; }
    public boolean isAfternoonEnabled() { return afternoonEnabled; }
    public int getStartHourAfternoon() { return startHourAfternoon; }
    public int getEndHourAfternoon() { return endHourAfternoon; }
    public int getSoutenanceDurationMinutes() { return soutenanceDurationMinutes; }
    public int getBreakBetweenMinutes() { return breakBetweenMinutes; }
    public List<String> getDefaultRooms() { return defaultRooms; }
    public List<String> getProfessorColorPalette() { return professorColorPalette; }
    public ConstraintSet getConstraints() { return constraints; }

    /** Effective stride between two slot starts (duration + break) in minutes. */
    public int getSlotStrideMinutes() {
        return Math.max(1, soutenanceDurationMinutes + Math.max(0, breakBetweenMinutes));
    }

    // ─── Slot computation ────────────────────────────────────────────────────

    public List<String> computeSlotLabels() {
        List<int[]> slots = computeSlotMinutes();
        List<String> out = new ArrayList<>(slots.size());
        for (int[] s : slots) {
            int h = s[0], m = s[1];
            out.add(m == 0 ? h + "h" : String.format(Locale.ROOT, "%dh%02d", h, m));
        }
        return out;
    }

    public List<int[]> computeSlotMinutes() {
        List<int[]> result = new ArrayList<>();
        if (morningEnabled)   addSlotsForRange(result, startHourMorning,   endHourMorning);
        if (afternoonEnabled) addSlotsForRange(result, startHourAfternoon, endHourAfternoon);
        return result;
    }

    public int[] getSlots() {
        List<int[]> slots = computeSlotMinutes();
        int[] out = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) out[i] = slots.get(i)[0];
        return out;
    }

    public int getSlotsPerDay() { return computeSlotMinutes().size(); }

    private void addSlotsForRange(List<int[]> result, int startH, int endH) {
        if (startH < 0 || endH <= startH) return;
        int durationMin = Math.max(15, soutenanceDurationMinutes);
        int stride = getSlotStrideMinutes();
        int cur = startH * 60, end = endH * 60;
        while (cur + durationMin <= end) {
            result.add(new int[]{cur / 60, cur % 60});
            cur += stride;
        }
    }

    // ─── Builder ─────────────────────────────────────────────────────────────

    public static final class Builder {
        private int numberOfDays = 4;
        private LocalDate startDate = LocalDate.of(2026, 6, 23);
        private boolean morningEnabled = true;
        private int startHourMorning = 9;
        private int endHourMorning = 12;
        private boolean afternoonEnabled = true;
        private int startHourAfternoon = 14;
        private int endHourAfternoon = 18;
        private int soutenanceDurationMinutes = 60;
        private int breakBetweenMinutes = 0;
        private List<String> defaultRooms = new ArrayList<>();
        private List<String> professorColorPalette = Arrays.asList(
                "E74C3C","3498DB","2ECC71","F39C12","9B59B6",
                "1ABC9C","E67E22","2980B9","27AE60","8E44AD",
                "C0392B","16A085","D35400","2C3E50","F1C40F",
                "7F8C8D","6C3483","117A65","784212","1F618D");
        private ConstraintSet constraints = ConstraintSet.defaults();

        public Builder numberOfDays(int v) { this.numberOfDays = Math.max(1, v); return this; }
        public Builder startDate(LocalDate v) { if (v != null) this.startDate = v; return this; }
        public Builder startDate(String iso) {
            if (iso == null || iso.trim().isEmpty()) return this;
            try { this.startDate = LocalDate.parse(iso.trim()); } catch (Exception ignored) {}
            return this;
        }
        public Builder morningEnabled(boolean v) { this.morningEnabled = v; return this; }
        public Builder startHourMorning(int v) { this.startHourMorning = clamp(v); return this; }
        public Builder endHourMorning(int v) { this.endHourMorning = clamp(v); return this; }
        public Builder afternoonEnabled(boolean v) { this.afternoonEnabled = v; return this; }
        public Builder startHourAfternoon(int v) { this.startHourAfternoon = clamp(v); return this; }
        public Builder endHourAfternoon(int v) { this.endHourAfternoon = clamp(v); return this; }
        public Builder soutenanceDurationMinutes(int v) { this.soutenanceDurationMinutes = Math.max(15, v); return this; }
        public Builder breakBetweenMinutes(int v) { this.breakBetweenMinutes = Math.max(0, v); return this; }
        public Builder defaultRooms(List<String> v) { if (v != null) this.defaultRooms = new ArrayList<>(v); return this; }
        public Builder professorColorPalette(List<String> v) { if (v != null) this.professorColorPalette = new ArrayList<>(v); return this; }
        public Builder constraints(ConstraintSet v) { if (v != null) this.constraints = v; return this; }

        public PlanningConfig build() {
            // Ensure at least one half-day is enabled
            if (!morningEnabled && !afternoonEnabled) morningEnabled = true;
            return new PlanningConfig(this);
        }

        private static int clamp(int v) { return Math.max(0, Math.min(23, v)); }
    }

    // ─── Convenience for legacy code paths ──────────────────────────────────

    public int getDefaultStartYear()  { return startDate.getYear(); }
    public int getDefaultStartMonth() { return startDate.getMonthValue() - 1; }
    public int getDefaultStartDay()   { return startDate.getDayOfMonth(); }
    public int getMaxDays()           { return numberOfDays; }
    public int getMaxJuryLoadGap()    { return constraints.getMaxJuryLoadGap(); }

    public Calendar startCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(getDefaultStartYear(), getDefaultStartMonth(), getDefaultStartDay(), 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }
}
