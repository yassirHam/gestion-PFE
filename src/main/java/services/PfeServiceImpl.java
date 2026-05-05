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
        // Must delete planning data first to avoid FK constraint violations and stale data
        try (org.hibernate.Session session = util.HibernateUtil.getSessionFactory().openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();
            session.createMutationQuery("delete from Soutenance s where s.etudiant.filiere = :filiere")
                   .setParameter("filiere", filiere)
                   .executeUpdate();
            session.createMutationQuery("delete from Affectation a where a.etudiant.filiere = :filiere")
                   .setParameter("filiere", filiere)
                   .executeUpdate();
            tx.commit();
        }
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
        // Delete soutenances first to avoid FK violations and stale data
        try (org.hibernate.Session session = util.HibernateUtil.getSessionFactory().openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();
            session.createMutationQuery("delete from Soutenance").executeUpdate();
            session.createMutationQuery("delete from Affectation").executeUpdate();
            tx.commit();
        }
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
            // First, delete related Soutenances to prevent stale planning data
            session.createMutationQuery("delete from Soutenance s where s.etudiant.filiere in (:filieres)")
                   .setParameterList("filieres", filieres)
                   .executeUpdate();
                   
            // Then delete Affectations
            session.createMutationQuery("delete from Affectation a where a.etudiant.filiere in (:filieres)")
                   .setParameterList("filieres", filieres)
                   .executeUpdate();
            tx.commit();
        }

        // 1. Grouper les étudiants par filière en PROJETS (1 ou 2 étudiants)
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

        // 2. Mélanger chaque groupe filière séparément
        Random rnd = new Random();
        for (List<List<Etudiant>> group : projectsByFiliere.values()) {
            Collections.shuffle(group, rnd);
        }

        // 3. Interleaver : 1 projet de chaque filière en rotation → liste mixte
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

        // 4. Mélanger les profs et distribuer en round-robin
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
    public List<Soutenance> genererPlanning(List<String> filieres, List<String> debugLog, List<Long> selectedSalles) {
        return planningService.genererPlanning(filieres, debugLog, selectedSalles);
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
        dao.SalleDAO salleDao = new dao.SalleDAOImpl();
        return salleDao.findAll();
    }

    @Override
    public void addSalle(String numSalle) {
        dao.SalleDAO salleDao = new dao.SalleDAOImpl();
        entities.Salle s = new entities.Salle();
        s.setNum_salle(numSalle);
        s.setBlock("Bloc Principal");
        s.setStatus("Libre");
        salleDao.save(s);
    }
}
