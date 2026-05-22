package services;

import dao.AffectationDAO;
import dao.AffectationDAOImpl;
import dao.JuryDAO;
import dao.JuryDAOImpl;
import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import dao.SalleDAO;
import dao.SalleDAOImpl;
import dao.SoutenanceDAO;
import dao.SoutenanceDAOImpl;
import entities.Affectation;
import entities.Etudiant;
import entities.Jury;
import entities.Professeur;
import entities.Salle;
import entities.Soutenance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generates a soutenance planning from the existing affectations.
 *
 * <p>The orchestration is delegated to {@link PlanningConfig} (time +
 * constraints) and to a pluggable {@link JurySelectionStrategy}, so the
 * planning rules can be tuned by the user without touching this class.</p>
 *
 * <p>HARD constraints prevent the planning from being saved: when any
 * unresolvable conflict is detected, the resulting {@link PlanningResult}
 * carries the list of violations and the database is left unchanged.</p>
 */
public class PlanningServiceImpl implements PlanningService {

    private final AffectationDAO affDao;
    private final ProfesseurDAO profDao;
    private final SalleDAO salleDao;
    private final JuryDAO juryDao;
    private final SoutenanceDAO soutDao;
    private final NlpService nlpService;
    private final JurySelectionStrategy jurySelectionStrategy;

    private Map<Long, String> profColorMap = new LinkedHashMap<>();

    public PlanningServiceImpl() {
        this(new AffectationDAOImpl(), new ProfesseurDAOImpl(), new SalleDAOImpl(), new JuryDAOImpl(),
                new SoutenanceDAOImpl(), new NlpServiceImpl(), new DefaultJurySelectionStrategy());
    }

