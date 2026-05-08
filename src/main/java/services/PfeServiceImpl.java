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
import entities.Soutenance;

import java.util.*;

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

    public PfeServiceImpl() {
        this(new AffectationDAOImpl(), new EtudiantDAOImpl(), new ProfesseurDAOImpl(), new FichierListeDAOImpl(),
                new SalleDAOImpl(), ServiceFactory.createPlanningService(), ServiceFactory.createVerificationService());
    }

    public PfeServiceImpl(AffectationDAO affDao, EtudiantDAO etuDao, ProfesseurDAO profDao, FichierListeDAO fichierDao,
                          SalleDAO salleDao, PlanningService planningService, VerificationService verificationService) {
        this(affDao, etuDao, profDao, fichierDao, salleDao, new JuryDAOImpl(), new SoutenanceDAOImpl(),
                planningService, verificationService);
    }

    public PfeServiceImpl(AffectationDAO affDao, EtudiantDAO etuDao, ProfesseurDAO profDao, FichierListeDAO fichierDao,
                          SalleDAO salleDao, JuryDAO juryDao, SoutenanceDAO soutDao,
                          PlanningService planningService, VerificationService verificationService) {
        this.affDao = Objects.requireNonNull(affDao);
        this.etuDao = Objects.requireNonNull(etuDao);
        this.profDao = Objects.requireNonNull(profDao);
        this.fichierDao = Objects.requireNonNull(fichierDao);
        this.salleDao = Objects.requireNonNull(salleDao);
        this.juryDao = Objects.requireNonNull(juryDao);
        this.soutDao = Objects.requireNonNull(soutDao);
        this.planningService = Objects.requireNonNull(planningService);
        this.verificationService = Objects.requireNonNull(verificationService);
    }

    @Override
    public void saveEtudiants(List<Etudiant> etudiants, String filiere, String fileName) {
        normalizeBinomeSubjects(etudiants);
        etuDao.deleteByFiliere(filiere);
        etuDao.saveAll(etudiants);
        fichierDao.deleteByFiliere(filiere);
        fichierDao.save(new FichierListe(fileName, filiere, etudiants.size()));
    }

    @Override
    public void deleteEtudiantsByFiliere(String filiere) {
        etuDao.deleteByFiliere(filiere);
        fichierDao.deleteByFiliere(filiere);
    }

    @Override
    public void saveProfesseurs(List<Professeur> profs) {
        profDao.saveAll(profs);
    }

    @Override
    public void deleteAffectationsAndProfesseurs() {
        soutDao.deleteAll();
        affDao.deleteAll();
        juryDao.deleteAll();
        profDao.deleteAll();
    }

    @Override
    public List<Affectation> getAllAffectationsWithDetails() {
        return affDao.findAllWithDetails();
    }

    @Override
    public void restoreAffectation(java.io.File backupFile) throws java.io.IOException {
        affDao.deleteAll();
        
        java.util.List<Etudiant> allEtu = etuDao.findAll();
        java.util.Map<Long, Etudiant> etuMap = new java.util.HashMap<>();
        for (Etudiant e : allEtu) etuMap.put(e.getIde(), e);
        
        java.util.List<Professeur> allProf = profDao.findAll();
        java.util.Map<Long, Professeur> profMap = new java.util.HashMap<>();
        for (Professeur p : allProf) profMap.put(p.getIdp(), p);
        
        java.util.List<Affectation> toSave = new java.util.ArrayList<>();
        
        java.util.List<String> lines = java.nio.file.Files.readAllLines(backupFile.toPath());
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
                } catch (Exception e) {
                    // Ignore corrupted line
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

        if (etudiants.isEmpty() || profs.isEmpty()) {
            debugLog.add("Impossible: Etudiants (" + etudiants.size() + ") ou Profs (" + profs.size() + ") introuvables.");
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

        List<Professeur> shuffledProfs = new ArrayList<>(profs);
        Collections.shuffle(shuffledProfs, rnd);

        List<Affectation> result = new ArrayList<>();
        for (int i = 0; i < mixedProjects.size(); i++) {
            Professeur assigned = shuffledProfs.get(i % shuffledProfs.size());
            for (Etudiant e : mixedProjects.get(i)) {
                Affectation a = new Affectation();
                a.setEtudiant(e);
                a.setEncadrant(assigned);
                result.add(a);
            }
        }

        affDao.saveAll(result);
        debugLog.add(result.size() + " affectations enregistrées");
    }

    @Override
    public List<FichierListe> getAllFichiers() {
        return fichierDao.findAll();
    }

    @Override
    public Map<String, Integer> getEtudiantsParProf(List<String> filieresFiltre) {
        List<Affectation> affectations = affDao.findAllWithDetails();
        Map<String, Integer> map = new HashMap<>();
        for (Affectation a : affectations) {
            if (filieresFiltre != null && !filieresFiltre.isEmpty() && !filieresFiltre.contains(a.getEtudiant().getFiliere())) {
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
    public Map<String, Integer> getSoutenancesParProf(List<String> filieresFiltre) {
        List<Soutenance> soutenances = planningService.getAllSoutenances();
        Map<String, Integer> map = new HashMap<>();
        
        for (Soutenance s : soutenances) {
            if (filieresFiltre != null && !filieresFiltre.isEmpty() && !filieresFiltre.contains(s.getEtudiant().getFiliere())) {
                continue;
            }
            if (s.getJury() != null) {
                // Add president
                if (s.getJury().getPresident() != null) {
                    String nom = s.getJury().getPresident().getNom() + " " + s.getJury().getPresident().getPrenom();
                    map.put(nom, map.getOrDefault(nom, 0) + 1);
                }
                // Add rapporteur 1
                if (s.getJury().getRapporteur1() != null) {
                    String nom = s.getJury().getRapporteur1().getNom() + " " + s.getJury().getRapporteur1().getPrenom();
                    map.put(nom, map.getOrDefault(nom, 0) + 1);
                }
                // Add rapporteur 2
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
    public java.util.Map<String, Object> searchDashboard(String query) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
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
                
                if (fullName1.contains(query) || fullName2.contains(query) || nom.contains(query) || prenom.contains(query)) {
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
            
            if (fullName1.contains(query) || fullName2.contains(query) || nom.contains(query) || prenom.contains(query)) {
                
                result.put("searchType", "PROFESSEUR");
                result.put("prof", p);
                
                List<Affectation> encadrements = new java.util.ArrayList<>();
                for (Affectation a : allAff) {
                    if (a.getEncadrant() != null && a.getEncadrant().getIdp().equals(p.getIdp())) {
                        encadrements.add(a);
                    }
                }
                result.put("encadrements", encadrements);
                
                List<Soutenance> soutenances = new java.util.ArrayList<>();
                for (Soutenance s : allSout) {
                    if (s.getEtudiant() == null) continue;
                    
                    boolean isEnc = false;
                    for(Affectation a : allAff) {
                        if (a.getEtudiant().getIde().equals(s.getEtudiant().getIde()) && 
                            a.getEncadrant() != null && a.getEncadrant().getIdp().equals(p.getIdp())) {
                            isEnc = true;
                            break;
                        }
                    }

                    boolean isJury = false;
                    if (s.getJury() != null) {
                        isJury = (s.getJury().getPresident() != null && s.getJury().getPresident().getIdp().equals(p.getIdp())) ||
                                 (s.getJury().getRapporteur1() != null && s.getJury().getRapporteur1().getIdp().equals(p.getIdp())) ||
                                 (s.getJury().getRapporteur2() != null && s.getJury().getRapporteur2().getIdp().equals(p.getIdp()));
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

    // ── Planning delegation ──────────────────────────────────────────────────

    @Override
    public List<Soutenance> genererPlanning(List<String> filieres, List<String> debugLog, List<Long> selectedSalles, String startDate) {
        return planningService.genererPlanning(filieres, debugLog, selectedSalles, startDate);
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

    @Override
    public List<entities.Salle> getAllSalles() {
        return salleDao.findAll();
    }

    @Override
    public boolean addSalle(String numSalle) {
        String normalizedNumSalle = normalizeSalleName(numSalle);
        if (normalizedNumSalle.isEmpty()) {
            return false;
        }

        for (entities.Salle existingSalle : salleDao.findAll()) {
            if (normalizeSalleName(existingSalle.getNum_salle()).equals(normalizedNumSalle)) {
                return false;
            }
        }

        entities.Salle s = new entities.Salle();
        s.setNum_salle(numSalle.trim());
        s.setBlock("Bloc Principal");
        s.setStatus("Libre");
        salleDao.save(s);
        return true;
    }

    private String normalizeSalleName(String numSalle) {
        if (numSalle == null) {
            return "";
        }
        String normalized = numSalle.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        normalized = normalized.replaceFirst("^SALLE\\s*", "S");
        normalized = normalized.replaceFirst("^S\\s+(\\d)", "S$1");
        normalized = normalized.replaceFirst("^ANCIEN\\s+BLOC\\s*", "AB");
        normalized = normalized.replaceFirst("^NOUVEAU\\s+BLOC\\s*", "NB");
        return normalized;
    }

    private void normalizeBinomeSubjects(List<Etudiant> etudiants) {
        if (etudiants == null || etudiants.isEmpty()) {
            return;
        }

        Map<String, Etudiant> etuByCne = new HashMap<>();
        for (Etudiant e : etudiants) {
            String cne = safe(e.getCne());
            if (!cne.isEmpty()) {
                etuByCne.put(cne, e);
            }
        }

        Set<String> processedPairs = new HashSet<>();
        for (Etudiant e : etudiants) {
            String cne = safe(e.getCne());
            String binomeCne = safe(e.getBinome_cne());
            if (cne.isEmpty() || binomeCne.isEmpty()) {
                continue;
            }

            Etudiant partner = etuByCne.get(binomeCne);
            if (partner == null) {
                continue;
            }

            String pairKey = cne.compareTo(binomeCne) <= 0 ? cne + "|" + binomeCne : binomeCne + "|" + cne;
            if (!processedPairs.add(pairKey)) {
                continue;
            }

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
        if (!firstSubject.isEmpty() && !isDefaultSubject(firstSubject)) {
            return firstSubject;
        }
        if (!secondSubject.isEmpty() && !isDefaultSubject(secondSubject)) {
            return secondSubject;
        }
        if (!firstSubject.isEmpty()) {
            return firstSubject;
        }
        return secondSubject;
    }

    private boolean isDefaultSubject(String subject) {
        return subject.toLowerCase(Locale.ROOT).contains("projet de fin d");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
