package controller;

import dao.EtudiantDAO;
import dao.EtudiantDAOImpl;
import dao.FichierListeDAO;
import dao.FichierListeDAOImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/supprimerListes")
public class SupprimerListeServlet extends HttpServlet {

    private EtudiantDAO etuDao = new EtudiantDAOImpl();
    private FichierListeDAO fichierDao = new FichierListeDAOImpl();

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String[] selected = req.getParameterValues("selectedFilieres");
        List<String> debug = new ArrayList<>();

        if (selected == null || selected.length == 0) {
            debug.add("Aucune filière sélectionnée");
        } else {
            for (String filiere : selected) {
                etuDao.deleteByFiliere(filiere);
                fichierDao.deleteByFiliere(filiere);
                debug.add("Liste [" + filiere + "] supprimée");
            }
        }

        req.setAttribute("fichiers", fichierDao.findAll());
        req.setAttribute("debug", debug);
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }
}