    public PlanningServiceImpl(AffectationDAO affDao, ProfesseurDAO profDao, SalleDAO salleDao, JuryDAO juryDao,
                               SoutenanceDAO soutDao, NlpService nlpService,
                               JurySelectionStrategy jurySelectionStrategy) {
        this.affDao = Objects.requireNonNull(affDao);
        this.profDao = Objects.requireNonNull(profDao);
        this.salleDao = Objects.requireNonNull(salleDao);
        this.juryDao = Objects.requireNonNull(juryDao);
        this.soutDao = Objects.requireNonNull(soutDao);
        this.nlpService = Objects.requireNonNull(nlpService);
        this.jurySelectionStrategy = Objects.requireNonNull(jurySelectionStrategy);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    @Override
    public PlanningResult genererPlanning(List<String> filieres, List<Long> selectedSalles, PlanningConfig config) {
        PlanningResult result = new PlanningResult();
        PlanningConfig cfg = config == null ? PlanningConfig.defaults() : config;
        ConstraintSet constraints = cfg.getConstraints();

        // ── Pre-flight checks ───────────────────────────────────────────────
        List<Affectation> affectations = filterAffectationsByFiliere(affDao.findAllWithDetails(), filieres);
        if (affectations.isEmpty()) {
            result.addDebug("Aucune affectation trouvee pour les filieres selectionnees.");
            result.addViolation(new ConstraintViolation("AFFECTATION_REQUIRED", "Affectations requises",
                    ConstraintPriority.HARD, "Aucune affectation n'est disponible.",
                    "Lancez d'abord l'affectation des encadrants."));
            result.setSuccess(false);
            return result;
        }

        List<Professeur> allProfs = profDao.findAll();
        if (allProfs.size() < 3) {
            result.addDebug("Il faut au moins 3 professeurs pour former un jury.");
            result.addViolation(new ConstraintViolation("MIN_PROFESSORS", "Minimum 3 professeurs",
                    ConstraintPriority.HARD,
                    "Seulement " + allProfs.size() + " professeur(s) disponible(s).",
                    "Importez au moins 3 professeurs."));
            result.setSuccess(false);
            return result;
        }

        ensureSallesExistent(cfg);
        List<Salle> salles = selectSalles(selectedSalles);
        if (salles.isEmpty()) {
            result.addDebug("Aucune salle selectionnee ou disponible.");
            result.addViolation(new ConstraintViolation("SALLE_REQUIRED", "Salle requise",
                    ConstraintPriority.HARD, "Aucune salle selectionnee.",
                    "Selectionnez au moins une salle dans la configuration."));
            result.setSuccess(false);
            return result;
        }

        int[] slots = cfg.getSlots();
        List<String> slotLabels = cfg.computeSlotLabels();
        if (slots.length == 0) {
            result.addViolation(new ConstraintViolation("SLOTS_REQUIRED", "Aucun creneau",
                    ConstraintPriority.HARD,
                    "La plage horaire et la duree configuree ne produisent aucun creneau.",
                    "Augmentez la plage horaire ou reduisez la duree d'une soutenance."));
            result.setSuccess(false);
            return result;
        }

        buildColorMap(allProfs, cfg);

        // ── Reset previous planning so we can rebuild ──────────────────────
        resetPlanning();

        Map<String, Set<Long>> profBusyAtSlot = new HashMap<>();
        Map<Long, List<int[]>> profSlotMinutes = new HashMap<>();
        Map<Long, Integer> profJuryCount = new HashMap<>();
        Map<Long, Map<String, Integer>> profDailyCount = new HashMap<>();
        Map<String, Boolean> roomBusy = new HashMap<>();
        Map<String, Integer> roomDailyCount = new HashMap<>();

        for (Professeur p : allProfs) {
            profJuryCount.put(p.getIdp(), 0);
            profDailyCount.put(p.getIdp(), new HashMap<>());
        }

        PlanningDates planningDates = buildPlanningDates(cfg, result);
        if (planningDates.validDates.isEmpty()) {
            result.addViolation(new ConstraintViolation("DATES_REQUIRED", "Aucune date valide",
                    ConstraintPriority.HARD,
                    "La configuration n'a produit aucune date valide.",
                    "Augmentez le nombre de jours ou retirez certaines dates exclues."));
            result.setSuccess(false);
            return result;
        }

        List<List<Affectation>> projects = groupAffectationsByProject(affectations);
        Collections.shuffle(projects);

        Map<String, SujetAnalysis> nlpBatchResults = analyzeProjectSubjects(projects, allProfs, result);
        List<Soutenance> generated = new ArrayList<>();

        // ── Schedule each project ──────────────────────────────────────────
        for (List<Affectation> project : projects) {
            Affectation mainAff = project.get(0);
            Etudiant etudiant = mainAff.getEtudiant();
            Professeur encadrant = mainAff.getEncadrant();
            SujetAnalysis nlpResult = getProjectAnalysis(etudiant, nlpBatchResults, result);

            List<Professeur> juryPool = new ArrayList<>(allProfs);
            juryPool.removeIf(p -> p.getIdp().equals(encadrant.getIdp()));

            PlanningChoice bestChoice = findBestPlanningChoice(encadrant, juryPool, planningDates, slots, slotLabels,
                    salles, profBusyAtSlot, profSlotMinutes, profJuryCount, profDailyCount, roomBusy, roomDailyCount,
                    nlpResult, cfg);

            if (bestChoice != null) {
                generated.addAll(saveProjectPlanning(project, encadrant, bestChoice, planningDates, slotLabels,
                        profBusyAtSlot, profSlotMinutes, profDailyCount, profJuryCount, roomBusy, roomDailyCount,
                        result));
            } else {
                String studentNames = projectStudentNames(project);
                result.addDebug("Impossible de planifier: " + studentNames
                        + " (aucun creneau valide dans " + cfg.getNumberOfDays() + " jour(s))");
                result.addUnscheduledProject(studentNames);
            }
        }

        // ── Validate the generated planning ─────────────────────────────────
        validatePlanning(generated, allProfs, salles, planningDates, slotLabels, cfg, result);

        // ── Persist or roll back ────────────────────────────────────────────
        if (result.hasBlockingIssues()) {
            // Don't persist; surface the violations to the user.
            result.setSuccess(false);
            buildBlockingSuggestions(cfg, salles.size(), allProfs.size(), planningDates.validDates.size(),
                    cfg.getSlotsPerDay(), result);
            result.clearSoutenances();
            return result;
        }

        soutDao.saveAll(generated);
        result.addSoutenances(generated);
        logJuryDistribution(result, allProfs, profJuryCount);
        result.setSuccess(true);
        return result;
    }

    @Override
    public List<Soutenance> getAllSoutenances() {
        List<Soutenance> list = soutDao.findAllWithDetails();
        list.sort(Comparator
                .comparing(Soutenance::getDate)
                .thenComparingInt(s -> slotOrder(s.getHeure()))
                .thenComparing(s -> s.getSalle().getNum_salle()));
        return list;
    }

    @Override
    public Map<Long, String> getProfessorColors() {
        if (profColorMap.isEmpty()) {
            buildColorMap(profDao.findAll(), PlanningConfig.defaults());
        }
        return profColorMap;
    }

    @Override
    public void deletePlanning() {
        resetPlanning();
    }

    // ─── Pre-processing ──────────────────────────────────────────────────────

    private List<Affectation> filterAffectationsByFiliere(List<Affectation> all, List<String> filieres) {
        if (filieres == null || filieres.isEmpty()) return all;
        List<Affectation> out = new ArrayList<>();
        for (Affectation a : all) {
            if (filieres.contains(a.getEtudiant().getFiliere())) out.add(a);
        }
        return out;
    }

    private List<Salle> selectSalles(List<Long> selectedSalles) {
        List<Salle> all = salleDao.findAll();
        if (selectedSalles == null || selectedSalles.isEmpty()) return new ArrayList<>(all);
        List<Salle> out = new ArrayList<>();
        for (Salle s : all) {
            if (selectedSalles.contains(s.getId_salle())) out.add(s);
        }
        return out;
    }

    private void resetPlanning() {
        soutDao.deleteAll();
        juryDao.deleteAll();
    }

    private PlanningDates buildPlanningDates(PlanningConfig cfg, PlanningResult result) {
        ConstraintSet constraints = cfg.getConstraints();
        Set<String> excludedDates = new HashSet<>(constraints.getCustomExcludedDates());
        boolean excludeWeekends = constraints.isExcludeWeekends();

        Calendar baseCal = cfg.startCalendar();

        List<String> validDates = new ArrayList<>();
        List<Date> validDateObjects = new ArrayList<>();
        int daysAdded = 0;
        int offset = 0;
        int safety = 0;

        while (daysAdded < cfg.getNumberOfDays() && safety++ < 365) {
            Calendar day = (Calendar) baseCal.clone();
            day.add(Calendar.DAY_OF_MONTH, offset++);
            int dow = day.get(Calendar.DAY_OF_WEEK);
            String iso = String.format("%04d-%02d-%02d",
                    day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));

            if (excludeWeekends && (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)) continue;
            if (excludedDates.contains(iso)) {
                result.addDebug("Date " + iso + " exclue (configuration utilisateur).");
                continue;
            }

            validDates.add(iso);
            validDateObjects.add(day.getTime());
            daysAdded++;
        }

        return new PlanningDates(validDates, validDateObjects);
    }

