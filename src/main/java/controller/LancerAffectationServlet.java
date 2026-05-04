package controller;

import dao.*;
import entities.Affectation;
import entities.Etudiant;
import entities.Professeur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

@WebServlet("/lancerAffectation")
public class LancerAffectationServlet extends HttpServlet {

    private EtudiantDAO    etuDao  = new EtudiantDAOImpl();
    private ProfesseurDAO  profDao = new ProfesseurDAOImpl();
    private AffectationDAO affDao  = new AffectationDAOImpl();
    private FichierListeDAO fichierDao = new FichierListeDAOImpl();

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String[] selected = req.getParameterValues("selectedFilieres");
        List<String> debug = new ArrayList<>();

        if (selected == null || selected.length == 0) {
            debug.add("⚠️ Sélectionnez au moins une filière");
            req.setAttribute("fichiers", fichierDao.findAll());
            req.setAttribute("debug", debug);
            req.getRequestDispatcher("affectation.jsp").forward(req, resp);
            return;
        }

        List<String> filieres = Arrays.asList(selected);
        debug.add("Filières sélectionnées: " + String.join(", ", filieres));

        List<Etudiant>    etudiants = etuDao.findByFilieres(filieres);
        List<Professeur>  profs     = profDao.findAll();

        debug.add("Étudiants trouvés: " + etudiants.size());
        debug.add("Professeurs disponibles: " + profs.size());

        if (etudiants.isEmpty()) {
            debug.add("❌ Aucun étudiant pour les filières sélectionnées");
            req.setAttribute("fichiers", fichierDao.findAll());
            req.setAttribute("debug", debug);
            req.getRequestDispatcher("affectation.jsp").forward(req, resp);
            return;
        }

        if (profs.isEmpty()) {
            debug.add("Aucun professeur en base — uploadez d'abord les professeurs");
            req.setAttribute("fichiers", fichierDao.findAll());
            req.setAttribute("debug", debug);
            req.getRequestDispatcher("affectation.jsp").forward(req, resp);
            return;
        }

        // ── Supprimer les anciennes affectations pour ces filières uniquement ──
        // (on garde les affectations des autres filières)
        try (org.hibernate.Session session = util.HibernateUtil.getSessionFactory().openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();
            session.createMutationQuery(
                "delete from Affectation a where a.etudiant.filiere in (:filieres)")
                .setParameterList("filieres", filieres)
                .executeUpdate();
            tx.commit();
        }

        // ── Algorithme d'affectation équilibrée ──
        // Trier les étudiants par filière pour grouper les affectations
        etudiants.sort(Comparator.comparing(Etudiant::getFiliere)
                                 .thenComparing(Etudiant::getNomE));

        Map<Long, Integer> compteur = new LinkedHashMap<>();
        for (Professeur p : profs) compteur.put(p.getIdp(), 0);

        List<Affectation> result = new ArrayList<>();

        for (Etudiant e : etudiants) {
            // Trouver le prof avec le moins d'étudiants
            Professeur selected2 = null;
            int min = Integer.MAX_VALUE;
            for (Professeur p : profs) {
                int count = compteur.get(p.getIdp());
                if (count < min) { min = count; selected2 = p; }
            }
            Affectation a = new Affectation();
            a.setEtudiant(e);
            a.setEncadrant(selected2);
            result.add(a);
            compteur.put(selected2.getIdp(), compteur.get(selected2.getIdp()) + 1);
        }

        affDao.saveAll(result);
        debug.add(result.size() + " affectations enregistrées");

        // ── Construire la vue groupée par encadrant ──
        Map<Professeur, List<Affectation>> grouped = new LinkedHashMap<>();
        List<Affectation> allAffectations = affDao.findAllWithDetails();

        for (Affectation a : allAffectations) {
            grouped.computeIfAbsent(a.getEncadrant(), k -> new ArrayList<>()).add(a);
        }

        req.setAttribute("grouped", grouped);
        req.setAttribute("fichiers", fichierDao.findAll());
        req.setAttribute("debug", debug);
        req.setAttribute("totalAffectations", allAffectations.size());
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }
}