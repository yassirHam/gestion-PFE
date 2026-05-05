package controller;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

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

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import util.ExcelImporter;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@WebServlet("*.do")
@MultipartConfig(maxFileSize = 10485760) // 10MB
public class FrontController extends HttpServlet {

    private EtudiantDAO etuDao = new EtudiantDAOImpl();
    private ProfesseurDAO profDao = new ProfesseurDAOImpl();
    private AffectationDAO affDao = new AffectationDAOImpl();
    private FichierListeDAO fichierDao = new FichierListeDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        switch (path) {
            case "/affectation.do":
                doAffectation(req, resp);
                break;
            case "/uploadEtudiants.do":
                doUploadEtudiants(req, resp);
                break;
            case "/uploadProfs.do":
                doUploadProfs(req, resp);
                break;
            case "/lancerAffectation.do":
                doLancerAffectation(req, resp);
                break;
            case "/supprimerListes.do":
                doSupprimerListes(req, resp);
                break;
            case "/exportPdf.do":
                doExportPdf(req, resp);
                break;
            case "/exportDocx.do":
                doExportDocx(req, resp);
                break;
            default:
                req.getRequestDispatcher("index.jsp").forward(req, resp);
                break;
        }
    }

    private void doAffectation(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("fichiers", fichierDao.findAll());
        
        // Populate grouped data if any
        Map<Professeur, List<Affectation>> grouped = new LinkedHashMap<>();
        List<Affectation> allAffectations = affDao.findAllWithDetails();
        for (Affectation a : allAffectations) {
            grouped.computeIfAbsent(a.getEncadrant(), k -> new ArrayList<>()).add(a);
        }
        if(!grouped.isEmpty()){
            req.setAttribute("grouped", grouped);
        }
        
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }

    private void doUploadEtudiants(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

                etuDao.deleteByFiliere(filiere);
                etuDao.saveAll(list);
                debug.add( list.size() + " étudiants [" + filiere + "] sauvegardés");

                fichierDao.deleteByFiliere(filiere);
                fichierDao.save(new FichierListe(fileName, filiere, list.size()));

            } catch (Exception e) {
                debug.add("Erreur: " + fileName + " → " + e.getMessage());
                e.printStackTrace();
            }
        }

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
        return base.toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }

    private void doUploadProfs(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> debug = new ArrayList<>();
        Part file = req.getPart("files");

        if (file == null || file.getSize() == 0) {
            debug.add("Aucun fichier reçu");
            req.setAttribute("debug", debug);
            req.getRequestDispatcher("affectation.jsp").forward(req, resp);
            return;
        }

        try {
            List<Professeur> list = ExcelImporter.importProfs(file.getInputStream());
            
            profDao.deleteAll();
            profDao.saveAll(list);
            
            debug.add(list.size() + " professeurs importés avec succès !");

        } catch (Exception e) {
            debug.add("Erreur: " + e.getMessage());
            e.printStackTrace();
        }

        req.setAttribute("debug", debug);
        req.setAttribute("message", "Profs importés ");
        req.setAttribute("fichiers", fichierDao.findAll());
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }

    private void doSupprimerListes(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

    private void doLancerAffectation(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

        // ── Algorithme fair + random ─────────────────────────────────────────
        try (org.hibernate.Session session = util.HibernateUtil.getSessionFactory().openSession()) {
            org.hibernate.Transaction tx = session.beginTransaction();
            session.createMutationQuery(
                "delete from Affectation a where a.etudiant.filiere in (:filieres)")
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
        // ─────────────────────────────────────────────────────────────────────

        affDao.saveAll(result);
        debug.add(result.size() + " affectations enregistrées");

        // Stocker les filières sélectionnées en session pour filtrer l'export
        req.getSession().setAttribute("lastFilieres", filieres);

        req.setAttribute("affectationDone", true);
        req.setAttribute("fichiers", fichierDao.findAll());
        req.setAttribute("debug", debug);
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }

    // Couleurs PDF — valeurs float (0-1) pour iText 7
    private static final DeviceRgb COLOR_HEADER = new DeviceRgb(0.102f, 0.337f, 0.859f); // #1A56DB
    private static final DeviceRgb COLOR_GI     = new DeviceRgb(0.812f, 0.886f, 1.000f); // #CFE2FF
    private static final DeviceRgb COLOR_ID     = new DeviceRgb(1.000f, 0.953f, 0.804f); // #FFF3CD
    private static final DeviceRgb COLOR_TDIA   = new DeviceRgb(0.851f, 0.918f, 0.827f); // #D9EAD3
    private static final DeviceRgb COLOR_EMPTY  = new DeviceRgb(0.973f, 0.976f, 0.980f); // #F8F9FA

    @SuppressWarnings("unchecked")
    private void doExportPdf(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Affectation> all = affDao.findAllWithDetails();

        // Filtrer par les filières du dernier lancement
        List<String> lastFilieres = (List<String>) req.getSession().getAttribute("lastFilieres");
        List<Affectation> affectations;
        if (lastFilieres != null && !lastFilieres.isEmpty()) {
            affectations = new ArrayList<>();
            for (Affectation a : all) {
                if (lastFilieres.contains(a.getEtudiant().getFiliere())) {
                    affectations.add(a);
                }
            }
        } else {
            affectations = all;
        }

        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=affectations.pdf");

        PdfWriter   writer = new PdfWriter(resp.getOutputStream());
        PdfDocument pdf    = new PdfDocument(writer);
        pdf.setDefaultPageSize(PageSize.A4.rotate());
        com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);
        doc.setMargins(30, 30, 30, 30);

        PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        doc.add(new Paragraph("École Nationale des Sciences Appliquées – Al Hoceima")
                .setFont(bold).setFontSize(13).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("Département Mathématiques et Informatique")
                .setFont(normal).setFontSize(11).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("Affectation des encadrants de Projet de Fin d'Etude")
                .setFont(normal).setFontSize(11).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("Année Universitaire 2025/2026")
                .setFont(normal).setFontSize(10).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(10));

        Table legend = new Table(UnitValue.createPercentArray(new float[]{10, 10}))
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                .setMarginBottom(10);

        legend.addCell(legendCell("Filière ID", COLOR_ID, normal));
        legend.addCell(legendCell("Filière GI", COLOR_GI, normal));
        legend.addCell(legendCell("Filière TDIA", COLOR_TDIA, normal));
        doc.add(legend);

        Map<Professeur, List<Etudiant>> map = new LinkedHashMap<>();
        affectations.sort(Comparator.comparing((Affectation a) -> a.getEncadrant().getNom()));
        for (Affectation a : affectations) {
            map.computeIfAbsent(a.getEncadrant(), k -> new ArrayList<>()).add(a.getEtudiant());
        }

        int maxStudents = 4;
        for (List<Etudiant> l : map.values()) {
            if (l.size() > maxStudents) maxStudents = l.size();
        }

        float[] cols = new float[2 + maxStudents * 2];
        cols[0] = 12; cols[1] = 12;
        for (int i = 2; i < cols.length; i++) cols[i] = 9;

        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        table.addHeaderCell(headerCell("Encadrant", bold, 2, false));
        table.addHeaderCell(headerCell("Etudiants encadrés", bold, maxStudents * 2, false));

        table.addHeaderCell(subHeaderCell("Nom", bold));
        table.addHeaderCell(subHeaderCell("Prénom", bold));
        for (int i = 1; i <= maxStudents; i++) {
            table.addHeaderCell(subHeaderCell("Etudiant " + i + " - Nom", bold));
            table.addHeaderCell(subHeaderCell("Etudiant " + i + " - Prénom", bold));
        }

        for (Map.Entry<Professeur, List<Etudiant>> entry : map.entrySet()) {
            Professeur prof = entry.getKey();
            List<Etudiant> list = entry.getValue();

            table.addCell(profCell(prof.getNom(),    bold, COLOR_HEADER));
            table.addCell(profCell(prof.getPrenom(), bold, COLOR_HEADER));

            for (int i = 0; i < maxStudents; i++) {
                if (i < list.size()) {
                    Etudiant e = list.get(i);
                    DeviceRgb color = filiereColorPdf(e.getFiliere());
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

    private Cell legendCell(String label, DeviceRgb color, PdfFont font) {
        return new Cell().add(new Paragraph(label).setFont(font).setFontSize(9))
                .setBackgroundColor(color)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.GRAY, 0.5f))
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setPadding(3);
    }

    private Cell headerCell(String text, PdfFont font, int colspan, boolean sub) {
        Cell c = new Cell(1, colspan)
                .add(new Paragraph(text).setFont(font)
                        .setFontColor(ColorConstants.WHITE).setFontSize(10))
                .setBackgroundColor(COLOR_HEADER)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setPadding(4);
        return c;
    }

    private Cell subHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font)
                        .setFontColor(ColorConstants.WHITE).setFontSize(8))
                .setBackgroundColor(COLOR_HEADER)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
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
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(3);
    }

    private DeviceRgb filiereColorPdf(String filiere) {
        if ("GI".equals(filiere))   return COLOR_GI;
        if ("ID".equals(filiere))   return COLOR_ID;
        if ("TDIA".equals(filiere)) return COLOR_TDIA;
        return COLOR_EMPTY;
    }

    // Couleurs DOCX — hex RGB sans #
    private static final String C_HEADER_DOCX = "1A56DB"; // bleu
    private static final String C_GI_DOCX     = "CFE2FF"; // bleu clair
    private static final String C_ID_DOCX     = "FFF3CD"; // jaune clair
    private static final String C_TDIA_DOCX   = "D9EAD3"; // vert clair
    private static final String C_EMPTY_DOCX  = "F8F9FA"; // gris très clair
    private static final String C_WHITE_DOCX  = "FFFFFF";

    @SuppressWarnings("unchecked")
    private void doExportDocx(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Affectation> all = affDao.findAllWithDetails();

        // Filtrer par les filières du dernier lancement
        List<String> lastFilieres = (List<String>) req.getSession().getAttribute("lastFilieres");
        List<Affectation> affectations;
        if (lastFilieres != null && !lastFilieres.isEmpty()) {
            affectations = new ArrayList<>();
            for (Affectation a : all) {
                if (lastFilieres.contains(a.getEtudiant().getFiliere())) {
                    affectations.add(a);
                }
            }
        } else {
            affectations = all;
        }

        resp.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        resp.setHeader("Content-Disposition", "attachment; filename=affectations.docx");

        try (XWPFDocument doc = new XWPFDocument()) {

            CTDocument1 ctDoc = doc.getDocument();
            CTBody body = ctDoc.getBody();
            if (!body.isSetSectPr()) body.addNewSectPr();
            CTSectPr sect = body.getSectPr();
            CTPageSz pgSz = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
            pgSz.setW(BigInteger.valueOf(16838)); 
            pgSz.setH(BigInteger.valueOf(11906)); 
            pgSz.setOrient(STPageOrientation.LANDSCAPE);

            center(doc, "École Nationale des Sciences Appliquées – Al Hoceima", 14, true);
            center(doc, "Département Mathématiques et Informatique", 12, false);
            center(doc, "Affectation des encadrants de Projet de Fin d'Etude", 11, false);
            center(doc, "Année Universitaire 2025/2026", 10, false);
            doc.createParagraph(); 

            XWPFTable legend = doc.createTable(1, 3);
            setWidth(legend, 4000);
            setLegendCellDocx(legend.getRow(0).getCell(0), "Filière ID",   C_ID_DOCX);
            setLegendCellDocx(legend.getRow(0).getCell(1), "Filière GI",   C_GI_DOCX);
            setLegendCellDocx(legend.getRow(0).getCell(2), "Filière TDIA", C_TDIA_DOCX);
            doc.createParagraph();

            Map<Professeur, List<Etudiant>> map = new LinkedHashMap<>();
            affectations.sort(Comparator.comparing((Affectation a) -> a.getEncadrant().getNom()));
            for (Affectation a : affectations) {
                map.computeIfAbsent(a.getEncadrant(), k -> new ArrayList<>()).add(a.getEtudiant());
            }

            int maxStudents = 4;
            for (List<Etudiant> l : map.values()) {
                if (l.size() > maxStudents) maxStudents = l.size();
            }

            XWPFTable table = doc.createTable();
            setWidth(table, 9500); 

            XWPFTableRow row0 = table.getRow(0);
            while (row0.getTableCells().size() < 2 + maxStudents * 2) row0.addNewTableCell();

            setCellMergeH(row0, 0, 1, "Encadrant",          C_HEADER_DOCX, true, true, 10);
            setCellMergeH(row0, 2, 1 + maxStudents * 2, "Etudiants encadrés", C_HEADER_DOCX, true, true, 10);

            XWPFTableRow row1 = table.createRow();
            while (row1.getTableCells().size() < 2 + maxStudents * 2) row1.addNewTableCell();

            setCellDocx(row1.getCell(0), "Nom",    C_HEADER_DOCX, true, true, 9);
            setCellDocx(row1.getCell(1), "Prénom", C_HEADER_DOCX, true, true, 9);
            for (int i = 1; i <= maxStudents; i++) {
                setCellDocx(row1.getCell((i - 1) * 2 + 2), "Etudiant " + i, C_HEADER_DOCX, true, true, 9);
                setCellDocx(row1.getCell((i - 1) * 2 + 3), "",              C_HEADER_DOCX, true, true, 9);
            }

            for (Map.Entry<Professeur, List<Etudiant>> entry : map.entrySet()) {
                Professeur prof = entry.getKey();
                List<Etudiant> list = entry.getValue();

                XWPFTableRow row = table.createRow();
                while (row.getTableCells().size() < 2 + maxStudents * 2) row.addNewTableCell();

                setCellDocx(row.getCell(0), prof.getNom(),    C_HEADER_DOCX, true, true,  9);
                setCellDocx(row.getCell(1), prof.getPrenom(), C_HEADER_DOCX, true, true,  9);

                for (int i = 0; i < maxStudents; i++) {
                    String nom    = "";
                    String prenom = "";
                    String color  = C_EMPTY_DOCX;

                    if (i < list.size()) {
                        Etudiant e = list.get(i);
                        nom    = e.getNomE();
                        prenom = e.getPrenomE();
                        color  = filiereColorDocx(e.getFiliere());
                    }

                    setCellDocx(row.getCell(i * 2 + 2), nom,    color, false, false, 8);
                    setCellDocx(row.getCell(i * 2 + 3), prenom, color, false, false, 8);
                }
            }

            doc.write(resp.getOutputStream());
        }
    }

    private void center(XWPFDocument doc, String text, int size, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setBold(bold);
    }

    private void setCellDocx(XWPFTableCell cell, String text, String bgColor, boolean whiteText, boolean bold, int size) {
        setCellBg(cell, bgColor);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(size);
        if (whiteText) run.setColor(C_WHITE_DOCX);
    }

    private void setCellMergeH(XWPFTableRow row, int from, int to, String text, String bgColor, boolean whiteText, boolean bold, int size) {
        XWPFTableCell first = row.getCell(from);
        CTTcPr tcPr0 = getTcPr(first);
        CTHMerge hm0 = tcPr0.isSetHMerge() ? tcPr0.getHMerge() : tcPr0.addNewHMerge();
        hm0.setVal(STMerge.RESTART);
        setCellDocx(first, text, bgColor, whiteText, bold, size);

        for (int i = from + 1; i <= to; i++) {
            XWPFTableCell c = row.getCell(i);
            CTTcPr tcPr = getTcPr(c);
            CTHMerge hm = tcPr.isSetHMerge() ? tcPr.getHMerge() : tcPr.addNewHMerge();
            hm.setVal(STMerge.CONTINUE);
            setCellBg(c, bgColor);
        }
    }

    private CTTcPr getTcPr(XWPFTableCell cell) {
        CTTc ctTc = cell.getCTTc();
        return ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
    }

    private void setCellBg(XWPFTableCell cell, String color) {
        CTTcPr tcPr = getTcPr(cell);
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setFill(color);
        shd.setVal(STShd.CLEAR);
    }

    private void setWidth(XWPFTable table, int widthTwips) {
        CTTbl tbl = table.getCTTbl();
        CTTblPr tblPr = tbl.getTblPr();
        if (tblPr == null) {
            tblPr = tbl.addNewTblPr();
        }

        CTTblWidth w = tblPr.getTblW();
        if (w == null) {
            w = tblPr.addNewTblW();
        }

        w.setW(BigInteger.valueOf(widthTwips));
        w.setType(STTblWidth.DXA);
    }

    private void setLegendCellDocx(XWPFTableCell cell, String label, String color) {
        setCellBg(cell, color);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(label);
        run.setFontSize(9);
    }

    private String filiereColorDocx(String filiere) {
        if ("GI".equals(filiere))   return C_GI_DOCX;
        if ("ID".equals(filiere))   return C_ID_DOCX;
        if ("TDIA".equals(filiere)) return C_TDIA_DOCX;
        return C_EMPTY_DOCX;
    }
}
