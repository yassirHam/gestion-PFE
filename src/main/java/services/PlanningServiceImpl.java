package services;

import dao.AffectationDAO;
import dao.AffectationDAOImpl;
import dao.JuryDAO;
import dao.JuryDAOImpl;
import dao.ProfesseurAvailabilityDAO;
import dao.ProfesseurAvailabilityDAOImpl;
import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import dao.SalleDAO;
import dao.SalleDAOImpl;
import dao.SoutenanceDAO;
import dao.SoutenanceDAOImpl;
import entities.AcademicSession;
import entities.Affectation;
import entities.Etudiant;
import entities.Jury;
import entities.PlanningVersion;
import entities.Professeur;
import entities.ProfesseurAvailability;
import entities.Salle;
import entities.Soutenance;
import entities.SoutenanceStatus;

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
 *
 * <h3>Operational extensions</h3>
 * <ul>
 *   <li>Soutenances of the active {@link PlanningVersion} that are
 *       <em>locked</em> are preserved across regenerations — only the
 *       remaining slots are recomputed.</li>
 *   <li>Professors flagged as {@code excluded} or with a matching
 *       {@link ProfesseurAvailability} record are removed from the jury
 *       pool for the affected day/period.</li>
 *   <li>Per-professor {@code maxSoutenancesPerDay} (when set) is applied
 *       on top of the global cap.</li>
 *   <li>Unavailable salles ({@code available = false}) are silently
 *       dropped; high-priority salles are filled first.</li>
 *   <li>Soutenances are tagged with the active session and version so the
 *       approval/freeze pipeline can operate at version granularity.</li>
 * </ul>
 */
public class PlanningServiceImpl implements PlanningService {

    private final AffectationDAO affDao;
    private final ProfesseurDAO profDao;
    private final SalleDAO salleDao;
    private final JuryDAO juryDao;
    private final SoutenanceDAO soutDao;
    private final NlpService nlpService;
    private final JurySelectionStrategy jurySelectionStrategy;
    private final ProfesseurAvailabilityDAO availabilityDao;

    private Map<Long, String> profColorMap = new LinkedHashMap<>();

    public PlanningServiceImpl() {
        this(new AffectationDAOImpl(), new ProfesseurDAOImpl(), new SalleDAOImpl(), new JuryDAOImpl(),
                new SoutenanceDAOImpl(), new NlpServiceImpl(), new DefaultJurySelectionStrategy());
    }

    public PlanningServiceImpl(AffectationDAO affDao, ProfesseurDAO profDao, SalleDAO salleDao, JuryDAO juryDao,
                               SoutenanceDAO soutDao, NlpService nlpService,
                               JurySelectionStrategy jurySelectionStrategy) {
        this(affDao, profDao, salleDao, juryDao, soutDao, nlpService, jurySelectionStrategy,
                new ProfesseurAvailabilityDAOImpl());
    }

