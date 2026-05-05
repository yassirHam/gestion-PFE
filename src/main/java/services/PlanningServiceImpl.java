package services;

import dao.*;
import entities.*;

import java.util.*;

/**
 * Planning generator — assigns jury (2 profs ≠ encadrant), room, date and hour
 * for every affectation already in DB.
 *
 * Constraints:
 *  - Start date : 23 June 2026
 *  - Maximum 4 days of planning
 *  - Time slots : 9h, 10h, 11h (morning) and 14h, 15h, 16h, 17h (afternoon)
 *  - A professor needs at least 1-hour REST between two consecutive soutenances
 *  - Spread load: encadrants must not have all their soutenances on the same day
 *  - Spread slots: fill all hours (9h, 10h...) evenly across rooms
 *  - EQUITY: balanced jury membership across all professors
 *  - INFO REQUIRED: Each jury must have at least 1 prof with "info" in discipline/specialite
 */
public class PlanningServiceImpl implements PlanningService {

    // ── DAOs ─────────────────────────────────────────────────────────────────
    private final AffectationDAO affDao  = new AffectationDAOImpl();
    private final ProfesseurDAO  profDao = new ProfesseurDAOImpl();
    private final SalleDAO       salleDao = new SalleDAOImpl();
    private final JuryDAO        juryDao  = new JuryDAOImpl();
    private final SoutenanceDAO  soutDao  = new SoutenanceDAOImpl();

    // ── Time slots ───────────────────────────────────────────────────────────
    private static final int[] SLOTS = {9, 10, 11, 14, 15, 16, 17};

    // ── Start date: 23 June 2026, max 4 days ─────────────────────────────────
    private static final int START_YEAR  = 2026;
    private static final int START_MONTH = Calendar.JUNE;
    private static final int START_DAY   = 23;
    private static final int MAX_DAYS    = 4;

    // ── Default rooms if DB is empty ─────────────────────────────────────────
    private static final String[] DEFAULT_ROOMS = {"S4A", "S5A", "S16A", "S17A", "AMPHI A"};

    // ── Professor color palette (hex, no #) ──────────────────────────────────
    private static final String[] COLOR_PALETTE = {
        "E74C3C", "3498DB", "2ECC71", "F39C12", "9B59B6",
        "1ABC9C", "E67E22", "2980B9", "27AE60", "8E44AD",
        "C0392B", "16A085", "D35400", "2C3E50", "F1C40F",
        "7F8C8D", "6C3483", "117A65", "784212", "1F618D"
    };