    private List<List<Affectation>> groupAffectationsByProject(List<Affectation> affectations) {
        Map<String, Affectation> affByCne = new HashMap<>();
        for (Affectation a : affectations) affByCne.put(a.getEtudiant().getCne(), a);

        List<List<Affectation>> projects = new ArrayList<>();
        Set<String> processedCne = new HashSet<>();

        for (Affectation a : affectations) {
            String cne = a.getEtudiant().getCne();
            if (processedCne.contains(cne)) continue;
            List<Affectation> project = new ArrayList<>();
            project.add(a);
            processedCne.add(cne);

            Etudiant e = a.getEtudiant();
            if (e.hasBinome() && affByCne.containsKey(e.getBinome_cne())) {
                Affectation partnerAff = affByCne.get(e.getBinome_cne());
                if (!processedCne.contains(partnerAff.getEtudiant().getCne())) {
                    project.add(partnerAff);
                    processedCne.add(partnerAff.getEtudiant().getCne());
                }
            }
            normalizeProjectSubject(project);
            projects.add(project);
        }
        return projects;
    }

    private void normalizeProjectSubject(List<Affectation> project) {
        if (project == null || project.size() < 2) return;
        String subject = "";
        for (Affectation a : project) {
            if (a.getEtudiant() == null) continue;
            String c = safe(a.getEtudiant().getSujet_stage());
            if (!c.isEmpty() && !isDefaultSubject(c)) { subject = c; break; }
            if (subject.isEmpty()) subject = c;
        }
        if (!subject.isEmpty()) {
            for (Affectation a : project) {
                if (a.getEtudiant() != null) a.getEtudiant().setSujet_stage(subject);
            }
        }
    }

    private Map<String, SujetAnalysis> analyzeProjectSubjects(List<List<Affectation>> projects,
                                                              List<Professeur> allProfs, PlanningResult result) {
        List<String> specialitesDispo = new ArrayList<>();
        for (Professeur p : allProfs) {
            String s = safe(p.getSpecialite());
            if (!s.isEmpty()) specialitesDispo.add(s);
        }
        specialitesDispo = new ArrayList<>(new LinkedHashSet<>(specialitesDispo));

        List<String> uniqueSujets = new ArrayList<>();
        for (List<Affectation> project : projects) {
            String sujet = project.get(0).getEtudiant().getSujet_stage();
            if (sujet != null && !sujet.trim().isEmpty() && !uniqueSujets.contains(sujet)) {
                uniqueSujets.add(sujet);
            }
        }
        if (uniqueSujets.isEmpty()) return new HashMap<>();

        try {
            result.addDebug("Demarrage analyse NLP Batch pour " + uniqueSujets.size() + " sujets uniques...");
            Map<String, SujetAnalysis> r = nlpService.analyzeSujetsBatch(uniqueSujets, specialitesDispo);
            result.addDebug("Analyse NLP Batch terminee.");
            return r;
        } catch (Exception e) {
            result.addDebug("Erreur NLP Batch : " + e.getMessage());
            return new HashMap<>();
        }
    }

