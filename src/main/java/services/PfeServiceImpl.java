package services;

import dao.AffectationDAO;
import dao.AffectationDAOImpl;
import dao.EtudiantDAO;
import dao.EtudiantDAOImpl;
import dao.FichierListeDAO;
import dao.FichierListeDAOImpl;
import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import entities.Affectation;
import entities.Etudiant;
import entities.FichierListe;
import entities.Professeur;
import entities.Soutenance;

import java.util.*;

public class PfeServiceImpl implements PfeService {

    private final AffectationDAO affDao = new AffectationDAOImpl();
    private final EtudiantDAO etuDao = new EtudiantDAOImpl();
    private final ProfesseurDAO profDao = new ProfesseurDAOImpl();
    private final FichierListeDAO fichierDao = new FichierListeDAOImpl();
    private final PlanningService planningService = new PlanningServiceImpl();

    @Override
    public void saveEtudiants(List<Etudiant> etudiants, String filiere, String fileName) {
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
    public List<Etudiant> getEtudiantsByFilieres(List<String> filieres) {
        return etuDao.findByFilieres(filieres);
    }

    @Override
    public void saveProfesseurs(List<Professeur> profs) {
        profDao.saveAll(profs);
    }

    @Override
    public List<Professeur> getAllProfesseurs() {
        return profDao.findAll();
    }

    @Override
    public void deleteAffectationsAndProfesseurs() {
        affDao.deleteAll();
        profDao.deleteAll();
    }

    @Override
    public List<Affectation> getAllAffectationsWithDetails() {
        return affDao.findAllWithDetails();
    }

    @Override
    public void deleteAllAffectations() {
        affDao.deleteAll();
    }

    @Override
    public void lancerAffectationGlobale(List<String> filieres, List<String> debugLog) {
        List<Etudiant> etudiants = etuDao.findByFilieres(filieres);
        List<Professeur> profs = profDao.findAll();

        if (etudiants.isEmpty() || profs.isEmpty()) {
            debugLog.add("Impossible: Etudiants (" + etudiants.size() + ") ou Profs (" + profs.size() + ") introuvables.");
            return;
        }

        try (org.hibernate.Session session = util.HibernateUtil.getSessionFactory().openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();
            session.createMutationQuery("delete from Affectation a where a.etudiant.filiere in (:filieres)")
                   .setParameterList("filieres", filieres)
                   .executeUpdate();
            tx.commit();
        }

        // 1. Grouper les étudiants par filière
        Map<String, List<Etudiant>> byFiliere = new LinkedHashMap<>();
        for (Etudiant e : etudiants) {
            byFiliere.computeIfAbsent(e.getFiliere(), k -> new ArrayList<>()).add(e);
        }

        // 2. Mélanger chaque groupe filière séparément
        Random rnd = new Random();
        for (List<Etudiant> group : byFiliere.values()) {
            Collections.shuffle(group, rnd);
        }

        // 3. Interleaver : 1 de chaque filière en rotation → liste mixte
        List<Etudiant> mixed = new ArrayList<>();
        List<List<Etudiant>> groups = new ArrayList<>(byFiliere.values());
        boolean added = true;
        while (added) {
            added = false;
            for (List<Etudiant> g : groups) {
                if (!g.isEmpty()) {
                    mixed.add(g.remove(0));
                    added = true;
                }
            }
        }

        // 4. Mélanger les profs et distribuer en round-robin
        List<Professeur> shuffledProfs = new ArrayList<>(profs);
        Collections.shuffle(shuffledProfs, rnd);

        List<Affectation> result = new ArrayList<>();
        for (int i = 0; i < mixed.size(); i++) {
            Professeur assigned = shuffledProfs.get(i % shuffledProfs.size());
            Affectation a = new Affectation();
            a.setEtudiant(mixed.get(i));
            a.setEncadrant(assigned);
            result.add(a);
        }

        affDao.saveAll(result);
        debugLog.add(result.size() + " affectations enregistrées");
    }

    @Override
    public List<FichierListe> getAllFichiers() {
        return fichierDao.findAll();
    }

    @Override
    public void deleteFichierByFiliere(String filiere) {
        fichierDao.deleteByFiliere(filiere);
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

    // ── Planning delegation ──────────────────────────────────────────────────

    @Override
    public List<Soutenance> genererPlanning(List<String> debugLog) {
        return planningService.genererPlanning(debugLog);
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
}
