package services;

import dao.*;
import entities.*;

import java.util.*;

/**
 * Generates a soutenance planning from existing affectations.
 *
 * The orchestration stays here, while variable business rules are delegated to
 * configuration and strategy objects so the planning can be extended without
 * rewriting this service.
 */
public class PlanningServiceImpl implements PlanningService {

    private final AffectationDAO affDao;
    private final ProfesseurDAO profDao;
    private final SalleDAO salleDao;
    private final JuryDAO juryDao;
    private final SoutenanceDAO soutDao;
    private final NlpService nlpService;
    private final PlanningConfig config;
    private final JurySelectionStrategy jurySelectionStrategy;

    private Map<Long, String> profColorMap = new LinkedHashMap<>();

    public PlanningServiceImpl() {
        this(new AffectationDAOImpl(), new ProfesseurDAOImpl(), new SalleDAOImpl(), new JuryDAOImpl(),
                new SoutenanceDAOImpl(), new NlpServiceImpl(), PlanningConfig.defaults(),
                new DefaultJurySelectionStrategy());
    }

    public PlanningServiceImpl(AffectationDAO affDao, ProfesseurDAO profDao, SalleDAO salleDao, JuryDAO juryDao,
                               SoutenanceDAO soutDao, NlpService nlpService, PlanningConfig config,
                               JurySelectionStrategy jurySelectionStrategy) {
        this.affDao = Objects.requireNonNull(affDao);
        this.profDao = Objects.requireNonNull(profDao);
        this.salleDao = Objects.requireNonNull(salleDao);
        this.juryDao = Objects.requireNonNull(juryDao);
        this.soutDao = Objects.requireNonNull(soutDao);
        this.nlpService = Objects.requireNonNull(nlpService);
        this.config = Objects.requireNonNull(config);
        this.jurySelectionStrategy = Objects.requireNonNull(jurySelectionStrategy);
    }