    private Map<Long, String> profColorMap = new LinkedHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<Soutenance> genererPlanning(List<String> filieres, List<String> log, List<Long> selectedSalles) {

        List<Affectation> allAffectations = affDao.findAllWithDetails();
        // Filter by selected filières if provided
        List<Affectation> affectations;
        if (filieres != null && !filieres.isEmpty()) {
            affectations = new ArrayList<>();
            for (Affectation a : allAffectations) {
                if (filieres.contains(a.getEtudiant().getFiliere())) {
                    affectations.add(a);
                }
            }
        } else {
            affectations = allAffectations;
        }

        if (affectations.isEmpty()) {
            log.add(" Aucune affectation trouvée pour les filières sélectionnées.");
            return Collections.emptyList();
        }

        List<Professeur> allProfs = profDao.findAll();
        if (allProfs.size() < 3) {
            log.add(" Il faut au moins 3 professeurs.");
            return Collections.emptyList();
        }

        ensureSallesExistent();
        List<Salle> allSalles = salleDao.findAll();
        List<Salle> salles = new ArrayList<>();
        if (selectedSalles == null || selectedSalles.isEmpty()) {
            salles.addAll(allSalles);
        } else {
            for (Salle s : allSalles) {
                if (selectedSalles.contains(s.getId_salle())) {
                    salles.add(s);
                }
            }
        }
        
        if (salles.isEmpty()) {
            log.add(" Aucune salle sélectionnée ou disponible.");
            return Collections.emptyList();
        }
        
        buildColorMap(allProfs);

        // Reset old planning
        soutDao.deleteAll();
        juryDao.deleteAll();

        // State trackers
        Map<String, Set<Long>> profBusyAtSlot = new HashMap<>();
        Map<Long, List<String>> profSchedule = new HashMap<>();
        Map<Long, Integer> profJuryCount = new HashMap<>();
        Map<Long, Map<String, Integer>> profDailyCount = new HashMap<>();
        Map<String, Boolean> roomBusy = new HashMap<>();

        for (Professeur p : allProfs) {
            profJuryCount.put(p.getIdp(), 0);
            profDailyCount.put(p.getIdp(), new HashMap<>());
        }

        // Pre-compute the 4 dates
        Calendar baseCal = Calendar.getInstance();
        baseCal.set(START_YEAR, START_MONTH, START_DAY, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        List<String> validDates = new ArrayList<>();
        List<Date> validDateObjects = new ArrayList<>();
        int daysAdded = 0;
        int d = 0;
        while (daysAdded < MAX_DAYS) {
            Calendar day = (Calendar) baseCal.clone();
            day.add(Calendar.DAY_OF_MONTH, d++);
            int dow = day.get(Calendar.DAY_OF_WEEK);
            // Skip weekends
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) continue;

            String dateStr = String.format("%04d-%02d-%02d",
                    day.get(Calendar.YEAR),
                    day.get(Calendar.MONTH) + 1,
                    day.get(Calendar.DAY_OF_MONTH));
            validDates.add(dateStr);
            validDateObjects.add(day.getTime());
            daysAdded++;
        }

        List<Soutenance> result = new ArrayList<>();

        for (Affectation aff : affectations) {
            Etudiant etudiant = aff.getEtudiant();
            Professeur encadrant = aff.getEncadrant();

            List<Professeur> juryPool = new ArrayList<>(allProfs);
            juryPool.removeIf(p -> p.getIdp().equals(encadrant.getIdp()));

            int bestDayIdx = -1;
            int bestSlot = -1;
            Salle bestSalle = null;
            Professeur bestM1 = null;
            Professeur bestM2 = null;
            
            int minDailyLoad = Integer.MAX_VALUE;
            int minSlotLoad = Integer.MAX_VALUE;

            // Search for the best slot across all 4 days
            for (int dayIdx = 0; dayIdx < validDates.size(); dayIdx++) {
                String dateStr = validDates.get(dayIdx);
                int encadrantDailyLoad = profDailyCount.get(encadrant.getIdp()).getOrDefault(dateStr, 0);

                // Optimization: if this day is already worse than the best found, skip
                if (encadrantDailyLoad > minDailyLoad) continue;

                for (int slot : SLOTS) {
                    if (!isProfAvailable(encadrant.getIdp(), dateStr, slot, profBusyAtSlot, profSchedule)) continue;

                    String slotKey = dateStr + "|" + slot;
                    int slotLoad = getSlotLoad(slotKey, salles, roomBusy);

                    // We want to minimize encadrant load FIRST, then distribute slots evenly
                    boolean better = false;
                    if (encadrantDailyLoad < minDailyLoad) {
                        better = true;
                    } else if (encadrantDailyLoad == minDailyLoad) {
                        if (slotLoad < minSlotLoad) better = true;
                    }

                    if (!better) continue;

                    Salle freeSalle = getFreeRoom(slotKey, salles, roomBusy);
                    if (freeSalle == null) continue;

                    // Filter and select the 2 best jury members
                    List<Professeur> available = new ArrayList<>();
                    for (Professeur p : juryPool) {
                        if (isProfAvailable(p.getIdp(), dateStr, slot, profBusyAtSlot, profSchedule)) {
                            available.add(p);
                        }
                    }
                    if (available.size() < 2) continue;

                    Professeur[] pickedJury = pickBestJury(encadrant, available, profJuryCount);
                    if (pickedJury == null) continue;

                    // If we reach here, it's the best option so far
                    bestDayIdx = dayIdx;
                    bestSlot = slot;
                    bestSalle = freeSalle;
                    bestM1 = pickedJury[0];
                    bestM2 = pickedJury[1];
                    minDailyLoad = encadrantDailyLoad;
                    minSlotLoad = slotLoad;
                }
            }

            if (bestDayIdx != -1) {
                String dateStr = validDates.get(bestDayIdx);
                String slotKey = dateStr + "|" + bestSlot;

                markProfBusy(encadrant.getIdp(), dateStr, bestSlot, profBusyAtSlot, profSchedule, profDailyCount);
                markProfBusy(bestM1.getIdp(),    dateStr, bestSlot, profBusyAtSlot, profSchedule, profDailyCount);
                markProfBusy(bestM2.getIdp(),    dateStr, bestSlot, profBusyAtSlot, profSchedule, profDailyCount);

                profJuryCount.merge(bestM1.getIdp(), 1, Integer::sum);
                profJuryCount.merge(bestM2.getIdp(), 1, Integer::sum);

                roomBusy.put(slotKey + "|" + bestSalle.getId_salle(), true);

                Jury jury = new Jury();
                jury.setPresident(encadrant);
                jury.setRapporteur1(bestM1);
                jury.setRapporteur2(bestM2);
                jury = juryDao.save(jury);

                Soutenance sout = new Soutenance();
                sout.setDate(validDateObjects.get(bestDayIdx));
                sout.setHeure(bestSlot + "h");
                sout.setSalle(bestSalle);
                sout.setEtudiant(etudiant);
                sout.setJury(jury);
                result.add(sout);

                log.add("✔ " + etudiant.getNomE() + " → " + dateStr + " " + bestSlot + "h | Salle: " + bestSalle.getNum_salle()
                        + " | Enc: " + encadrant.getNom() + " | Jury: " + bestM1.getNom() + ", " + bestM2.getNom());
            } else {
                log.add("⚠️ Impossible de planifier: " + etudiant.getNomE() + " (aucun créneau valide dans les 4 jours)");
            }
        }

        soutDao.saveAll(result);

        log.add("─── Répartition des participations jury ───");
        profJuryCount.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(e -> {
                Professeur p = allProfs.stream().filter(pr -> pr.getIdp().equals(e.getKey())).findFirst().orElse(null);
                if (p != null) log.add("   " + p.getNom() + " " + p.getPrenom() + " → " + e.getValue() + " fois");
            });

        return result;
    }