    private SujetAnalysis getProjectAnalysis(Etudiant etudiant, Map<String, SujetAnalysis> nlpBatchResults,
                                             PlanningResult result) {
        String sujet = etudiant.getSujet_stage();
        if (sujet == null || sujet.trim().isEmpty()) return null;
        SujetAnalysis nlp = nlpBatchResults.get(sujet);
        if (nlp != null) {
            result.addDebug("NLP [" + etudiant.getNomE() + "] sujet='" + sujet + "' -> " + nlp);
            if (nlp.getLanguage() != null) etudiant.setLanguage(nlp.getLanguage());
        }
        return nlp;
    }

    // ─── Slot search ─────────────────────────────────────────────────────────

    private PlanningChoice findBestPlanningChoice(Professeur encadrant, List<Professeur> juryPool,
                                                  PlanningDates planningDates, int[] slots, List<String> slotLabels,
                                                  List<Salle> salles,
                                                  Map<String, Set<Long>> profBusyAtSlot,
                                                  Map<Long, List<int[]>> profSlotMinutes,
                                                  Map<Long, Integer> profJuryCount,
                                                  Map<Long, Map<String, Integer>> profDailyCount,
                                                  Map<String, Boolean> roomBusy,
                                                  Map<String, Integer> roomDailyCount,
                                                  SujetAnalysis nlpResult, PlanningConfig cfg) {
        ConstraintSet c = cfg.getConstraints();
        List<int[]> slotMinutes = cfg.computeSlotMinutes();
        int restMinutes = c.getProfRestHours() * 60;
        int durationMin = cfg.getSoutenanceDurationMinutes();
        int maxProfPerDay = c.getMaxSoutenancesPerProfPerDay();
        int maxRoomPerDay = c.getMaxSoutenancesPerRoomPerDay();

        PlanningChoice bestChoice = null;
        int minDailyLoad = Integer.MAX_VALUE;
        int minSlotLoad = Integer.MAX_VALUE;

        for (int dayIdx = 0; dayIdx < planningDates.validDates.size(); dayIdx++) {
            String dateStr = planningDates.validDates.get(dayIdx);
            int encDailyLoad = profDailyCount.get(encadrant.getIdp()).getOrDefault(dateStr, 0);
            if (encDailyLoad >= maxProfPerDay) continue;
            if (encDailyLoad > minDailyLoad) continue;

            for (int slotIdx = 0; slotIdx < slots.length; slotIdx++) {
                int slotHour = slots[slotIdx];
                int[] slotMin = slotMinutes.get(slotIdx);
                String slotLabel = slotLabels.get(slotIdx);

                if (!isProfAvailable(encadrant.getIdp(), dateStr, slotMin, restMinutes, durationMin,
                        profBusyAtSlot, profSlotMinutes, slotLabel)) continue;

                String slotKey = dateStr + "|" + slotLabel;
                int slotLoad = getSlotLoad(slotKey, salles, roomBusy);
                if (!isBetterChoice(encDailyLoad, slotLoad, minDailyLoad, minSlotLoad)) continue;

                Salle freeSalle = getFreeRoom(slotKey, dateStr, salles, roomBusy, roomDailyCount, maxRoomPerDay);
                if (freeSalle == null) continue;

                List<Professeur> available = findAvailableProfessors(juryPool, dateStr, slotMin, restMinutes,
                        durationMin, profBusyAtSlot, profSlotMinutes, slotLabel, profDailyCount, maxProfPerDay);
                if (available.size() < 2) continue;

                Professeur[] picked = jurySelectionStrategy.selectJury(encadrant, available, profJuryCount,
                        nlpResult, cfg);
                if (picked == null) continue;

                bestChoice = new PlanningChoice(dayIdx, slotIdx, slotHour, slotLabel, slotMin,
                        durationMin, freeSalle, picked[0], picked[1]);
                minDailyLoad = encDailyLoad;
                minSlotLoad = slotLoad;
            }
        }

        return bestChoice;
    }

    private boolean isBetterChoice(int encDailyLoad, int slotLoad, int minDailyLoad, int minSlotLoad) {
        if (encDailyLoad < minDailyLoad) return true;
        return encDailyLoad == minDailyLoad && slotLoad < minSlotLoad;
    }

    private List<Professeur> findAvailableProfessors(List<Professeur> juryPool, String dateStr, int[] slotMin,
                                                     int restMinutes, int durationMin,
                                                     Map<String, Set<Long>> profBusyAtSlot,
                                                     Map<Long, List<int[]>> profSlotMinutes,
                                                     String slotLabel,
                                                     Map<Long, Map<String, Integer>> profDailyCount,
                                                     int maxProfPerDay) {
        List<Professeur> out = new ArrayList<>();
        for (Professeur p : juryPool) {
            int dailyLoad = profDailyCount.get(p.getIdp()).getOrDefault(dateStr, 0);
            if (dailyLoad >= maxProfPerDay) continue;
            if (isProfAvailable(p.getIdp(), dateStr, slotMin, restMinutes, durationMin, profBusyAtSlot,
                    profSlotMinutes, slotLabel)) {
                out.add(p);
            }
        }
        return out;
    }

