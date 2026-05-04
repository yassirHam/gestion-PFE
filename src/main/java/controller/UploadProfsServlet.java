package controller;

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
import java.util.List;

import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import entities.Professeur;

@WebServlet("/uploadProfs")
@MultipartConfig
public class UploadProfsServlet extends HttpServlet {

    private ProfesseurDAO dao = new ProfesseurDAOImpl();

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<String> debug = new ArrayList<>();

        Part file = req.getPart("files");

        if (file == null || file.getSize() == 0) {
            debug.add("Aucun fichier reçu");
            req.setAttribute("debug", debug);
            req.getRequestDispatcher("affectation.jsp").forward(req, resp);
            return;
        }

        debug.add("Lecture fichier: " + file.getSubmittedFileName());

        try {
            List<Professeur> list = ExcelImporter.importProfs(file.getInputStream());
            debug.add(list.size() + " professeurs importés");

            for (Professeur p : list) {
                debug.add("Prof: " + p.getNom() + " " + p.getPrenom() + " | " + p.getDiscipline());
            }

            dao.deleteAll();
            dao.saveAll(list);
            debug.add("Sauvegarde en base terminée");

        } catch (Exception e) {
            debug.add("Erreur: " + e.getMessage());
            e.printStackTrace();
        }

        req.setAttribute("debug", debug);
        req.setAttribute("message", "Profs importés ");
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }
}