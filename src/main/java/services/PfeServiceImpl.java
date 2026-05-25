package services;

import dao.AffectationDAO;
import dao.AffectationDAOImpl;
import dao.EtudiantDAO;
import dao.EtudiantDAOImpl;
import dao.FichierListeDAO;
import dao.FichierListeDAOImpl;
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
import entities.FichierListe;
import entities.Professeur;
import entities.Salle;
import entities.Soutenance;
import util.ExcelImporter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.Set;

public class PfeServiceImpl implements PfeService {

    private final AffectationDAO affDao;
    private final EtudiantDAO etuDao;
    private final ProfesseurDAO profDao;
    private final FichierListeDAO fichierDao;
    private final SalleDAO salleDao;
    private final JuryDAO juryDao;
    private final SoutenanceDAO soutDao;
    private final PlanningService planningService;
    private final VerificationService verificationService;
    private final RecommendationService recommendationService;

    public PfeServiceImpl() {
        this(new AffectationDAOImpl(), new EtudiantDAOImpl(), new ProfesseurDAOImpl(),
                new FichierListeDAOImpl(), new SalleDAOImpl(),
                ServiceFactory.createPlanningService(), ServiceFactory.createVerificationService(),
                new RecommendationServiceImpl());
    }

    public PfeServiceImpl(AffectationDAO affDao, EtudiantDAO etuDao, ProfesseurDAO profDao, FichierListeDAO fichierDao,
                          SalleDAO salleDao, PlanningService planningService, VerificationService verificationService,
                          RecommendationService recommendationService) {
        this(affDao, etuDao, profDao, fichierDao, salleDao, new JuryDAOImpl(), new SoutenanceDAOImpl(),
                planningService, verificationService, recommendationService);
    }

    public PfeServiceImpl(AffectationDAO affDao, EtudiantDAO etuDao, ProfesseurDAO profDao, FichierListeDAO fichierDao,
                          SalleDAO salleDao, JuryDAO juryDao, SoutenanceDAO soutDao,
                          PlanningService planningService, VerificationService verificationService,
                          RecommendationService recommendationService) {
        this.affDao = Objects.requireNonNull(affDao);
        this.etuDao = Objects.requireNonNull(etuDao);
        this.profDao = Objects.requireNonNull(profDao);
        this.fichierDao = Objects.requireNonNull(fichierDao);
        this.salleDao = Objects.requireNonNull(salleDao);
        this.juryDao = Objects.requireNonNull(juryDao);
        this.soutDao = Objects.requireNonNull(soutDao);
        this.planningService = Objects.requireNonNull(planningService);
        this.verificationService = Objects.requireNonNull(verificationService);
        this.recommendationService = Objects.requireNonNull(recommendationService);
    }

    // ─── Etudiants ──────────────────────────────────────────────────────────

    @Override
    public void saveEtudiants(List<Etudiant> etudiants, String filiere, String fileName) {
        normalizeBinomeSubjects(etudiants);

        List<Etudiant> existants = etuDao.findByFilieres(Collections.singletonList(filiere));
        List<Etudiant> aSauvegarder = new ArrayList<>();

        for (Etudiant nouveau : etudiants) {
            Etudiant etuExistant = null;
            for (Etudiant e : existants) {
                if (e.getCne() != null && nouveau.getCne() != null
                        && e.getCne().equalsIgnoreCase(nouveau.getCne())) {
                    etuExistant = e;
                    break;
                }
            }

            if (etuExistant != null) {
                etuExistant.setNomE(nouveau.getNomE());
                etuExistant.setPrenomE(nouveau.getPrenomE());
                etuExistant.setBinome_cne(nouveau.getBinome_cne());
                etuExistant.setSujet_stage(nouveau.getSujet_stage());
                aSauvegarder.add(etuExistant);
            } else {
                aSauvegarder.add(nouveau);
            }
        }

        etuDao.saveAll(aSauvegarder);

        fichierDao.deleteByFiliere(filiere);
        fichierDao.save(new FichierListe(fileName, filiere, etudiants.size()));
    }

    @Override
    public void deleteEtudiantsByFiliere(String filiere) {
        etuDao.deleteByFiliere(filiere);
        fichierDao.deleteByFiliere(filiere);
    }

    // ─── Professeurs ────────────────────────────────────────────────────────