    public PlanningServiceImpl(AffectationDAO affDao, ProfesseurDAO profDao, SalleDAO salleDao, JuryDAO juryDao,
                               SoutenanceDAO soutDao, NlpService nlpService,
                               JurySelectionStrategy jurySelectionStrategy,
                               ProfesseurAvailabilityDAO availabilityDao) {
        this.affDao = Objects.requireNonNull(affDao);
        this.profDao = Objects.requireNonNull(profDao);
        this.salleDao = Objects.requireNonNull(salleDao);
        this.juryDao = Objects.requireNonNull(juryDao);
        this.soutDao = Objects.requireNonNull(soutDao);
        this.nlpService = Objects.requireNonNull(nlpService);
        this.jurySelectionStrategy = Objects.requireNonNull(jurySelectionStrategy);
        this.availabilityDao = Objects.requireNonNull(availabilityDao);
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
        // Filter out excluded professors from the schedulable pool. Excluded
        // professors are kept in `allProfs` for verification purposes only.
        List<Professeur> schedulableProfs = new ArrayList<>();
        for (Professeur p : allProfs) {
            if (!p.isExcluded()) schedulableProfs.add(p);
        }
        if (schedulableProfs.size() < 3) {
            result.addDebug("Il faut au moins 3 professeurs disponibles pour former un jury.");
            result.addViolation(new ConstraintViolation("MIN_PROFESSORS", "Minimum 3 professeurs",
                    ConstraintPriority.HARD,
                    schedulableProfs.size() + " professeur(s) disponible(s) (sur " + allProfs.size()
                            + ", certains exclus temporairement).",
                    "Importez au moins 3 professeurs ou réintégrez les professeurs exclus."));
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

        // ── Resolve session / version (operational metadata) ───────────────
        AcademicSession activeSession;
        PlanningVersion activeVersion;
        try {
            activeSession = SessionService.getInstance().getActive();
            activeVersion = SessionService.getInstance().getCurrentVersion(
                    activeSession == null ? null : activeSession.getId());
            if (activeSession != null && activeVersion == null) {
                activeVersion = SessionService.getInstance().createVersion(activeSession, null, null);
            }
            if (activeVersion != null && activeVersion.isFrozen()) {
                result.addViolation(new ConstraintViolation("VERSION_FROZEN", "Version figée",
                        ConstraintPriority.HARD,
                        "La version courante (" + activeVersion.getDisplayName() + ") est publiée et figée.",
                        "Créez une nouvelle version dans la session active avant de relancer le planning."));
                result.setSuccess(false);
                return result;
            }
        } catch (Exception e) {
            result.addDebug("Avertissement : impossible de résoudre la session/version active : " + e.getMessage());
            activeSession = null;
            activeVersion = null;
        }

        // ── Identify locked soutenances we must preserve ────────────────────
        List<Soutenance> existing = soutDao.findAllWithDetails();
        List<Soutenance> lockedExisting = new ArrayList<>();
        Set<Long> lockedEtudiantIds = new HashSet<>();
        for (Soutenance s : existing) {
            if (s.isFrozen()) {
                lockedExisting.add(s);
                if (s.getEtudiant() != null) lockedEtudiantIds.add(s.getEtudiant().getIde());
            }
        }
        // Filter out students that already have a locked soutenance (don't reschedule them)
        if (!lockedEtudiantIds.isEmpty()) {
            int before = affectations.size();
            affectations.removeIf(a -> a.getEtudiant() != null
                    && lockedEtudiantIds.contains(a.getEtudiant().getIde()));
            result.addDebug("Préservation de " + lockedExisting.size()
                    + " soutenance(s) verrouillée(s) (" + (before - affectations.size())
                    + " affectation(s) ignorée(s)).");
        }

        // ── Reset un-locked soutenances only ────────────────────────────────
        if (lockedExisting.isEmpty()) {
            resetPlanning();
        } else {
            resetPlanningPreservingLocked(lockedExisting);
        }

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

        // Pre-fill the busy maps with the locked soutenances we keep so the
        // generator never reuses their slots/professors/salles.
        seedFromLocked(lockedExisting, profBusyAtSlot, profSlotMinutes, profDailyCount,
                profJuryCount, roomBusy, roomDailyCount, cfg);

        // Index per-professor unavailability for the active session.
        Map<Long, List<ProfesseurAvailability>> unavailabilityByProf;
        try {
            List<ProfesseurAvailability> all = availabilityDao.findBySession(
                    activeSession == null ? null : activeSession.getId());
            unavailabilityByProf = new HashMap<>();
            for (ProfesseurAvailability av : all) {
                if (av.getProfesseur() == null) continue;
                if (av.getKind() == ProfesseurAvailability.Kind.UNAVAILABLE
                        || av.getKind() == ProfesseurAvailability.Kind.EXCLUDED_GLOBAL) {
                    unavailabilityByProf.computeIfAbsent(av.getProfesseur().getIdp(), k -> new ArrayList<>())
                            .add(av);
                }
            }
        } catch (Exception e) {
            unavailabilityByProf = new HashMap<>();
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
        // Sort by deadline / priority: higher priority first (urgent projects),
        // then random shuffle inside a priority bucket so we don't always pick
        // the same students.
        projects.sort((a, b) -> Integer.compare(projectPriority(b), projectPriority(a)));
        Collections.shuffle(projects, new java.util.Random(0)); // stable for tests
        projects.sort((a, b) -> Integer.compare(projectPriority(b), projectPriority(a)));

        Map<String, SujetAnalysis> nlpBatchResults = analyzeProjectSubjects(projects, allProfs, result);
        List<Soutenance> generated = new ArrayList<>();

        // ── Schedule each project ──────────────────────────────────────────
        for (List<Affectation> project : projects) {
            Affectation mainAff = project.get(0);
            Etudiant etudiant = mainAff.getEtudiant();
            Professeur encadrant = mainAff.getEncadrant();
            SujetAnalysis nlpResult = getProjectAnalysis(etudiant, nlpBatchResults, result);

            List<Professeur> juryPool = new ArrayList<>(schedulableProfs);
            juryPool.removeIf(p -> p.getIdp().equals(encadrant.getIdp()));

            PlanningChoice bestChoice = findBestPlanningChoice(encadrant, juryPool, planningDates, slots, slotLabels,
                    salles, profBusyAtSlot, profSlotMinutes, profJuryCount, profDailyCount, roomBusy, roomDailyCount,
                    nlpResult, cfg, unavailabilityByProf);

            if (bestChoice != null) {
                generated.addAll(saveProjectPlanning(project, encadrant, bestChoice, planningDates, slotLabels,
                        profBusyAtSlot, profSlotMinutes, profDailyCount, profJuryCount, roomBusy, roomDailyCount,
                        result, activeSession, activeVersion));
            } else {
                String studentNames = projectStudentNames(project);
                result.addDebug("Impossible de planifier: " + studentNames
                        + " (aucun creneau valide dans " + cfg.getNumberOfDays() + " jour(s))");
                result.addUnscheduledProject(studentNames);
            }
        }

        // Always include the locked soutenances in the validation/result set
        // so reports reflect the full picture.
        List<Soutenance> fullPlanning = new ArrayList<>();
        fullPlanning.addAll(lockedExisting);
        fullPlanning.addAll(generated);

        // ── Validate the generated planning ─────────────────────────────────
        validatePlanning(fullPlanning, allProfs, salles, planningDates, slotLabels, cfg, result);

        // ── Persist or roll back ────────────────────────────────────────────
        if (result.hasBlockingIssues()) {
            // Don't persist; surface the violations to the user.
            result.setSuccess(false);
            buildBlockingSuggestions(cfg, salles.size(), allProfs.size(), planningDates.validDates.size(),
                    cfg.getSlotsPerDay(), result);
            result.clearSoutenances();
            // Restore the locked soutenances to the DB (we rolled back the table earlier)
            if (!lockedExisting.isEmpty()) soutDao.saveAll(lockedExisting);
            return result;
        }

        soutDao.saveAll(generated);
        result.addSoutenances(fullPlanning);
        logJuryDistribution(result, allProfs, profJuryCount);
        result.setSuccess(true);
        return result;
    }

    private int projectPriority(List<Affectation> project) {
        int max = 0;
        for (Affectation a : project) {
            if (a == null || a.getEtudiant() == null) continue;
            max = Math.max(max, a.getEtudiant().getPriority());
        }
        return max;
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
        // Honour Salle.available + ordering by priority desc.
        List<Salle> all = salleDao.findAvailable();
        if (all == null || all.isEmpty()) all = salleDao.findAll();
        List<Salle> filtered = new ArrayList<>();
        for (Salle s : all) {
            if (s.isAvailable()) filtered.add(s);
        }
        if (filtered.isEmpty()) filtered = all;
        if (selectedSalles == null || selectedSalles.isEmpty()) return new ArrayList<>(filtered);
        List<Salle> out = new ArrayList<>();
        for (Salle s : filtered) {
            if (selectedSalles.contains(s.getId_salle())) out.add(s);
        }
        return out;
    }

    private void resetPlanning() {
        soutDao.deleteAll();
        juryDao.deleteAll();
    }

    /**
     * Delete only the un-locked soutenances and orphan juries; preserve the
     * locked ones (they'll be reused as-is in the new planning).
     */
    private void resetPlanningPreservingLocked(List<Soutenance> lockedExisting) {
        Set<Long> keepSoutenanceIds = new HashSet<>();
        Set<Long> keepJuryIds = new HashSet<>();
        for (Soutenance s : lockedExisting) {
            if (s.getIds() != null) keepSoutenanceIds.add(s.getIds());
            if (s.getJury() != null && s.getJury().getIdJury() != null) keepJuryIds.add(s.getJury().getIdJury());
        }
        // First: drop every unlocked soutenance
        for (Soutenance s : soutDao.findAllWithDetails()) {
            if (!keepSoutenanceIds.contains(s.getIds())) {
                soutDao.deleteById(s.getIds());
            }
        }
        // Then: drop juries that no soutenance points to anymore
        for (Jury j : juryDao.findAll()) {
            if (!keepJuryIds.contains(j.getIdJury())) {
                // We can't selectively delete a jury via the existing DAO
                // without breaking constraints; rely on garbage collection
                // through deleteAll() if no locked jury exists. With locked
                // juries we leave orphans alone — they don't affect new
                // generation since we re-key everything.
            }
        }
    }

    private void seedFromLocked(List<Soutenance> locked,
                                Map<String, Set<Long>> profBusyAtSlot,
                                Map<Long, List<int[]>> profSlotMinutes,
                                Map<Long, Map<String, Integer>> profDailyCount,
                                Map<Long, Integer> profJuryCount,
                                Map<String, Boolean> roomBusy,
                                Map<String, Integer> roomDailyCount,
                                PlanningConfig cfg) {
        if (locked == null || locked.isEmpty()) return;
        int durationMin = Math.max(15, cfg.getSoutenanceDurationMinutes());
        for (Soutenance s : locked) {
            if (s.getDate() == null || s.getJury() == null || s.getSalle() == null) continue;
            String dateStr = isoDate(s.getDate());
            String slotLabel = s.getHeure();
            String slotKey = dateStr + "|" + slotLabel;

            int[] slotMin = parseSlotLabel(slotLabel);
            int startMin = slotMin[0] * 60 + slotMin[1];
            int endMin = startMin + durationMin;

            for (Professeur p : profsOfJury(s.getJury())) {
                profBusyAtSlot.computeIfAbsent(slotKey, k -> new HashSet<>()).add(p.getIdp());
                profSlotMinutes.computeIfAbsent(p.getIdp(), k -> new ArrayList<>())
                        .add(new int[]{dateKeyHash(dateStr), startMin, endMin});
                profDailyCount.computeIfAbsent(p.getIdp(), k -> new HashMap<>())
                        .merge(dateStr, 1, Integer::sum);
            }
            if (s.getJury().getRapporteur1() != null) {
                profJuryCount.merge(s.getJury().getRapporteur1().getIdp(), 1, Integer::sum);
            }
            if (s.getJury().getRapporteur2() != null) {
                profJuryCount.merge(s.getJury().getRapporteur2().getIdp(), 1, Integer::sum);
            }
            roomBusy.put(slotKey + "|" + s.getSalle().getId_salle(), true);
            roomDailyCount.merge(dateStr + "|" + s.getSalle().getId_salle(), 1, Integer::sum);
        }
    }

    private static int[] parseSlotLabel(String slotLabel) {
        if (slotLabel == null) return new int[]{0, 0};
        String s = slotLabel.replace("h", ":").trim();
        if (s.endsWith(":")) s = s + "00";
        try {
            String[] parts = s.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return new int[]{h, m};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
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
                                                  SujetAnalysis nlpResult, PlanningConfig cfg,
                                                  Map<Long, List<ProfesseurAvailability>> unavailabilityByProf) {
        ConstraintSet c = cfg.getConstraints();
        List<int[]> slotMinutes = cfg.computeSlotMinutes();
        int restMinutes = c.getProfRestHours() * 60;
        int durationMin = cfg.getSoutenanceDurationMinutes();
        int globalProfPerDay = c.getMaxSoutenancesPerProfPerDay();
        int maxRoomPerDay = c.getMaxSoutenancesPerRoomPerDay();

        // Per-prof daily cap (override the global if set on the entity).
        int encMaxPerDay = effectiveMaxPerDay(encadrant, globalProfPerDay);

        PlanningChoice bestChoice = null;
        int minDailyLoad = Integer.MAX_VALUE;
        int minSlotLoad = Integer.MAX_VALUE;

        for (int dayIdx = 0; dayIdx < planningDates.validDates.size(); dayIdx++) {
            String dateStr = planningDates.validDates.get(dayIdx);
            int encDailyLoad = profDailyCount.get(encadrant.getIdp()).getOrDefault(dateStr, 0);
            if (encDailyLoad >= encMaxPerDay) continue;
            if (encDailyLoad > minDailyLoad) continue;
            // Skip days where the encadrant is unavailable (any period).
            if (isProfUnavailableDay(encadrant.getIdp(), dateStr, null, unavailabilityByProf)) continue;

            for (int slotIdx = 0; slotIdx < slots.length; slotIdx++) {
                int slotHour = slots[slotIdx];
                int[] slotMin = slotMinutes.get(slotIdx);
                String slotLabel = slotLabels.get(slotIdx);

                if (!isProfAvailable(encadrant.getIdp(), dateStr, slotMin, restMinutes, durationMin,
                        profBusyAtSlot, profSlotMinutes, slotLabel)) continue;
                if (isProfUnavailableDay(encadrant.getIdp(), dateStr, slotMin, unavailabilityByProf)) continue;

                String slotKey = dateStr + "|" + slotLabel;
                int slotLoad = getSlotLoad(slotKey, salles, roomBusy);
                if (!isBetterChoice(encDailyLoad, slotLoad, minDailyLoad, minSlotLoad)) continue;

                Salle freeSalle = getFreeRoom(slotKey, dateStr, salles, roomBusy, roomDailyCount, maxRoomPerDay);
                if (freeSalle == null) continue;

                List<Professeur> available = findAvailableProfessors(juryPool, dateStr, slotMin, restMinutes,
                        durationMin, profBusyAtSlot, profSlotMinutes, slotLabel, profDailyCount,
                        globalProfPerDay, unavailabilityByProf);
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

    private static int effectiveMaxPerDay(Professeur p, int globalCap) {
        if (p == null) return globalCap;
        Integer perProf = p.getMaxSoutenancesPerDay();
        if (perProf == null || perProf <= 0) return globalCap;
        return Math.min(perProf, globalCap);
    }

    /**
     * Look up declared {@link ProfesseurAvailability} records for a given
     * professor and check whether the given (date, slot) is incompatible.
     * If {@code slotMin} is null the check is at the day level (any period).
     */
    private boolean isProfUnavailableDay(Long profId, String dateStr, int[] slotMin,
                                         Map<Long, List<ProfesseurAvailability>> unavailabilityByProf) {
        if (profId == null) return false;
        List<ProfesseurAvailability> list = unavailabilityByProf.get(profId);
        if (list == null || list.isEmpty()) return false;
        for (ProfesseurAvailability av : list) {
            if (av.getTheDate() == null) continue;
            if (!isoDate(av.getTheDate()).equals(dateStr)) continue;
            ProfesseurAvailability.Period period = av.getPeriod();
            if (period == null || period == ProfesseurAvailability.Period.ALL_DAY) return true;
            if (slotMin == null) return true; // entire-day check
            int hour = slotMin[0];
            if (period == ProfesseurAvailability.Period.MORNING && hour < 13) return true;
            if (period == ProfesseurAvailability.Period.AFTERNOON && hour >= 13) return true;
        }
        return false;
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
                                                     int globalMaxProfPerDay,
                                                     Map<Long, List<ProfesseurAvailability>> unavailabilityByProf) {
        List<Professeur> out = new ArrayList<>();
        for (Professeur p : juryPool) {
            int dailyLoad = profDailyCount.get(p.getIdp()).getOrDefault(dateStr, 0);
            if (dailyLoad >= effectiveMaxPerDay(p, globalMaxProfPerDay)) continue;
            if (isProfUnavailableDay(p.getIdp(), dateStr, slotMin, unavailabilityByProf)) continue;
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
        for (int[] existing : profSlotMinutes.getOrDefault(profId, Collections.emptyList())) {
            int day = existing[0];
            if (day != dateKeyHash(dateStr)) continue;
            int existStart = existing[1];
            int existEnd = existing[2];
            int gap;
            if (newEnd <= existStart) {
                gap = existStart - newEnd;
            } else if (newStart >= existEnd) {
                gap = newStart - existEnd;
            } else {
                gap = -1;
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

    /**
     * Pick the highest-priority salle still free at this slot. Salles with
     * priority=0 keep the original behaviour (linear scan).
     */
    private Salle getFreeRoom(String slotKey, String dateStr, List<Salle> salles, Map<String, Boolean> roomBusy,
                              Map<String, Integer> roomDailyCount, int maxRoomPerDay) {
        // The DAO already returns salles sorted by priority desc, num_salle.
        for (Salle s : salles) {
            if (!s.isAvailable()) continue;
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
                                                 PlanningResult result,
                                                 AcademicSession activeSession,
                                                 PlanningVersion activeVersion) {
        String dateStr = planningDates.validDates.get(choice.dayIdx);
        String slotLabel = choice.slotLabel;
        String slotKey = dateStr + "|" + slotLabel;

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
            s.setStatus(SoutenanceStatus.PLANNED);
            s.setVersion(activeVersion);
            s.setSession(activeSession);
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
        profDailyCount.computeIfAbsent(profId, k -> new HashMap<>()).merge(dateStr, 1, Integer::sum);
    }

    private void ensureSallesExistent(PlanningConfig cfg) {
        if (salleDao.count() == 0) {
            List<Salle> defaults = new ArrayList<>();
            for (String name : cfg.getDefaultRooms()) {
                Salle s = new Salle();
                s.setNum_salle(name);
                s.setBlock("");
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

    private void validatePlanning(List<Soutenance> generated, List<Professeur> allProfs, List<Salle> salles,
                                  PlanningDates planningDates, List<String> slotLabels, PlanningConfig cfg,
                                  PlanningResult result) {
        ConstraintSet c = cfg.getConstraints();

        Map<String, Integer> roomDailyCount = new HashMap<>();
        Map<Long, Map<String, Integer>> profDaily = new HashMap<>();
        Map<Long, Integer> profJuryCount = new HashMap<>();

        Set<String> roomProjectSeen = new HashSet<>();
        for (Soutenance s : generated) {
            if (s.getSalle() == null || s.getDate() == null) continue;
            String dateStr = isoDate(s.getDate());
            String key = dateStr + "|" + s.getHeure() + "|" + s.getSalle().getId_salle()
                    + "|" + (s.getJury() == null ? "0" : s.getJury().getIdJury());
            if (roomProjectSeen.add(key)) {
                String dailyKey = dateStr + "|" + s.getSalle().getId_salle();
                roomDailyCount.merge(dailyKey, 1, Integer::sum);
            }
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

        // ── New operational constraints ─────────────────────────────────────
        validateOperationalConstraints(generated, cfg, result);
    }

    /**
     * Operational constraint pass: max-per-half-day, jury repetition,
     * min president grade, external member presence, language compatibility,
     * session deadline, consecutive-slot rule.
     */
    private void validateOperationalConstraints(List<Soutenance> generated, PlanningConfig cfg,
                                                 PlanningResult result) {
        ConstraintSet c = cfg.getConstraints();

        // Max soutenances per prof per half-day
        int maxHalfday = c.getMaxSoutenancesPerHalfday();
        ConstraintPriority halfdayPriority = c.priorityOf(ConstraintIds.MAX_SOUTENANCES_PER_HALFDAY);
        Map<String, Integer> profHalfdayCount = new HashMap<>();
        for (Soutenance s : generated) {
            if (s.getDate() == null || s.getJury() == null) continue;
            String dateStr = isoDate(s.getDate());
            String halfday = isMorningSlot(s.getHeure()) ? "AM" : "PM";
            for (Professeur p : profsOfJury(s.getJury())) {
                if (p == null) continue;
                String key = dateStr + "|" + halfday + "|" + p.getIdp();
                profHalfdayCount.merge(key, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> e : profHalfdayCount.entrySet()) {
            if (e.getValue() > maxHalfday) {
                result.addViolation(new ConstraintViolation(ConstraintIds.MAX_SOUTENANCES_PER_HALFDAY,
                        c.labelOf(ConstraintIds.MAX_SOUTENANCES_PER_HALFDAY),
                        halfdayPriority,
                        e.getKey() + " : " + e.getValue() + " soutenances (max " + maxHalfday + " par demi-journee).",
                        "Etalez la charge sur plusieurs demi-journees ou augmentez le seuil."));
            }
        }

        // Jury repetition cap
        int maxRep = c.getMaxJuryRepetition();
        ConstraintPriority repPriority = c.priorityOf(ConstraintIds.MAX_JURY_REPETITION);
        Map<String, Integer> juryTriadCount = new HashMap<>();
        Set<String> seenSoutenanceJury = new HashSet<>();
        for (Soutenance s : generated) {
            entities.Jury j = s.getJury();
            if (j == null) continue;
            String dedupKey = j.getIdJury() + "|" + (s.getEtudiant() == null ? "0" : s.getEtudiant().getIde());
            if (!seenSoutenanceJury.add(dedupKey)) continue;
            String triadKey = juryTriadKey(j);
            juryTriadCount.merge(triadKey, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : juryTriadCount.entrySet()) {
            if (e.getValue() > maxRep) {
                result.addViolation(new ConstraintViolation(ConstraintIds.MAX_JURY_REPETITION,
                        c.labelOf(ConstraintIds.MAX_JURY_REPETITION),
                        repPriority,
                        "La meme triade de jury (" + e.getKey() + ") apparait " + e.getValue()
                                + " fois (max " + maxRep + ").",
                        "Diversifiez la composition des jurys ou augmentez le seuil."));
            }
        }

        // President min grade
        entities.ProfesseurGrade minGrade = c.getMinPresidentGrade();
        if (minGrade != null) {
            ConstraintPriority gradePriority = c.priorityOf(ConstraintIds.MIN_PRESIDENT_GRADE);
            for (Soutenance s : generated) {
                entities.Jury j = s.getJury();
                if (j == null || j.getPresident() == null) continue;
                Professeur pres = j.getPresident();
                if (pres.getGrade() == null
                        || pres.getGrade().getSeniorityScore() < minGrade.getSeniorityScore()) {
                    result.addViolation(new ConstraintViolation(ConstraintIds.MIN_PRESIDENT_GRADE,
                            c.labelOf(ConstraintIds.MIN_PRESIDENT_GRADE),
                            gradePriority,
                            "President " + pres.getNom() + " (" + (pres.getGrade() == null ? "?" : pres.getGrade())
                                    + ") en-dessous du grade minimum " + minGrade + ".",
                            "Designez un encadrant de grade au moins " + minGrade + " pour ce projet."));
                }
            }
        }

        // External member presence
        if (c.isRequireExternalMember()) {
            ConstraintPriority extPriority = c.priorityOf(ConstraintIds.REQUIRE_EXTERNAL_MEMBER);
            for (Soutenance s : generated) {
                entities.Jury j = s.getJury();
                if (j == null) continue;
                boolean hasExternal = false;
                for (Professeur p : profsOfJury(j)) {
                    if (p != null && !p.isInternal()) { hasExternal = true; break; }
                }
                if (!hasExternal) {
                    result.addViolation(new ConstraintViolation(ConstraintIds.REQUIRE_EXTERNAL_MEMBER,
                            c.labelOf(ConstraintIds.REQUIRE_EXTERNAL_MEMBER),
                            extPriority,
                            "Aucun membre externe pour la soutenance #"
                                    + (s.getIds() == null ? "?" : s.getIds()) + ".",
                            "Ajoutez un professeur externe au jury ou desactivez la regle."));
                }
            }
        }

        // Language requirement
        if (c.isRespectLanguageRequirement()) {
            ConstraintPriority langPriority = c.priorityOf(ConstraintIds.RESPECT_LANGUAGE_REQUIREMENT);
            for (Soutenance s : generated) {
                if (s.getEtudiant() == null) continue;
                String lang = s.getEtudiant().getLanguage();
                if (lang == null || lang.equalsIgnoreCase("fr")) continue;
                entities.Jury j = s.getJury();
                if (j == null) continue;
                boolean ok = false;
                for (Professeur p : profsOfJury(j)) {
                    if (p == null) continue;
                    if (p.speaksLanguage(lang) || p.speaksLanguage("en") || p.speaksLanguage("ag")) {
                        ok = true; break;
                    }
                }
                if (!ok) {
                    result.addViolation(new ConstraintViolation(ConstraintIds.RESPECT_LANGUAGE_REQUIREMENT,
                            c.labelOf(ConstraintIds.RESPECT_LANGUAGE_REQUIREMENT),
                            langPriority,
                            "Aucun membre du jury ne parle '" + lang + "' (sujet de "
                                    + s.getEtudiant().getNomE() + ").",
                            "Ajoutez un membre parlant la langue requise ou desactivez la regle."));
                }
            }
        }

        // Session deadline
        if (c.isRespectSessionDeadline()) {
            ConstraintPriority deadlinePriority = c.priorityOf(ConstraintIds.RESPECT_SESSION_DEADLINE);
            for (Soutenance s : generated) {
                if (s.getSession() == null || s.getSession().getDeadlineDate() == null) continue;
                if (s.getDate() != null && s.getDate().after(s.getSession().getDeadlineDate())) {
                    result.addViolation(new ConstraintViolation(ConstraintIds.RESPECT_SESSION_DEADLINE,
                            c.labelOf(ConstraintIds.RESPECT_SESSION_DEADLINE),
                            deadlinePriority,
                            "Soutenance prevue le " + isoDate(s.getDate()) + " apres la date butoir "
                                    + isoDate(s.getSession().getDeadlineDate()) + ".",
                            "Avancez la date ou repoussez la deadline de la session."));
                }
            }
        }

        // Avoid same prof on strictly consecutive slots
        if (c.isForbidConsecutiveSlots()) {
            ConstraintPriority consecPriority = c.priorityOf(ConstraintIds.FORBID_CONSECUTIVE_SLOTS);
            int stride = cfg.getSlotStrideMinutes();
            Map<Long, List<int[]>> byProf = new HashMap<>();
            for (Soutenance s : generated) {
                if (s.getDate() == null || s.getJury() == null || s.getHeure() == null) continue;
                int[] slot = parseSlotLabelLocal(s.getHeure());
                int startMin = slot[0] * 60 + slot[1];
                String dateStr = isoDate(s.getDate());
                int dayKey = dateKeyHash(dateStr);
                for (Professeur p : profsOfJury(s.getJury())) {
                    if (p == null) continue;
                    byProf.computeIfAbsent(p.getIdp(), k -> new ArrayList<>())
                            .add(new int[]{dayKey, startMin});
                }
            }
            for (Map.Entry<Long, List<int[]>> e : byProf.entrySet()) {
                List<int[]> list = e.getValue();
                list.sort(Comparator.comparingInt((int[] a) -> a[0]).thenComparingInt(a -> a[1]));
                for (int i = 1; i < list.size(); i++) {
                    int[] a = list.get(i - 1);
                    int[] b = list.get(i);
                    if (a[0] == b[0] && (b[1] - a[1]) <= stride) {
                        result.addViolation(new ConstraintViolation(ConstraintIds.FORBID_CONSECUTIVE_SLOTS,
                                c.labelOf(ConstraintIds.FORBID_CONSECUTIVE_SLOTS),
                                consecPriority,
                                "Prof #" + e.getKey() + " : 2 soutenances consecutives a "
                                        + a[1] / 60 + "h" + String.format(java.util.Locale.ROOT, "%02d", a[1] % 60)
                                        + " et " + b[1] / 60 + "h" + String.format(java.util.Locale.ROOT, "%02d", b[1] % 60),
                                "Ajoutez une pause ou deplacez l'une des deux soutenances."));
                        break; // one warning per prof is enough
                    }
                }
            }
        }
    }

    private static String juryTriadKey(entities.Jury j) {
        Long p = j.getPresident() == null ? null : j.getPresident().getIdp();
        Long r1 = j.getRapporteur1() == null ? null : j.getRapporteur1().getIdp();
        Long r2 = j.getRapporteur2() == null ? null : j.getRapporteur2().getIdp();
        long[] arr = new long[]{p == null ? -1 : p, r1 == null ? -1 : r1, r2 == null ? -1 : r2};
        java.util.Arrays.sort(arr);
        return arr[0] + "-" + arr[1] + "-" + arr[2];
    }

    private static boolean isMorningSlot(String heure) {
        if (heure == null) return true;
        int[] slot = parseSlotLabelLocal(heure);
        return slot[0] < 13;
    }

    private static int[] parseSlotLabelLocal(String slotLabel) {
        if (slotLabel == null) return new int[]{0, 0};
        String s = slotLabel.replace("h", ":").trim();
        if (s.endsWith(":")) s = s + "00";
        try {
            String[] parts = s.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return new int[]{h, m};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    private List<Professeur> profsOfJury(Jury j) {
        List<Professeur> out = new ArrayList<>();
        if (j.getPresident() != null) out.add(j.getPresident());
        if (j.getRapporteur1() != null) out.add(j.getRapporteur1());
        if (j.getRapporteur2() != null) out.add(j.getRapporteur2());
        if (j.getInvite() != null) out.add(j.getInvite());
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
