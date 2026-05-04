package controller;

import dao.EtudiantDAO;
import dao.EtudiantDAOImpl;
import dao.FichierListeDAO;
import dao.FichierListeDAOImpl;
import entities.Etudiant;
import entities.FichierListe;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import util.ExcelImporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@WebServlet("/uploadEtudiants")
@MultipartConfig(maxFileSize = 10485760) // 10MB
public class UploadEtudiantsServlet extends HttpServlet {

    private EtudiantDAO etuDao = new EtudiantDAOImpl();
    private FichierListeDAO fichierDao = new FichierListeDAOImpl();

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Collection<Part> parts = req.getParts();
        List<String> debug = new ArrayList<>();

        for (Part part : parts) {
            if (!part.getName().equals("files") || part.getSize() == 0) continue;

            String fileName = part.getSubmittedFileName();
            String filiere  = extractFiliere(fileName);
            debug.add("📂 Fichier reçu: " + fileName + " → Filière: " + filiere);

            try {
                List<Etudiant> list = ExcelImporter.importEtudiants(part.getInputStream(), filiere);
                debug.add(list.size() + " étudiants lus");

                if (list.isEmpty()) {
                    debug.add("Aucun étudiant trouvé dans " + fileName);
                    continue;
                }

                // Supprimer les anciens étudiants de cette filière uniquement
                etuDao.deleteByFiliere(filiere);

                // Insérer les nouveaux
                etuDao.saveAll(list);
                debug.add( list.size() + " étudiants [" + filiere + "] sauvegardés");

                // Mettre à jour le registre des fichiers uploadés
                fichierDao.deleteByFiliere(filiere);
                fichierDao.save(new FichierListe(fileName, filiere, list.size()));

            } catch (Exception e) {
                debug.add("Erreur: " + fileName + " → " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Recharger la liste des fichiers disponibles
        req.setAttribute("fichiers", fichierDao.findAll());
        req.setAttribute("debug", debug);
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }

    private String extractFiliere(String fileName) {
        if (fileName == null) return "INCONNUE";
        String base = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf(".")).trim()
                : fileName.trim();
        String lower = base.toLowerCase();
        if (lower.equals("gi") || lower.contains("génie info") || lower.contains("genie info")) return "GI";
        if (lower.equals("id") || lower.contains("ingénierie") || lower.contains("ingenierie")) return "ID";
        if (lower.contains("tdia") || lower.contains("intelligence artificielle") || lower.contains("transformation digitale")) return "TDIA";
        if (lower.equals("gc") || lower.contains("génie civil") || lower.contains("genie civil")) return "GC";
        // Retourner le nom de fichier en majuscules comme filière
        return base.toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }
}