    /**
     * Check that the professor is free at this slot AND has at least
     * {@code restMinutes} between any other booking that day.
     */
    private boolean isProfAvailable(Long profId, String dateStr, int[] slotMin, int restMinutes, int durationMin,
                                    Map<String, Set<Long>> profBusyAtSlot,
                                    Map<Long, List<int[]>> profSlotMinutes,
                                    String slotLabel) {
        if (profBusyAtSlot.getOrDefault(dateStr + "|" + slotLabel, Collections.emptySet()).contains(profId)) {
            return false;
        }
        int newStart = slotMin[0] * 60 + slotMin[1];
        int newEnd = newStart + durationMin;
        // Each entry in profSlotMinutes is [date-encoded, startMinute, endMinute].
        for (int[] existing : profSlotMinutes.getOrDefault(profId, Collections.emptyList())) {
            int day = existing[0];
            if (day != dateKeyHash(dateStr)) continue;
            int existStart = existing[1];
            int existEnd = existing[2];
            // Compute gap between intervals (min absolute distance between intervals)
            int gap;
            if (newEnd <= existStart) {
                gap = existStart - newEnd;
            } else if (newStart >= existEnd) {
                gap = newStart - existEnd;
            } else {
                gap = -1; // overlap
            }
            if (gap < restMinutes) return false;
        }
        return true;
    }

    private int getSlotLoad(String slotKey, List<Salle> salles, Map<String, Boolean> roomBusy) {
        int n = 0;
        for (Salle s : salles) {
            if (roomBusy.getOrDefault(slotKey + "|" + s.getId_salle(), false)) n++;
        }
        return n;
    }

    private Salle getFreeRoom(String slotKey, String dateStr, List<Salle> salles, Map<String, Boolean> roomBusy,
                              Map<String, Integer> roomDailyCount, int maxRoomPerDay) {
        for (Salle s : salles) {
            if (roomBusy.getOrDefault(slotKey + "|" + s.getId_salle(), false)) continue;
            int dailyCount = roomDailyCount.getOrDefault(dateStr + "|" + s.getId_salle(), 0);
            if (dailyCount >= maxRoomPerDay) continue;
            return s;
        }
        return null;
    }

    private List<Soutenance> saveProjectPlanning(List<Affectation> project, Professeur encadrant,
                                                 PlanningChoice choice, PlanningDates planningDates,
                                                 List<String> slotLabels,
                                                 Map<String, Set<Long>> profBusyAtSlot,
                                                 Map<Long, List<int[]>> profSlotMinutes,
                                                 Map<Long, Map<String, Integer>> profDailyCount,
                                                 Map<Long, Integer> profJuryCount,
                                                 Map<String, Boolean> roomBusy,
                                                 Map<String, Integer> roomDailyCount,
                                                 PlanningResult result) {
        String dateStr = planningDates.validDates.get(choice.dayIdx);
        String slotLabel = choice.slotLabel;
        String slotKey = dateStr + "|" + slotLabel;

        // The duration is implicit in the stride that produced the slot list
        int duration = choice.endMinute - choice.startMinute;
        if (duration <= 0) duration = 60;

        markProfBusy(encadrant.getIdp(), dateStr, choice.startMinute, choice.endMinute, slotLabel,
                profBusyAtSlot, profSlotMinutes, profDailyCount);
        markProfBusy(choice.rapporteur1.getIdp(), dateStr, choice.startMinute, choice.endMinute, slotLabel,
                profBusyAtSlot, profSlotMinutes, profDailyCount);
        markProfBusy(choice.rapporteur2.getIdp(), dateStr, choice.startMinute, choice.endMinute, slotLabel,
                profBusyAtSlot, profSlotMinutes, profDailyCount);

        profJuryCount.merge(choice.rapporteur1.getIdp(), 1, Integer::sum);
        profJuryCount.merge(choice.rapporteur2.getIdp(), 1, Integer::sum);
        roomBusy.put(slotKey + "|" + choice.salle.getId_salle(), true);
        roomDailyCount.merge(dateStr + "|" + choice.salle.getId_salle(), 1, Integer::sum);

        Jury jury = new Jury();
        jury.setPresident(encadrant);
        jury.setRapporteur1(choice.rapporteur1);
        jury.setRapporteur2(choice.rapporteur2);
        jury = juryDao.save(jury);

        List<Soutenance> sout = new ArrayList<>();
        for (Affectation aff : project) {
            Soutenance s = new Soutenance();
            s.setDate(planningDates.validDateObjects.get(choice.dayIdx));
            s.setHeure(slotLabel);
            s.setSalle(choice.salle);
            s.setEtudiant(aff.getEtudiant());
            s.setJury(jury);
            sout.add(s);
        }

        result.addDebug(projectStudentNames(project) + " -> " + dateStr + " " + slotLabel
                + " | Salle: " + choice.salle.getNum_salle()
                + " | Enc: " + encadrant.getNom()
                + " | Jury: " + choice.rapporteur1.getNom() + ", " + choice.rapporteur2.getNom());
        return sout;
    }