    @Override
    public List<Soutenance> genererPlanning(List<String> filieres, List<String> log, List<Long> selectedSalles,
                                            String startDate) {
        List<Affectation> affectations = filterAffectationsByFiliere(affDao.findAllWithDetails(), filieres);
        if (affectations.isEmpty()) {
            log.add("Aucune affectation trouvee pour les filieres selectionnees.");
            return Collections.emptyList();
        }

        List<Professeur> allProfs = profDao.findAll();
        if (allProfs.size() < 3) {
            log.add("Il faut au moins 3 professeurs.");
            return Collections.emptyList();
        }

        ensureSallesExistent();
        List<Salle> salles = selectSalles(selectedSalles);
        if (salles.isEmpty()) {
            log.add("Aucune salle selectionnee ou disponible.");
            return Collections.emptyList();
        }

        buildColorMap(allProfs);
        resetPlanning();

        Map<String, Set<Long>> profBusyAtSlot = new HashMap<>();
        Map<Long, List<String>> profSchedule = new HashMap<>();
        Map<Long, Integer> profJuryCount = new HashMap<>();
        Map<Long, Map<String, Integer>> profDailyCount = new HashMap<>();
        Map<String, Boolean> roomBusy = new HashMap<>();

        for (Professeur p : allProfs) {
            profJuryCount.put(p.getIdp(), 0);
            profDailyCount.put(p.getIdp(), new HashMap<>());
        }

        PlanningDates planningDates = buildPlanningDates(startDate, log);
        List<List<Affectation>> projects = groupAffectationsByProject(affectations);
        Collections.shuffle(projects);

        Map<String, SujetAnalysis> nlpBatchResults = analyzeProjectSubjects(projects, allProfs, log);
        List<Soutenance> result = new ArrayList<>();
        int[] slots = config.getSlots();

        // Sort projects so encadrants with the most students are scheduled first.
        // Their availability is the scarcest resource, and processing them early
        // leaves more flexibility for jury balancing later.
        Map<Long, Integer> encadrantProjectCount = countEncadrantProjects(projects);
        projects.sort(Comparator.comparingInt(
                (List<Affectation> proj) -> -encadrantProjectCount.getOrDefault(encadrantIdOf(proj), 0)));

        // Pre-populate profJuryCount with the expected number of soutenances each professor
        // will preside over (one per student they supervise). This way, the load balancing
        // accounts for total participation (president + rapporteur), matching what the
        // dashboard chart displays. Without this, an encadrant of 5 students starts at 0
        // and gets piled with rapporteur duties on top of their 5 future president roles.
        for (List<Affectation> project : projects) {
            for (Affectation aff : project) {
                if (aff.getEncadrant() != null && aff.getEncadrant().getIdp() != null) {
                    profJuryCount.merge(aff.getEncadrant().getIdp(), 1, Integer::sum);
                }
            }
        }

        for (List<Affectation> project : projects) {
            Affectation mainAff = project.get(0);
            Etudiant etudiant = mainAff.getEtudiant();
            Professeur encadrant = mainAff.getEncadrant();
            SujetAnalysis nlpResult = getProjectAnalysis(etudiant, nlpBatchResults, log);

            List<Professeur> juryPool = new ArrayList<>(allProfs);
            juryPool.removeIf(p -> p.getIdp().equals(encadrant.getIdp()));

            // Global min load across the jury pool (NOT just the slot-available subset).
            // Passing this to the strategy keeps the load gap consistent across the entire
            // planning instead of drifting locally per slot.
            int globalMinLoad = computeGlobalMinLoad(juryPool, profJuryCount);

            PlanningChoice bestChoice = findBestPlanningChoice( encadrant, juryPool, planningDates, slots, salles,
                    profBusyAtSlot,profSchedule, profJuryCount, profDailyCount, roomBusy,nlpResult,
                    globalMinLoad);

            if (bestChoice != null) {
                result.addAll(saveProjectPlanning( project, encadrant, bestChoice, planningDates, profBusyAtSlot,
                        profSchedule, profDailyCount, profJuryCount, roomBusy,
                        log));
            } else {
                log.add("Impossible de planifier: " + projectStudentNames(project)
                        + " (aucun creneau valide dans les " + config.getMaxDays() + " jours)");
            }
        }

        soutDao.saveAll(result);
        logJuryDistribution(log, allProfs, profJuryCount);
        return result;
    }

    private List<Affectation> filterAffectationsByFiliere(List<Affectation> allAffectations, List<String> filieres) {
        if (filieres == null || filieres.isEmpty()) {
            return allAffectations;
        }

        List<Affectation> affectations = new ArrayList<>();
        for (Affectation a : allAffectations) {
            if (filieres.contains(a.getEtudiant().getFiliere())) {
                affectations.add(a);
            }
        }
        return affectations;
    }

    private List<Salle> selectSalles(List<Long> selectedSalles) {
        List<Salle> allSalles = salleDao.findAll();
        if (selectedSalles == null || selectedSalles.isEmpty()) {
            return new ArrayList<>(allSalles);
        }

        List<Salle> salles = new ArrayList<>();
        for (Salle s : allSalles) {
            if (selectedSalles.contains(s.getId_salle())) {
                salles.add(s);
            }
        }
        return salles;
    }

    private void resetPlanning() {
        soutDao.deleteAll();
        juryDao.deleteAll();
    }

