package controller;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.*;
import com.itextpdf.kernel.font.*;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.itextpdf.layout.borders.SolidBorder;

import dao.AffectationDAO;
import dao.AffectationDAOImpl;
import entities.Affectation;
import entities.Etudiant;
import entities.Professeur;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.util.List;
import java.io.IOException;
import java.util.*;

@WebServlet("/exportPdf")
public class Exportpdfservlet extends HttpServlet {

    private AffectationDAO affDao = new AffectationDAOImpl();

    // ── Couleurs ──────────────────────────────────────────────────────────────
    private static final DeviceRgb COLOR_HEADER = new DeviceRgb(26,  86, 219);  // bleu foncé
    private static final DeviceRgb COLOR_GI     = new DeviceRgb(207, 226, 255); // bleu clair
    private static final DeviceRgb COLOR_ID     = new DeviceRgb(255, 243, 205); // jaune pâle
    private static final DeviceRgb COLOR_TDIA   = new DeviceRgb(217, 234, 211); // vert pâle
    private static final DeviceRgb COLOR_EMPTY  = new DeviceRgb(248, 249, 250); // gris très clair

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<Affectation> affectations = affDao.findAllWithDetails();

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=affectations.pdf");

        PdfWriter   writer = new PdfWriter(resp.getOutputStream());
        PdfDocument pdf    = new PdfDocument(writer);
        // Paysage A4 pour avoir de la place
        pdf.setDefaultPageSize(PageSize.A4.rotate());
        Document doc = new Document(pdf);
        doc.setMargins(30, 30, 30, 30);

        PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ── En-tête document ─────────────────────────────────────────────────
        doc.add(new Paragraph("École Nationale des Sciences Appliquées – Al Hoceima")
                .setFont(bold).setFontSize(13).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Département Mathématiques et Informatique")
                .setFont(normal).setFontSize(11).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Affectation des encadrants de Projet de Fin d'Etude")
                .setFont(normal).setFontSize(11).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Année Universitaire 2025/2026")
                .setFont(normal).setFontSize(10).setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));

        // ── Légende ──────────────────────────────────────────────────────────
        Table legend = new Table(UnitValue.createPercentArray(new float[]{10, 10}))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginBottom(10);

        legend.addCell(legendCell("Filière ID", COLOR_ID, normal));
        legend.addCell(legendCell("Filière GI", COLOR_GI, normal));
        legend.addCell(legendCell("Filière TDIA", COLOR_TDIA, normal));
        doc.add(legend);

        // ── Grouper par encadrant ────────────────────────────────────────────
        Map<Professeur, List<Etudiant>> map = new LinkedHashMap<>();
        affectations.sort(Comparator.comparing(a -> a.getEncadrant().getNom()));
        for (Affectation a : affectations) {
            map.computeIfAbsent(a.getEncadrant(), k -> new ArrayList<>()).add(a.getEtudiant());
        }

        // ── Tableau principal ─────────────────────────────────────────────────
        // Colonnes : NomProf | PrenomProf | Nom1 | Prenom1 | Nom2 | Prenom2 | Nom3 | Prenom3 | Nom4 | Prenom4
        float[] cols = {12, 12, 9, 9, 9, 9, 9, 9, 9, 9};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        // ── En-tête niveau 1 ──
        table.addHeaderCell(headerCell("Encadrant", bold, 2, false));
        table.addHeaderCell(headerCell("Etudiants encadrés", bold, 8, false));

        // ── En-tête niveau 2 ──
        table.addHeaderCell(subHeaderCell("Nom", bold));
        table.addHeaderCell(subHeaderCell("Prénom", bold));
        for (int i = 1; i <= 4; i++) {
            table.addHeaderCell(subHeaderCell("Etudiant " + i + " - Nom", bold));
            table.addHeaderCell(subHeaderCell("Etudiant " + i + " - Prénom", bold));
        }

        // ── Données ──────────────────────────────────────────────────────────
        for (Map.Entry<Professeur, List<Etudiant>> entry : map.entrySet()) {
            Professeur prof = entry.getKey();
            List<Etudiant> list = entry.getValue();

            // Colonne encadrant
            table.addCell(profCell(prof.getNom(),    bold, COLOR_HEADER));
            table.addCell(profCell(prof.getPrenom(), bold, COLOR_HEADER));

            // Jusqu'à 4 étudiants
            for (int i = 0; i < 4; i++) {
                if (i < list.size()) {
                    Etudiant e = list.get(i);
                    DeviceRgb color = filiereColor(e.getFiliere());
                    table.addCell(etuCell(e.getNomE(),    normal, color));
                    table.addCell(etuCell(e.getPrenomE(), normal, color));
                } else {
                    table.addCell(etuCell("", normal, COLOR_EMPTY));
                    table.addCell(etuCell("", normal, COLOR_EMPTY));
                }
            }
        }

        doc.add(table);
        doc.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Cell legendCell(String label, DeviceRgb color, PdfFont font) {
        return new Cell().add(new Paragraph(label).setFont(font).setFontSize(9))
                .setBackgroundColor(color)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(3);
    }

    /** Cellule d'en-tête fusionnée sur `colspan` colonnes */
    private Cell headerCell(String text, PdfFont font, int colspan, boolean sub) {
        Cell c = new Cell(1, colspan)
                .add(new Paragraph(text).setFont(font)
                        .setFontColor(ColorConstants.WHITE).setFontSize(10))
                .setBackgroundColor(COLOR_HEADER)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(4);
        return c;
    }

    private Cell subHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font)
                        .setFontColor(ColorConstants.WHITE).setFontSize(8))
                .setBackgroundColor(COLOR_HEADER)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(3);
    }

    private Cell profCell(String text, PdfFont font, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text).setFont(font)
                        .setFontColor(ColorConstants.WHITE).setFontSize(9))
                .setBackgroundColor(bg)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(4);
    }

    private Cell etuCell(String text, PdfFont font, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(8))
                .setBackgroundColor(bg)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(3);
    }

    private DeviceRgb filiereColor(String filiere) {
        if ("GI".equals(filiere))   return COLOR_GI;
        if ("ID".equals(filiere))   return COLOR_ID;
        if ("TDIA".equals(filiere)) return COLOR_TDIA;
        return COLOR_EMPTY;
    }
}