    private void markProfBusy(Long profId, String dateStr, int startMin, int endMin, String slotLabel,
                              Map<String, Set<Long>> profBusyAtSlot,
                              Map<Long, List<int[]>> profSlotMinutes,
                              Map<Long, Map<String, Integer>> profDailyCount) {
        profBusyAtSlot.computeIfAbsent(dateStr + "|" + slotLabel, k -> new HashSet<>()).add(profId);
        profSlotMinutes.computeIfAbsent(profId, k -> new ArrayList<>())
                .add(new int[]{dateKeyHash(dateStr), startMin, endMin});
        profDailyCount.get(profId).merge(dateStr, 1, Integer::sum);
    }

    private void ensureSallesExistent(PlanningConfig cfg) {
        if (salleDao.count() == 0) {
            List<Salle> defaults = new ArrayList<>();
            for (String name : cfg.getDefaultRooms()) {
                Salle s = new Salle();
                s.setNum_salle(name);
                s.setBlock("Bloc Principal");
                s.setStatus("Libre");
                defaults.add(s);
            }
            salleDao.saveAll(defaults);
        }
    }

    private void buildColorMap(List<Professeur> profs, PlanningConfig cfg) {
        List<Professeur> sorted = new ArrayList<>(profs);
        sorted.sort(Comparator.comparing(Professeur::getIdp));
        profColorMap = new LinkedHashMap<>();

        List<String> palette = cfg.getProfessorColorPalette();
        float hue = 0.0f;
        float goldenRatioConjugate = 0.618033988749895f;

        for (int i = 0; i < sorted.size(); i++) {
            if (i < palette.size()) {
                profColorMap.put(sorted.get(i).getIdp(), palette.get(i));
            } else {
                hue += goldenRatioConjugate;
                hue %= 1.0f;
                java.awt.Color c = java.awt.Color.getHSBColor(hue, 0.75f, 0.85f);
                String hex = String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
                profColorMap.put(sorted.get(i).getIdp(), hex);
            }
        }
    }

    // ─── Validation pass ─────────────────────────────────────────────────────