    private PlanningDates buildPlanningDates(String startDate, List<String> log) {
        int startYear = config.getDefaultStartYear();
        int startMonth = config.getDefaultStartMonth();
        int startDay = config.getDefaultStartDay();

        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                String[] parts = startDate.split("-");
                if (parts.length == 3) {
                    startYear = Integer.parseInt(parts[0]);
                    startMonth = Integer.parseInt(parts[1]) - 1;
                    startDay = Integer.parseInt(parts[2]);
                }
            } catch (Exception e) {
                log.add("Format de date invalide, utilisation de la date par defaut.");
            }
        }

        Calendar baseCal = Calendar.getInstance();
        baseCal.set(startYear, startMonth, startDay, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        List<String> validDates = new ArrayList<>();
        List<Date> validDateObjects = new ArrayList<>();
        int daysAdded = 0;
        int offset = 0;

        while (daysAdded < config.getMaxDays()) {
            Calendar day = (Calendar) baseCal.clone();
            day.add(Calendar.DAY_OF_MONTH, offset++);
            int dow = day.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                continue;
            }

            validDates.add(String.format("%04d-%02d-%02d",
                    day.get(Calendar.YEAR),
                    day.get(Calendar.MONTH) + 1,
                    day.get(Calendar.DAY_OF_MONTH)));
            validDateObjects.add(day.getTime());
            daysAdded++;
        }

        return new PlanningDates(validDates, validDateObjects);
    }

    private List<List<Affectation>> groupAffectationsByProject(List<Affectation> affectations) {
        Map<String, Affectation> affByCne = new HashMap<>();
        for (Affectation a : affectations) {
            affByCne.put(a.getEtudiant().getCne(), a);
        }

        List<List<Affectation>> projects = new ArrayList<>();
        Set<String> processedCne = new HashSet<>();

        for (Affectation a : affectations) {
            String cne = a.getEtudiant().getCne();
            if (processedCne.contains(cne)) {
                continue;
            }

            List<Affectation> project = new ArrayList<>();
            project.add(a);
            processedCne.add(cne);

            if (a.getEtudiant().hasBinome() && affByCne.containsKey(a.getEtudiant().getBinome_cne())) {
                Affectation partnerAff = affByCne.get(a.getEtudiant().getBinome_cne());
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
        if (project == null || project.size() < 2) {
            return;
        }

        String subject = "";
        for (Affectation affectation : project) {
            if (affectation.getEtudiant() == null) {
                continue;
            }
            String candidate = safe(affectation.getEtudiant().getSujet_stage());
            if (!candidate.isEmpty() && !isDefaultSubject(candidate)) {
                subject = candidate;
                break;
            }
            if (subject.isEmpty()) {
                subject = candidate;
            }
        }

        if (!subject.isEmpty()) {
            for (Affectation affectation : project) {
                if (affectation.getEtudiant() != null) {
                    affectation.getEtudiant().setSujet_stage(subject);
                }
            }
        }
    }

    private boolean isDefaultSubject(String subject) {
        return subject.toLowerCase(Locale.ROOT).contains("projet de fin d");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Map<String, SujetAnalysis> analyzeProjectSubjects(List<List<Affectation>> projects,
                                                              List<Professeur> allProfs,
                                                              List<String> log) {
        List<String> specialitesDispo = new ArrayList<>();
        for (Professeur p : allProfs) {
            if (p.getSpecialite() != null && !p.getSpecialite().trim().isEmpty()) {
                specialitesDispo.add(p.getSpecialite().trim());
            }
        }
        specialitesDispo = new ArrayList<>(new LinkedHashSet<>(specialitesDispo));

        List<String> allUniqueSujets = new ArrayList<>();
        for (List<Affectation> project : projects) {
            String subject = project.get(0).getEtudiant().getSujet_stage();
            if (subject != null && !subject.trim().isEmpty() && !allUniqueSujets.contains(subject)) {
                allUniqueSujets.add(subject);
            }
        }

        if (allUniqueSujets.isEmpty()) {
            return new HashMap<>();
        }

        try {
            log.add("Demarrage analyse NLP Batch pour " + allUniqueSujets.size() + " sujets uniques...");
            Map<String, SujetAnalysis> nlpBatchResults = nlpService.analyzeSujetsBatch(allUniqueSujets, specialitesDispo);
            log.add("Analyse NLP Batch terminee.");
            return nlpBatchResults;
        } catch (Exception e) {
            log.add("Erreur NLP Batch : " + e.getMessage());
            return new HashMap<>();
        }
    }

    private SujetAnalysis getProjectAnalysis(Etudiant etudiant,
                                             Map<String, SujetAnalysis> nlpBatchResults,
                                             List<String> log) {
        String sujet = etudiant.getSujet_stage();
        if (sujet == null || sujet.trim().isEmpty()) {
            return null;
        }

        SujetAnalysis nlpResult = nlpBatchResults.get(sujet);
        if (nlpResult != null) {
            log.add("NLP [" + etudiant.getNomE() + "] sujet='" + sujet + "' -> " + nlpResult);
            if (nlpResult.getLanguage() != null) {
                etudiant.setLanguage(nlpResult.getLanguage());
            }
        }
        return nlpResult;
    }

    private PlanningChoice findBestPlanningChoice(Professeur encadrant,
                                                  List<Professeur> juryPool,
                                                  PlanningDates planningDates,
                                                  int[] slots,
                                                  List<Salle> salles,
                                                  Map<String, Set<Long>> profBusyAtSlot,
                                                  Map<Long, List<String>> profSchedule,
                                                  Map<Long, Integer> profJuryCount,
                                                  Map<Long, Map<String, Integer>> profDailyCount,
                                                  Map<String, Boolean> roomBusy,
                                                  SujetAnalysis nlpResult,
                                                  int globalMinLoad) {
        PlanningChoice bestChoice = null;
        int bestJuryMaxLoad = Integer.MAX_VALUE;
        int minDailyLoad = Integer.MAX_VALUE;
        int minSlotLoad = Integer.MAX_VALUE;

        for (int dayIdx = 0; dayIdx < planningDates.validDates.size(); dayIdx++) {
            String dateStr = planningDates.validDates.get(dayIdx);
            int encadrantDailyLoad = profDailyCount.get(encadrant.getIdp()).getOrDefault(dateStr, 0);

            for (int slot : slots) {
                if (!isProfAvailable(encadrant.getIdp(), dateStr, slot, profBusyAtSlot, profSchedule)) {
                    continue;
                }

                String slotKey = dateStr + "|" + slot;
                int slotLoad = getSlotLoad(slotKey, salles, roomBusy);

                Salle freeSalle = getFreeRoom(slotKey, salles, roomBusy);
                if (freeSalle == null) {
                    continue;
                }

                List<Professeur> available = findAvailableProfessors(juryPool, dateStr, slot, profBusyAtSlot, profSchedule);
                if (available.size() < 2) {
                    continue;
                }

                Professeur[] pickedJury = jurySelectionStrategy.selectJury(
                        encadrant, available, profJuryCount, globalMinLoad, nlpResult, config);
                if (pickedJury == null) {
                    continue;
                }

                int juryMaxLoad = Math.max(
                        profJuryCount.getOrDefault(pickedJury[0].getIdp(), 0),
                        profJuryCount.getOrDefault(pickedJury[1].getIdp(), 0)
                );

                if (isBetterChoice(juryMaxLoad, encadrantDailyLoad, slotLoad,
                        bestJuryMaxLoad, minDailyLoad, minSlotLoad)) {
                    bestChoice = new PlanningChoice(dayIdx, slot, freeSalle, pickedJury[0], pickedJury[1]);
                    bestJuryMaxLoad = juryMaxLoad;
                    minDailyLoad = encadrantDailyLoad;
                    minSlotLoad = slotLoad;
                }
            }
        }

        return bestChoice;
    }

    /**
     * Lexicographic preference: pick the slot whose chosen jury has the lowest
     * max load first. This is the dominant criterion for fairness. Ties are
     * broken by encadrant daily load, then by slot occupancy.
     */
    private boolean isBetterChoice(int juryMaxLoad,
                                   int encadrantDailyLoad,
                                   int slotLoad,
                                   int bestJuryMaxLoad,
                                   int minDailyLoad,
                                   int minSlotLoad) {
        if (juryMaxLoad != bestJuryMaxLoad) {
            return juryMaxLoad < bestJuryMaxLoad;
        }
        if (encadrantDailyLoad != minDailyLoad) {
            return encadrantDailyLoad < minDailyLoad;
        }
        return slotLoad < minSlotLoad;
    }

    private int computeGlobalMinLoad(List<Professeur> juryPool, Map<Long, Integer> profJuryCount) {
        int min = Integer.MAX_VALUE;
        for (Professeur p : juryPool) {
            int load = profJuryCount.getOrDefault(p.getIdp(), 0);
            if (load < min) min = load;
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private Long encadrantIdOf(List<Affectation> project) {
        if (project == null || project.isEmpty()) return null;
        Affectation main = project.get(0);
        if (main.getEncadrant() == null) return null;
        return main.getEncadrant().getIdp();
    }

    private Map<Long, Integer> countEncadrantProjects(List<List<Affectation>> projects) {
        Map<Long, Integer> count = new HashMap<>();
        for (List<Affectation> p : projects) {
            Long id = encadrantIdOf(p);
            if (id != null) {
                count.merge(id, 1, Integer::sum);
            }
        }
        return count;
    }

    private List<Professeur> findAvailableProfessors(List<Professeur> juryPool,
                                                     String dateStr,
                                                     int slot,
                                                     Map<String, Set<Long>> profBusyAtSlot,
                                                     Map<Long, List<String>> profSchedule) {
        List<Professeur> available = new ArrayList<>();
        for (Professeur p : juryPool) {
            if (isProfAvailable(p.getIdp(), dateStr, slot, profBusyAtSlot, profSchedule)) {
                available.add(p);
            }
        }
        return available;
    }

    private List<Soutenance> saveProjectPlanning(List<Affectation> project,
                                                 Professeur encadrant,
                                                 PlanningChoice choice,
                                                 PlanningDates planningDates,
                                                 Map<String, Set<Long>> profBusyAtSlot,
                                                 Map<Long, List<String>> profSchedule,
                                                 Map<Long, Map<String, Integer>> profDailyCount,
                                                 Map<Long, Integer> profJuryCount,
                                                 Map<String, Boolean> roomBusy,
                                                 List<String> log) {
        String dateStr = planningDates.validDates.get(choice.dayIdx);
        String slotKey = dateStr + "|" + choice.slot;

        markProfBusy(encadrant.getIdp(), dateStr, choice.slot, profBusyAtSlot, profSchedule, profDailyCount);
        markProfBusy(choice.rapporteur1.getIdp(), dateStr, choice.slot, profBusyAtSlot, profSchedule, profDailyCount);
        markProfBusy(choice.rapporteur2.getIdp(), dateStr, choice.slot, profBusyAtSlot, profSchedule, profDailyCount);

        // Increment by project size so that binomes count for 2 (matching the dashboard chart
        // which counts per-soutenance, not per-project). The encadrant count was already
        // pre-loaded based on the number of students they supervise, so we don't add it again.
        int projectSize = project.size();
        profJuryCount.merge(choice.rapporteur1.getIdp(), projectSize, Integer::sum);
        profJuryCount.merge(choice.rapporteur2.getIdp(), projectSize, Integer::sum);
        roomBusy.put(slotKey + "|" + choice.salle.getId_salle(), true);

        Jury jury = new Jury();
        jury.setPresident(encadrant);
        jury.setRapporteur1(choice.rapporteur1);
        jury.setRapporteur2(choice.rapporteur2);
        jury = juryDao.save(jury);

        List<Soutenance> soutenances = new ArrayList<>();
        for (Affectation aff : project) {
            Soutenance sout = new Soutenance();
            sout.setDate(planningDates.validDateObjects.get(choice.dayIdx));
            sout.setHeure(choice.slot + "h");
            sout.setSalle(choice.salle);
            sout.setEtudiant(aff.getEtudiant());
            sout.setJury(jury);
            soutenances.add(sout);
        }

        log.add(projectStudentNames(project) + " -> " + dateStr + " " + choice.slot + "h | Salle: "
                + choice.salle.getNum_salle() + " | Enc: " + encadrant.getNom()
                + " | Jury: " + choice.rapporteur1.getNom() + ", " + choice.rapporteur2.getNom());
        return soutenances;
    }

    private boolean isProfAvailable(Long profId,
                                    String dateStr,
                                    int slot,
                                    Map<String, Set<Long>> profBusyAtSlot,
                                    Map<Long, List<String>> profSchedule) {
        if (profBusyAtSlot.getOrDefault(dateStr + "|" + slot, Collections.emptySet()).contains(profId)) {
            return false;
        }

        for (String existing : profSchedule.getOrDefault(profId, Collections.emptyList())) {
            if (!existing.startsWith(dateStr + "|")) {
                continue;
            }
            int existSlot = Integer.parseInt(existing.split("\\|")[1]);
            if (Math.abs(existSlot - slot) == 1) {
                return false;
            }
        }
        return true;
    }

    private int getSlotLoad(String slotKey, List<Salle> salles, Map<String, Boolean> roomBusy) {
        int count = 0;
        for (Salle s : salles) {
            if (roomBusy.getOrDefault(slotKey + "|" + s.getId_salle(), false)) {
                count++;
            }
        }
        return count;
    }

    private Salle getFreeRoom(String slotKey, List<Salle> salles, Map<String, Boolean> roomBusy) {
        for (Salle s : salles) {
            if (!roomBusy.getOrDefault(slotKey + "|" + s.getId_salle(), false)) {
                return s;
            }
        }
        return null;
    }

    private void markProfBusy(Long profId,
                              String dateStr,
                              int slot,
                              Map<String, Set<Long>> profBusyAtSlot,
                              Map<Long, List<String>> profSchedule,
                              Map<Long, Map<String, Integer>> profDailyCount) {
        profBusyAtSlot.computeIfAbsent(dateStr + "|" + slot, k -> new HashSet<>()).add(profId);
        profSchedule.computeIfAbsent(profId, k -> new ArrayList<>()).add(dateStr + "|" + slot);
        profDailyCount.get(profId).merge(dateStr, 1, Integer::sum);
    }

    private void ensureSallesExistent() {
        if (salleDao.count() == 0) {
            List<Salle> defaults = new ArrayList<>();
            for (String name : config.getDefaultRooms()) {
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

        List<String> palette = config.getProfessorColorPalette();
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

    private void logJuryDistribution(List<String> log,
                                     List<Professeur> allProfs,
                                     Map<Long, Integer> profJuryCount) {
        log.add("--- Repartition des participations jury ---");
        profJuryCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> {
                    Professeur p = allProfs.stream()
                            .filter(pr -> pr.getIdp().equals(e.getKey()))
                            .findFirst()
                            .orElse(null);
                    if (p != null) {
                        log.add("   " + p.getNom() + " " + p.getPrenom() + " -> " + e.getValue() + " fois");
                    }
                });
    }

    private String projectStudentNames(List<Affectation> project) {
        List<String> names = new ArrayList<>();
        for (Affectation aff : project) {
            names.add(aff.getEtudiant().getNomE());
        }
        return String.join(" & ", names);
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

    private int slotOrder(String heure) {
        try {
            return Integer.parseInt(heure.replace("h", "").trim());
        } catch (NumberFormatException e) {
            return 99;
        }
    }

    @Override
    public Map<Long, String> getProfessorColors() {
        if (profColorMap.isEmpty()) {
            buildColorMap(profDao.findAll());
        }
        return profColorMap;
    }

    @Override
    public void deletePlanning() {
        resetPlanning();
    }

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
        private final int slot;
        private final Salle salle;
        private final Professeur rapporteur1;
        private final Professeur rapporteur2;

        private PlanningChoice(int dayIdx, int slot, Salle salle, Professeur rapporteur1, Professeur rapporteur2) {
            this.dayIdx = dayIdx;
            this.slot = slot;
            this.salle = salle;
            this.rapporteur1 = rapporteur1;
            this.rapporteur2 = rapporteur2;
        }
    }
}