    // ── Constraints Checkers ─────────────────────────────────────────────────

    private boolean isProfAvailable(Long profId, String dateStr, int slot,
                                    Map<String, Set<Long>> profBusyAtSlot,
                                    Map<Long, List<String>> profSchedule) {
        if (profBusyAtSlot.getOrDefault(dateStr + "|" + slot, Collections.emptySet()).contains(profId)) return false;

        // 1-hour rest: Math.abs(existSlot - slot) == 1 means adjacent slots (forbidden)
        for (String existing : profSchedule.getOrDefault(profId, Collections.emptyList())) {
            if (!existing.startsWith(dateStr + "|")) continue;
            int existSlot = Integer.parseInt(existing.split("\\|")[1]);
            if (Math.abs(existSlot - slot) == 1) return false;
        }
        return true;
    }

    private boolean isInfo(Professeur p) {
        String d = p.getDiscipline();
        String s = p.getSpecialite();
        if (d != null && d.toLowerCase().contains("info")) return true;
        if (s != null && s.toLowerCase().contains("info")) return true;
        return false;
    }

    private Professeur[] pickBestJury(Professeur encadrant, List<Professeur> available, Map<Long, Integer> profJuryCount) {
        boolean encadrantIsInfo = isInfo(encadrant);

        // Sort by participation count (ascending) to maintain equity + inject small random
        // factor so same-count profs don't always come in the same order
        available.sort(Comparator.comparingInt((Professeur p) -> profJuryCount.getOrDefault(p.getIdp(), 0))
                .thenComparingInt(p -> (int)(Math.random() * 1000)));

        // ── Constraint: at least 2 out of 3 jury members must be Informatique ──
        // Case A: encadrant is NOT info → both rapporteurs MUST be info (2 + 0 = 2 total)
        // Case B: encadrant IS info → at least 1 rapporteur must be info (1 + 1 = 2 total)

        // First pass: strict — prefer the pair that satisfies the rule and both have lowest load
        for (int i = 0; i < available.size(); i++) {
            for (int j = i + 1; j < available.size(); j++) {
                Professeur p1 = available.get(i);
                Professeur p2 = available.get(j);

                int infoCount = (encadrantIsInfo ? 1 : 0) + (isInfo(p1) ? 1 : 0) + (isInfo(p2) ? 1 : 0);
                if (infoCount >= 2) {
                    return new Professeur[]{p1, p2};
                }
            }
        }

        // Fallback: constraint cannot be met (not enough info profs available at this slot).
        // Accept the best possible pair to avoid leaving a student unscheduled.
        if (available.size() >= 2) {
            return new Professeur[]{available.get(0), available.get(1)};
        }
        return null;
    }