    /**
     * Inspects the generated planning and adds any HARD or SOFT
     * {@link ConstraintViolation}s found to the result. SOFT-only violations
     * do not block persistence; HARD violations cause the result to be marked
     * unsuccessful.
     */
    private void validatePlanning(List<Soutenance> generated, List<Professeur> allProfs, List<Salle> salles,
                                  PlanningDates planningDates, List<String> slotLabels, PlanningConfig cfg,
                                  PlanningResult result) {
        ConstraintSet c = cfg.getConstraints();

        // ── Salle conflicts (HARD) ──────────────────────────────────────────
        Map<String, Integer> roomSlotCount = new HashMap<>();
        Map<String, Integer> roomDailyCount = new HashMap<>();
        Map<Long, Map<String, Integer>> profDaily = new HashMap<>();
        Map<Long, Integer> profJuryCount = new HashMap<>();

        for (Soutenance s : generated) {
            if (s.getSalle() == null || s.getDate() == null) continue;
            String dateStr = isoDate(s.getDate());
            String slot = s.getHeure();
            String roomSlotKey = dateStr + "|" + slot + "|" + s.getSalle().getId_salle();
            roomSlotCount.merge(roomSlotKey, 1, Integer::sum);
            // Each (date, salle) pair is incremented once per project, not once per student
            // The grouping by jury+date+slot+salle naturally collapses binomes.
        }

        Set<String> roomProjectSeen = new HashSet<>();
        for (Soutenance s : generated) {
            if (s.getSalle() == null || s.getDate() == null) continue;
            String dateStr = isoDate(s.getDate());
            String key = dateStr + "|" + s.getHeure() + "|" + s.getSalle().getId_salle()
                    + "|" + (s.getJury() == null ? "0" : s.getJury().getIdJury());
            if (!roomProjectSeen.add(key)) continue;
            String dailyKey = dateStr + "|" + s.getSalle().getId_salle();
            roomDailyCount.merge(dailyKey, 1, Integer::sum);
        }

        // Detect actual slot collisions (different juries at same slot/room)
        Map<String, Set<Long>> juriesByRoomSlot = new HashMap<>();
        for (Soutenance s : generated) {
            if (s.getSalle() == null || s.getDate() == null) continue;
            String key = isoDate(s.getDate()) + "|" + s.getHeure() + "|" + s.getSalle().getId_salle();
            juriesByRoomSlot.computeIfAbsent(key, k -> new HashSet<>())
                    .add(s.getJury() == null ? -1L : s.getJury().getIdJury());
        }
        for (Map.Entry<String, Set<Long>> e : juriesByRoomSlot.entrySet()) {
            if (e.getValue().size() > 1) {
                result.addViolation(new ConstraintViolation("ROOM_OVERLAP", "Chevauchement de salle",
                        ConstraintPriority.HARD,
                        "Plusieurs jurys au meme creneau dans la meme salle: " + e.getKey(),
                        "Augmentez le nombre de salles ou la duree des creneaux."));
            }
        }

        // Max soutenances/salle/jour
        int maxRoom = c.getMaxSoutenancesPerRoomPerDay();
        for (Map.Entry<String, Integer> e : roomDailyCount.entrySet()) {
            if (e.getValue() > maxRoom) {
                result.addViolation(new ConstraintViolation(ConstraintIds.MAX_SOUTENANCES_PER_ROOM_PER_DAY,
                        c.labelOf(ConstraintIds.MAX_SOUTENANCES_PER_ROOM_PER_DAY),
                        c.priorityOf(ConstraintIds.MAX_SOUTENANCES_PER_ROOM_PER_DAY),
                        "La salle " + e.getKey() + " a " + e.getValue() + " soutenance(s) (limite "
                                + maxRoom + ").",
                        "Reduisez la charge ou augmentez la limite."));
            }
        }

        // ── Professor conflicts ─────────────────────────────────────────────
        Map<String, Set<Long>> juriesByProfSlot = new HashMap<>();
        for (Soutenance s : generated) {
            if (s.getJury() == null || s.getDate() == null) continue;
            String dateStr = isoDate(s.getDate());
            for (Professeur p : profsOfJury(s.getJury())) {
                String key = dateStr + "|" + s.getHeure() + "|" + p.getIdp();
                juriesByProfSlot.computeIfAbsent(key, k -> new HashSet<>()).add(s.getJury().getIdJury());
                profDaily.computeIfAbsent(p.getIdp(), k -> new HashMap<>()).merge(dateStr, 1, Integer::sum);
            }
            if (s.getJury().getRapporteur1() != null) {
                profJuryCount.merge(s.getJury().getRapporteur1().getIdp(), 1, Integer::sum);
            }
            if (s.getJury().getRapporteur2() != null) {
                profJuryCount.merge(s.getJury().getRapporteur2().getIdp(), 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Set<Long>> e : juriesByProfSlot.entrySet()) {
            if (e.getValue().size() > 1) {
                result.addViolation(new ConstraintViolation("PROF_OVERLAP",
                        "Professeur affecte au meme horaire", ConstraintPriority.HARD,
                        "Conflit pour " + e.getKey(),
                        "Reduisez la charge ou ajoutez des jours."));
            }
        }

        // Max per prof per day
        int maxProf = c.getMaxSoutenancesPerProfPerDay();
        for (Map.Entry<Long, Map<String, Integer>> e : profDaily.entrySet()) {
            for (Map.Entry<String, Integer> dayEntry : e.getValue().entrySet()) {
                if (dayEntry.getValue() > maxProf) {
                    result.addViolation(new ConstraintViolation(ConstraintIds.MAX_SOUTENANCES_PER_PROF_PER_DAY,
                            c.labelOf(ConstraintIds.MAX_SOUTENANCES_PER_PROF_PER_DAY),
                            c.priorityOf(ConstraintIds.MAX_SOUTENANCES_PER_PROF_PER_DAY),
                            "Prof " + e.getKey() + " a " + dayEntry.getValue()
                                    + " soutenances le " + dayEntry.getKey() + " (limite " + maxProf + ").",
                            "Augmentez la limite ou ajoutez des jours/professeurs."));
                }
            }
        }

        // Jury load gap (SOFT or HARD depending on user choice)
        if (!profJuryCount.isEmpty()) {
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (int v : profJuryCount.values()) { min = Math.min(min, v); max = Math.max(max, v); }
            int gap = max - min;
            if (gap > c.getMaxJuryLoadGap()) {
                result.addViolation(new ConstraintViolation(ConstraintIds.MAX_JURY_LOAD_GAP,
                        c.labelOf(ConstraintIds.MAX_JURY_LOAD_GAP),
                        c.priorityOf(ConstraintIds.MAX_JURY_LOAD_GAP),
                        "Ecart de participations jury: " + gap + " (max attendu " + c.getMaxJuryLoadGap() + ").",
                        "Repartissez la charge plus uniformement."));
            }
        }

        // Min distinct jury members + encadrant != rapporteur (HARD)
        int distinctMin = c.getMinDistinctJuryMembers();
        for (Soutenance s : generated) {
            Jury j = s.getJury();
            if (j == null) continue;
            Set<Long> ids = new HashSet<>();
            if (j.getPresident() != null) ids.add(j.getPresident().getIdp());
            if (j.getRapporteur1() != null) ids.add(j.getRapporteur1().getIdp());
            if (j.getRapporteur2() != null) ids.add(j.getRapporteur2().getIdp());
            if (ids.size() < distinctMin) {
                result.addViolation(new ConstraintViolation(ConstraintIds.MIN_DISTINCT_JURY_MEMBERS,
                        c.labelOf(ConstraintIds.MIN_DISTINCT_JURY_MEMBERS), ConstraintPriority.HARD,
                        "Jury non distinct pour " + (s.getEtudiant() == null ? "?" : s.getEtudiant().getNomE()) + ".",
                        "Le moteur a echoue a trouver 3 profs distincts."));
            }
        }
    }

    private List<Professeur> profsOfJury(Jury j) {
        List<Professeur> out = new ArrayList<>();
        if (j.getPresident() != null) out.add(j.getPresident());
        if (j.getRapporteur1() != null) out.add(j.getRapporteur1());
        if (j.getRapporteur2() != null) out.add(j.getRapporteur2());
        return out;
    }

    private void buildBlockingSuggestions(PlanningConfig cfg, int numberOfRooms, int numberOfProfs,
                                          int numberOfDays, int slotsPerDay, PlanningResult result) {
        if (!result.getUnscheduledProjects().isEmpty()) {
            int unscheduled = result.getUnscheduledProjects().size();
            result.addSuggestion(Recommendation.error(
                    "Projets non planifies",
                    unscheduled + " projet(s) n'ont pas pu etre planifie(s).",
                    "Ajoutez des jours, augmentez la capacite (salles/creneaux) ou assouplissez les contraintes."));
        }
        if (!result.getHardViolations().isEmpty()) {
            result.addSuggestion(Recommendation.error(
                    "Contraintes dures violees",
                    result.getHardViolations().size() + " contrainte(s) dure(s) violee(s).",
                    "Ajustez les valeurs des contraintes en rouge ci-dessous puis relancez."));
        }
    }

    private void logJuryDistribution(PlanningResult result, List<Professeur> allProfs,
                                     Map<Long, Integer> profJuryCount) {
        result.addDebug("--- Repartition des participations jury ---");
        profJuryCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> {
                    Professeur p = allProfs.stream()
                            .filter(pr -> pr.getIdp().equals(e.getKey()))
                            .findFirst()
                            .orElse(null);
                    if (p != null) {
                        result.addDebug("   " + p.getNom() + " " + p.getPrenom() + " -> " + e.getValue() + " fois");
                    }
                });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String projectStudentNames(List<Affectation> project) {
        List<String> names = new ArrayList<>();
        for (Affectation a : project) names.add(a.getEtudiant().getNomE());
        return String.join(" & ", names);
    }

    private boolean isDefaultSubject(String subject) {
        return subject.toLowerCase(Locale.ROOT).contains("projet de fin d");
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }

    private int slotOrder(String heure) {
        if (heure == null) return 99;
        try {
            String h = heure.replace("h", "").trim();
            // "9" or "9:30" or "930"
            if (h.contains(":")) {
                String[] parts = h.split(":");
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            }
            if (h.length() <= 2) return Integer.parseInt(h) * 60;
            return Integer.parseInt(h.substring(0, h.length() - 2)) * 60
                    + Integer.parseInt(h.substring(h.length() - 2));
        } catch (Exception e) {
            return 99;
        }
    }

    private static String isoDate(Date d) {
        if (d == null) return "";
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private int dateKeyHash(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).hashCode();
        } catch (Exception e) {
            return dateStr == null ? 0 : dateStr.hashCode();
        }
    }

    // ─── Internal value classes ─────────────────────────────────────────────

    private static class PlanningDates {
        private final List<String> validDates;
        private final List<Date> validDateObjects;

        private PlanningDates(List<String> validDates, List<Date> validDateObjects) {
            this.validDates = validDates;
            this.validDateObjects = validDateObjects;
        }
    }

    private static class PlanningChoice {
        private final int dayIdx;
        private final int slotIdx;
        private final int slotHour;
        private final String slotLabel;
        private final int startMinute;
        private final int endMinute;
        private final Salle salle;
        private final Professeur rapporteur1;
        private final Professeur rapporteur2;

        private PlanningChoice(int dayIdx, int slotIdx, int slotHour, String slotLabel, int[] slotMin,
                               int durationMinutes, Salle salle, Professeur r1, Professeur r2) {
            this.dayIdx = dayIdx;
            this.slotIdx = slotIdx;
            this.slotHour = slotHour;
            this.slotLabel = slotLabel;
            this.startMinute = slotMin[0] * 60 + slotMin[1];
            this.endMinute = startMinute + Math.max(15, durationMinutes);
            this.salle = salle;
            this.rapporteur1 = r1;
            this.rapporteur2 = r2;
        }
    }
}