    @Override
    public void saveProfesseurs(List<Professeur> profs) {
        List<Professeur> existants = profDao.findAll();
        List<Professeur> aSauvegarder = new ArrayList<>();

        for (Professeur nouveau : profs) {
            Professeur profExistant = null;
            for (Professeur p : existants) {
                if (safe(p.getNom()).equalsIgnoreCase(safe(nouveau.getNom()))
                        && safe(p.getPrenom()).equalsIgnoreCase(safe(nouveau.getPrenom()))) {
                    profExistant = p;
                    break;
                }
            }

            if (profExistant != null) {
                profExistant.setDiscipline(nouveau.getDiscipline());
                profExistant.setSpecialite(nouveau.getSpecialite());
                aSauvegarder.add(profExistant);
            } else {
                aSauvegarder.add(nouveau);
            }
        }

        profDao.saveAll(aSauvegarder);
    }

    @Override
    public void deleteAffectationsAndProfesseurs() {
        soutDao.deleteAll();
        affDao.deleteAll();
        juryDao.deleteAll();
        profDao.deleteAll();
    }

    // ─── Affectation ────────────────────────────────────────────────────────

    @Override
    public List<Affectation> getAllAffectationsWithDetails() {
        return affDao.findAllWithDetails();
    }

    @Override
    public void restoreAffectation(java.io.File backupFile) throws java.io.IOException {
        affDao.deleteAll();

        List<Etudiant> allEtu = etuDao.findAll();
        Map<Long, Etudiant> etuMap = new HashMap<>();
        for (Etudiant e : allEtu) etuMap.put(e.getIde(), e);

        List<Professeur> allProf = profDao.findAll();
        Map<Long, Professeur> profMap = new HashMap<>();
        for (Professeur p : allProf) profMap.put(p.getIdp(), p);

        List<Affectation> toSave = new ArrayList<>();
        List<String> lines = java.nio.file.Files.readAllLines(backupFile.toPath());
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length == 2) {
                try {
                    Long ide = Long.parseLong(parts[0]);
                    Long idp = Long.parseLong(parts[1]);
                    Etudiant etu = etuMap.get(ide);
                    Professeur prof = profMap.get(idp);
                    if (etu != null && prof != null) {
                        Affectation a = new Affectation();
                        a.setEtudiant(etu);
                        a.setEncadrant(prof);
                        toSave.add(a);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (!toSave.isEmpty()) {
            affDao.saveAll(toSave);
        }
    }

    @Override
    public void lancerAffectationGlobale(List<String> filieres, List<String> debugLog) {
        List<Etudiant> etudiants = etuDao.findByFilieres(filieres);
        List<Professeur> profs = profDao.findAll();
        // Excluded professors must not receive new affectations.
        profs.removeIf(Professeur::isExcluded);

        if (etudiants.isEmpty() || profs.isEmpty()) {
            debugLog.add("Impossible: Etudiants (" + etudiants.size() + ") ou Profs ("
                    + profs.size() + ") introuvables.");
            return;
        }

        soutDao.deleteByFilieres(filieres);
        affDao.deleteByFilieres(filieres);
        normalizeBinomeSubjects(etudiants);

        Map<String, List<List<Etudiant>>> projectsByFiliere = new LinkedHashMap<>();
        Map<String, Etudiant> etuByCne = new HashMap<>();
        for (Etudiant e : etudiants) etuByCne.put(e.getCne(), e);

        Set<String> processedCne = new HashSet<>();
        for (Etudiant e : etudiants) {
            if (processedCne.contains(e.getCne())) continue;
            List<Etudiant> project = new ArrayList<>();
            project.add(e);
            processedCne.add(e.getCne());

            if (e.hasBinome() && etuByCne.containsKey(e.getBinome_cne())) {
                Etudiant partner = etuByCne.get(e.getBinome_cne());
                if (!processedCne.contains(partner.getCne())) {
                    project.add(partner);
                    processedCne.add(partner.getCne());
                }
            }
            projectsByFiliere.computeIfAbsent(e.getFiliere(), k -> new ArrayList<>()).add(project);
        }

        Random rnd = new Random();
        for (List<List<Etudiant>> group : projectsByFiliere.values()) {
            Collections.shuffle(group, rnd);
            group.sort((p1, p2) -> Integer.compare(p2.size(), p1.size()));
        }

        List<List<Etudiant>> mixedProjects = new ArrayList<>();
        List<List<List<Etudiant>>> groups = new ArrayList<>(projectsByFiliere.values());
        boolean added = true;
        while (added) {
            added = false;
            for (List<List<Etudiant>> g : groups) {
                if (!g.isEmpty()) {
                    mixedProjects.add(g.remove(0));
                    added = true;
                }
            }
        }
        
        // Stable sort to ensure all binomes (size 2) are placed before solos (size 1)
        // while preserving the round-robin filiere mixing among projects of the same size.
        mixedProjects.sort((p1, p2) -> Integer.compare(p2.size(), p1.size()));

        List<Professeur> shuffledProfs = new ArrayList<>(profs);
        Collections.shuffle(shuffledProfs, rnd);

        // ── Load-balanced assignment using a min-heap ─────────────────────────
        // Always assign the next project to the professor with the lowest current
        // load. This guarantees the max difference between any two professors is
        // at most 1 project, eliminating the 5-vs-3 imbalance of round-robin.
        // Each entry: int[0] = current load, int[1] = stable index for tie-break.
        PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) ->
                a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        for (int i = 0; i < shuffledProfs.size(); i++) {
            heap.offer(new int[]{0, i});
        }

        List<Affectation> result = new ArrayList<>();
        for (List<Etudiant> project : mixedProjects) {
            int[] entry = heap.poll();
            Professeur assigned = shuffledProfs.get(entry[1]);
            for (Etudiant e : project) {
                Affectation a = new Affectation();
                a.setEtudiant(e);
                a.setEncadrant(assigned);
                result.add(a);
            }
            entry[0] += project.size();  // increment this professor's load by number of students
            heap.offer(entry);   // push back with updated load
        }

        affDao.saveAll(result);
        int totalProjects = mixedProjects.size();
        int avgLoad = totalProjects / shuffledProfs.size();
        int remainder = totalProjects % shuffledProfs.size();
        debugLog.add(result.size() + " affectations enregistrees | "
                + totalProjects + " projets / " + shuffledProfs.size() + " profs"
                + " → chaque prof encadre " + avgLoad
                + (remainder > 0 ? " ou " + (avgLoad + 1) : "")
                + " projet(s)");
    }

    // ─── Fichiers ───────────────────────────────────────────────────────────

    @Override
    public List<FichierListe> getAllFichiers() {
        return fichierDao.findAll();
    }

    // ─── Dashboard counters ─────────────────────────────────────────────────

    @Override
    public Map<String, Integer> getEtudiantsParProf(List<String> filieresFiltre) {
        List<Affectation> affectations = affDao.findAllWithDetails();
        Map<String, Integer> map = new HashMap<>();
        for (Affectation a : affectations) {
            if (filieresFiltre != null && !filieresFiltre.isEmpty()
                    && !filieresFiltre.contains(a.getEtudiant().getFiliere())) {
                continue;
            }
            String nom = a.getEncadrant().getNom() + " " + a.getEncadrant().getPrenom();
            map.put(nom, map.getOrDefault(nom, 0) + 1);
        }
        return map;
    }

    @Override
    public Map<String, Integer> getEtudiantsParFiliere(List<String> filieresFiltre) {
        List<Affectation> affectations = affDao.findAllWithDetails();
        Map<String, Integer> map = new HashMap<>();
        for (Affectation a : affectations) {
            String fil = a.getEtudiant().getFiliere();
            if (filieresFiltre != null && !filieresFiltre.isEmpty() && !filieresFiltre.contains(fil)) {
                continue;
            }
            map.put(fil, map.getOrDefault(fil, 0) + 1);
        }
        return map;
    }

    @Override
    public int getTotalEtudiantsAffectes(List<String> filieresFiltre) {
        if (filieresFiltre == null || filieresFiltre.isEmpty()) {
            return affDao.findAllWithDetails().size();
        }
        int count = 0;
        for (Affectation a : affDao.findAllWithDetails()) {
            if (filieresFiltre.contains(a.getEtudiant().getFiliere())) count++;
        }
        return count;
    }

    @Override
    public int getTotalProfesseursEncadrants(List<String> filieresFiltre) {
        return getEtudiantsParProf(filieresFiltre).size();
    }

    @Override
    public int getTotalProfesseurs() {
        return profDao.findAll().size();
    }

    @Override
    public int getTotalProjetsAffectes(List<String> filieresFiltre) {
        // Count distinct project groups (binomes count as 1)
        List<Affectation> affectations = affDao.findAllWithDetails();
        Set<String> seen = new HashSet<>();
        int projects = 0;
        Map<String, Etudiant> etuByCne = new HashMap<>();
        for (Affectation a : affectations) {
            if (a.getEtudiant() == null) continue;
            etuByCne.put(safe(a.getEtudiant().getCne()), a.getEtudiant());
        }
        for (Affectation a : affectations) {
            Etudiant e = a.getEtudiant();
            if (e == null) continue;
            if (filieresFiltre != null && !filieresFiltre.isEmpty()
                    && !filieresFiltre.contains(e.getFiliere())) continue;
            String cne = safe(e.getCne());
            if (cne.isEmpty()) cne = "IDE_" + e.getIde();
            if (seen.contains(cne)) continue;

            String binome = safe(e.getBinome_cne());
            if (!binome.isEmpty() && etuByCne.containsKey(binome)) {
                seen.add(binome);
            }
            seen.add(cne);
            projects++;
        }
        return projects;
    }

    @Override
    public Map<String, Integer> getSoutenancesParProf(List<String> filieresFiltre) {
        List<Soutenance> soutenances = planningService.getAllSoutenances();
        Map<String, Integer> map = new HashMap<>();
        for (Soutenance s : soutenances) {
            if (filieresFiltre != null && !filieresFiltre.isEmpty()
                    && !filieresFiltre.contains(s.getEtudiant().getFiliere())) {
                continue;
            }
            if (s.getJury() != null) {
                if (s.getJury().getPresident() != null) {
                    String nom = s.getJury().getPresident().getNom() + " " + s.getJury().getPresident().getPrenom();
                    map.put(nom, map.getOrDefault(nom, 0) + 1);
                }
                if (s.getJury().getRapporteur1() != null) {
                    String nom = s.getJury().getRapporteur1().getNom() + " " + s.getJury().getRapporteur1().getPrenom();
                    map.put(nom, map.getOrDefault(nom, 0) + 1);
                }
                if (s.getJury().getRapporteur2() != null) {
                    String nom = s.getJury().getRapporteur2().getNom() + " " + s.getJury().getRapporteur2().getPrenom();
                    map.put(nom, map.getOrDefault(nom, 0) + 1);
                }
            }
        }
        return map;
    }

    @Override
    public int getTotalSoutenances(List<String> filieresFiltre) {
        List<Soutenance> soutenances = planningService.getAllSoutenances();
        if (filieresFiltre == null || filieresFiltre.isEmpty()) {
            return soutenances.size();
        }
        int count = 0;
        for (Soutenance s : soutenances) {
            if (filieresFiltre.contains(s.getEtudiant().getFiliere())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public VerificationReport verifierFichiersGeneres(List<String> filieresFiltre) {
        return verificationService.verifyGeneratedFiles(filieresFiltre);
    }

    @Override
    public VerificationReport verifierFichiersGeneres(List<String> filieresFiltre, ConstraintSet constraints) {
        return verificationService.verifyGeneratedFiles(filieresFiltre, constraints);
    }

    @Override
    public Map<String, Object> searchDashboard(String query) {
        Map<String, Object> result = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            result.put("searchType", "NONE");
            return result;
        }
        query = query.toLowerCase().trim();

        List<Affectation> allAff = affDao.findAllWithDetails();
        List<Soutenance> allSout = planningService.getAllSoutenances();

        for (Affectation a : allAff) {
            Etudiant e = a.getEtudiant();
            if (e != null) {
                String nom = e.getNomE() != null ? e.getNomE().toLowerCase() : "";
                String prenom = e.getPrenomE() != null ? e.getPrenomE().toLowerCase() : "";
                String fullName1 = nom + " " + prenom;
                String fullName2 = prenom + " " + nom;
                if (fullName1.contains(query) || fullName2.contains(query)
                        || nom.contains(query) || prenom.contains(query)) {
                    result.put("searchType", "ETUDIANT");
                    result.put("etu", e);
                    result.put("affectation", a);
                    for (Soutenance s : allSout) {
                        if (s.getEtudiant() != null && s.getEtudiant().getIde().equals(e.getIde())) {
                            result.put("soutenance", s);
                            break;
                        }
                    }
                    return result;
                }
            }
        }

        for (Professeur p : profDao.findAll()) {
            String nom = p.getNom() != null ? p.getNom().toLowerCase() : "";
            String prenom = p.getPrenom() != null ? p.getPrenom().toLowerCase() : "";
            String fullName1 = nom + " " + prenom;
            String fullName2 = prenom + " " + nom;

            if (fullName1.contains(query) || fullName2.contains(query)
                    || nom.contains(query) || prenom.contains(query)) {

                result.put("searchType", "PROFESSEUR");
                result.put("prof", p);

                List<Affectation> encadrements = new ArrayList<>();
                for (Affectation a : allAff) {
                    if (a.getEncadrant() != null && a.getEncadrant().getIdp().equals(p.getIdp())) {
                        encadrements.add(a);
                    }
                }
                result.put("encadrements", encadrements);

                List<Soutenance> soutenances = new ArrayList<>();
                for (Soutenance s : allSout) {
                    if (s.getEtudiant() == null) continue;
                    boolean isEnc = false;
                    for (Affectation a : allAff) {
                        if (a.getEtudiant().getIde().equals(s.getEtudiant().getIde())
                                && a.getEncadrant() != null
                                && a.getEncadrant().getIdp().equals(p.getIdp())) {
                            isEnc = true;
                            break;
                        }
                    }
                    boolean isJury = false;
                    if (s.getJury() != null) {
                        isJury = (s.getJury().getPresident() != null
                                    && s.getJury().getPresident().getIdp().equals(p.getIdp()))
                                || (s.getJury().getRapporteur1() != null
                                    && s.getJury().getRapporteur1().getIdp().equals(p.getIdp()))
                                || (s.getJury().getRapporteur2() != null
                                    && s.getJury().getRapporteur2().getIdp().equals(p.getIdp()));
                    }
                    if (isEnc || isJury) {
                        soutenances.add(s);
                    }
                }
                result.put("soutenances", soutenances);
                return result;
            }
        }

        result.put("searchType", "NONE");
        return result;
    }

    // ─── Planning delegation ────────────────────────────────────────────────

    @Override
    public PlanningResult genererPlanning(List<String> filieres, List<Long> selectedSalles, PlanningConfig config) {
        return planningService.genererPlanning(filieres, selectedSalles,
                config == null ? PlanningConfig.defaults() : config);
    }

    @Override
    public List<Soutenance> getAllSoutenances() {
        return planningService.getAllSoutenances();
    }

    @Override
    public Map<Long, String> getProfessorColors() {
        return planningService.getProfessorColors();
    }

    @Override
    public void deletePlanning() {
        planningService.deletePlanning();
    }

    // ─── Salles management ─────────────────────────────────────────────────

    @Override
    public List<Salle> getAllSalles() {
        return salleDao.findAll();
    }

    @Override
    public boolean addSalle(String numSalle) {
        if (numSalle == null) return false;
        String trimmed = numSalle.trim();
        String normalized = normalizeSalleName(trimmed);
        if (normalized.isEmpty()) return false;

        for (Salle existingSalle : salleDao.findAll()) {
            if (normalizeSalleName(existingSalle.getNum_salle()).equals(normalized)) {
                return false;
            }
        }

        Salle s = new Salle();
        s.setNum_salle(trimmed);
        s.setBlock("");
        s.setStatus("Libre");
        salleDao.save(s);
        return true;
    }

    @Override
    public int addSalleBulk(String multilineNames) {
        if (multilineNames == null || multilineNames.trim().isEmpty()) return 0;
        int added = 0;
        for (String line : multilineNames.split("\\r?\\n|;|,")) {
            if (addSalle(line)) added++;
        }
        return added;
    }

    @Override
    public int saveSallesIfMissing(List<Salle> imported) {
        if (imported == null || imported.isEmpty()) return 0;
        int added = 0;
        Set<String> existingKeys = new HashSet<>();
        for (Salle existing : salleDao.findAll()) {
            existingKeys.add(normalizeSalleName(existing.getNum_salle()));
        }
        List<Salle> toSave = new ArrayList<>();
        for (Salle s : imported) {
            String key = normalizeSalleName(s.getNum_salle());
            if (key.isEmpty() || !existingKeys.add(key)) continue;
            if (s.getBlock() == null) s.setBlock("");
            if (s.getStatus() == null || s.getStatus().isEmpty()) s.setStatus("Libre");
            toSave.add(s);
        }
        if (!toSave.isEmpty()) {
            salleDao.saveAll(toSave);
            added = toSave.size();
        }
        return added;
    }

    @Override
    public boolean deleteSalle(Long id) {
        return salleDao.deleteById(id);
    }

    @Override
    public boolean isSalleUsedInPlanning(Long id) {
        return salleDao.isUsedInPlanning(id);
    }

    @Override
    public int deleteAllSalles() {
        List<Salle> all = salleDao.findAll();
        int removed = 0;
        for (Salle s : all) {
            if (salleDao.deleteById(s.getId_salle())) removed++;
        }
        return removed;
    }

    // ─── Workbook import ─────────────────────────────────────────────────────

    @Override
    public ExcelImporter.ImportResult importWorkbook(InputStream is, String fileName) {
        ExcelImporter.ImportResult result = ExcelImporter.importWorkbook(is);
        // Persist students per filiere
        for (Map.Entry<String, List<Etudiant>> e : result.getStudentsByFiliere().entrySet()) {
            saveEtudiants(e.getValue(), e.getKey(), fileName == null ? "import.xlsx" : fileName);
        }
        // Persist professors
        if (!result.getProfesseurs().isEmpty()) {
            saveProfesseurs(result.getProfesseurs());
        }
        // Persist any new salles
        if (!result.getSalles().isEmpty()) {
            saveSallesIfMissing(result.getSalles());
        }
        return result;
    }

    // ─── Recommendations ─────────────────────────────────────────────────────

    @Override
    public List<Recommendation> generateRecommendations(List<String> filieres, int numberOfRooms,
                                                        PlanningConfig config) {
        PlanningConfig cfg = config == null ? PlanningConfig.defaults() : config;
        int totalProjects = getTotalProjetsAffectes(filieres);
        int slotsPerDay = cfg.getSlotsPerDay();
        int numberOfDays = cfg.getNumberOfDays();
        int numberOfProfs = getTotalProfesseurs();
        return recommendationService.generateRecommendations(totalProjects, numberOfRooms, slotsPerDay,
                numberOfDays, numberOfProfs, cfg);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String normalizeSalleName(String numSalle) {
        if (numSalle == null) return "";
        // Generic normalization: collapse whitespace and uppercase. We don't
        // make any assumption about the establishment's room naming scheme
        // (e.g. blocks, building codes...) since those vary per institution.
        return numSalle.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private void normalizeBinomeSubjects(List<Etudiant> etudiants) {
        if (etudiants == null || etudiants.isEmpty()) return;

        Map<String, Etudiant> etuByCne = new HashMap<>();
        for (Etudiant e : etudiants) {
            String cne = safe(e.getCne());
            if (!cne.isEmpty()) etuByCne.put(cne, e);
        }

        Set<String> processedPairs = new HashSet<>();
        for (Etudiant e : etudiants) {
            String cne = safe(e.getCne());
            String binomeCne = safe(e.getBinome_cne());
            if (cne.isEmpty() || binomeCne.isEmpty()) continue;
            Etudiant partner = etuByCne.get(binomeCne);
            if (partner == null) continue;

            String pairKey = cne.compareTo(binomeCne) <= 0 ? cne + "|" + binomeCne : binomeCne + "|" + cne;
            if (!processedPairs.add(pairKey)) continue;

            String commonSubject = chooseProjectSubject(e, partner);
            if (!commonSubject.isEmpty()) {
                e.setSujet_stage(commonSubject);
                partner.setSujet_stage(commonSubject);
            }
        }
    }

    private String chooseProjectSubject(Etudiant first, Etudiant second) {
        String firstSubject = safe(first.getSujet_stage());
        String secondSubject = safe(second.getSujet_stage());
        if (!firstSubject.isEmpty() && !isDefaultSubject(firstSubject)) return firstSubject;
        if (!secondSubject.isEmpty() && !isDefaultSubject(secondSubject)) return secondSubject;
        if (!firstSubject.isEmpty()) return firstSubject;
        return secondSubject;
    }

    private boolean isDefaultSubject(String subject) {
        return subject.toLowerCase(Locale.ROOT).contains("projet de fin d");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