    private int getSlotLoad(String slotKey, List<Salle> salles, Map<String, Boolean> roomBusy) {
        int count = 0;
        for (Salle s : salles) {
            if (roomBusy.getOrDefault(slotKey + "|" + s.getId_salle(), false)) count++;
        }
        return count;
    }

    private Salle getFreeRoom(String slotKey, List<Salle> salles, Map<String, Boolean> roomBusy) {
        for (Salle s : salles) {
            if (!roomBusy.getOrDefault(slotKey + "|" + s.getId_salle(), false)) return s;
        }
        return null;
    }

    private void markProfBusy(Long profId, String dateStr, int slot,
                               Map<String, Set<Long>> profBusyAtSlot,
                               Map<Long, List<String>> profSchedule,
                               Map<Long, Map<String, Integer>> profDailyCount) {
        profBusyAtSlot.computeIfAbsent(dateStr + "|" + slot, k -> new HashSet<>()).add(profId);
        profSchedule.computeIfAbsent(profId, k -> new ArrayList<>()).add(dateStr + "|" + slot);
        profDailyCount.get(profId).merge(dateStr, 1, Integer::sum);
    }

    // ── Setup Helpers ────────────────────────────────────────────────────────

    private void ensureSallesExistent() {
        if (salleDao.count() == 0) {
            List<Salle> defaults = new ArrayList<>();
            for (String name : DEFAULT_ROOMS) {
                Salle s = new Salle();
                s.setNum_salle(name);
                s.setBlock("Bloc Principal");
                s.setStatus("Libre");
                defaults.add(s);
            }
            salleDao.saveAll(defaults);
        }
    }

    private void buildColorMap(List<Professeur> profs) {
        List<Professeur> sorted = new ArrayList<>(profs);
        sorted.sort(Comparator.comparing(Professeur::getIdp));
        profColorMap = new LinkedHashMap<>();
        
        // Generate distinct colors using the golden ratio conjugate
        float hue = 0.0f;
        float goldenRatioConjugate = 0.618033988749895f;
        
        for (int i = 0; i < sorted.size(); i++) {
            if (i < COLOR_PALETTE.length) {
                profColorMap.put(sorted.get(i).getIdp(), COLOR_PALETTE[i]);
            } else {
                hue += goldenRatioConjugate;
                hue %= 1.0f;
                java.awt.Color c = java.awt.Color.getHSBColor(hue, 0.75f, 0.85f);
                String hex = String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
                profColorMap.put(sorted.get(i).getIdp(), hex);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<Soutenance> getAllSoutenances() {
        List<Soutenance> list = soutDao.findAllWithDetails();
        // Sort: date ASC -> slot ASC -> room ASC
        list.sort(Comparator
                .comparing(Soutenance::getDate)
                .thenComparingInt(s -> slotOrder(s.getHeure()))
                .thenComparing(s -> s.getSalle().getNum_salle()));
        return list;
    }

    private int slotOrder(String heure) {
        try {
            return Integer.parseInt(heure.replace("h", "").trim());
        } catch (NumberFormatException e) {
            return 99;
        }
    }

    @Override
    public Map<Long, String> getProfessorColors() {
        if (profColorMap.isEmpty()) buildColorMap(profDao.findAll());
        return profColorMap;
    }

    @Override
    public void deletePlanning() {
        soutDao.deleteAll();
        juryDao.deleteAll();
    }
}
