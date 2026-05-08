package services;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class PlanningConfig {

    private final int[] slots;
    private final int defaultStartYear;
    private final int defaultStartMonth;
    private final int defaultStartDay;
    private final int maxDays;
    private final List<String> defaultRooms;
    private final List<String> professorColorPalette;
    private final int maxJuryLoadGap;

    public static PlanningConfig defaults() {
        return new PlanningConfig(
                new int[]{9, 10, 11, 14, 15, 16, 17}, 2026, Calendar.JUNE, 23, 4,
                List.of("S4A", "S5A", "S16A", "S17A", "AMPHI A"), List.of(
                        "E74C3C", "3498DB", "2ECC71", "F39C12", "9B59B6",
                        "1ABC9C", "E67E22", "2980B9", "27AE60", "8E44AD",
                        "C0392B", "16A085", "D35400", "2C3E50", "F1C40F",
                        "7F8C8D", "6C3483", "117A65", "784212", "1F618D"
                ),
                2
        );
    }

    public PlanningConfig(int[] slots, int defaultStartYear, int defaultStartMonth, int defaultStartDay, int maxDays,
                          List<String> defaultRooms, List<String> professorColorPalette, int maxJuryLoadGap)
    {
        this.slots = Arrays.copyOf(slots, slots.length);
        this.defaultStartYear = defaultStartYear;
        this.defaultStartMonth = defaultStartMonth;
        this.defaultStartDay = defaultStartDay;
        this.maxDays = maxDays;
        this.defaultRooms = List.copyOf(defaultRooms);
        this.professorColorPalette = List.copyOf(professorColorPalette);
        this.maxJuryLoadGap = maxJuryLoadGap;
    }

    public int[] getSlots() {
        return Arrays.copyOf(slots, slots.length);
    }

    public int getDefaultStartYear() {
        return defaultStartYear;
    }

    public int getDefaultStartMonth() {
        return defaultStartMonth;
    }

    public int getDefaultStartDay() {
        return defaultStartDay;
    }

    public int getMaxDays() {
        return maxDays;
    }

    public List<String> getDefaultRooms() {
        return defaultRooms;
    }

    public List<String> getProfessorColorPalette() {
        return professorColorPalette;
    }

    public int getMaxJuryLoadGap() {
        return maxJuryLoadGap;
    }
}
