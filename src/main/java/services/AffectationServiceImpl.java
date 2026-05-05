package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.AffectationDAO;
import dao.EtudiantDAO;
import dao.ProfesseurDAO;
import dao.AffectationDAOImpl;
import dao.EtudiantDAOImpl;
import dao.ProfesseurDAOImpl;
import entities.Affectation;
import entities.Etudiant;
import entities.Professeur;

public class AffectationServiceImpl implements AffectationService {

    private EtudiantDAO etudiantDAO    = new EtudiantDAOImpl();
    private ProfesseurDAO professeurDAO = new ProfesseurDAOImpl();
    private AffectationDAO affectationDAO = new AffectationDAOImpl();

    @Override
    public List<Affectation> lancerAffectation(List<String> filieres, List<String> debug) {

        List<Etudiant> etudiants;

        if (filieres == null || filieres.isEmpty()) {
            etudiants = etudiantDAO.findAll();
            debug.add("Toutes les filières sélectionnées");
        } else {
            etudiants = etudiantDAO.findByFilieres(filieres);
            debug.add("Filières sélectionnées: " + filieres);
        }

        List<Professeur> profs = professeurDAO.findAll();

        if (etudiants.isEmpty() || profs.isEmpty()) {
            debug.add("❌ Données insuffisantes");
            return new ArrayList<>();
        }

        affectationDAO.deleteAll();

        Map<Long, Integer> compteur = new HashMap<>();
        for (Professeur p : profs) {
            compteur.put(p.getIdp(), 0);
        }

        List<Affectation> result = new ArrayList<>();

        for (Etudiant e : etudiants) {

            Professeur selected = null;
            int min = Integer.MAX_VALUE;

            for (Professeur p : profs) {
                int count = compteur.get(p.getIdp());
                if (count < min) {
                    min = count;
                    selected = p;
                }
            }

            Affectation a = new Affectation();
            a.setEtudiant(e);
            a.setEncadrant(selected);

            result.add(a);

            compteur.put(selected.getIdp(),
                    compteur.get(selected.getIdp()) + 1);

            debug.add("✔ [" + e.getFiliere() + "] "
                    + e.getNomE() + " → " + selected.getNom());
        }

        affectationDAO.saveAll(result);

        return result;
    }
}