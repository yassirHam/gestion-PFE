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
import entities.Jury;
import entities.Professeur;
import entities.Salle;
import entities.Soutenance;

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
import java.text.SimpleDateFormat;
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

    private services.PfeService service = new services.PfeServiceImpl();

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
            case "/dashboard.do":
                doDashboard(req, resp);
                break;
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
            case "/restoreAffectation.do":
                doRestoreAffectation(req, resp);
                break;
            case "/planning.do":
                doPlanning(req, resp);
                break;
            case "/lancerPlanning.do":
                doLancerPlanning(req, resp);
                break;
            case "/addSalle.do":
                doAddSalle(req, resp);
                break;
            case "/downloadHistory.do":
                doDownloadHistory(req, resp);
                break;
            case "/clearHistory.do":
                doClearHistory(req, resp);
                break;
            case "/planningPdf.do":
                doPlanningPdf(req, resp);
                break;
            case "/planningDocx.do":
                doPlanningDocx(req, resp);
                break;
            case "/templateEtudiants.do":
                doTemplateEtudiants(req, resp);
                break;
            case "/templateProfs.do":
                doTemplateProfs(req, resp);
                break;
            default:
                req.getRequestDispatcher("index.jsp").forward(req, resp);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void doDashboard(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query = req.getParameter("q");
        if (query != null && !query.trim().isEmpty()) {
            Map<String, Object> searchResult = service.searchDashboard(query);
            req.setAttribute("searchResult", searchResult);
            req.setAttribute("searchQuery", query);
        }

        @SuppressWarnings("unchecked")
        List<String> lastFilieres = (List<String>) req.getSession().getAttribute("lastFilieres");
        
        int totalEtudiants = service.getTotalEtudiantsAffectes(lastFilieres);
        int totalProfs = service.getTotalProfesseursEncadrants(lastFilieres);
        int totalSoutenances = service.getTotalSoutenances(lastFilieres);
        
        Map<String, Integer> etudiantsParProf = service.getEtudiantsParProf(lastFilieres);
        Map<String, Integer> etudiantsParFiliere = service.getEtudiantsParFiliere(lastFilieres);
        Map<String, Integer> soutenancesParProf = service.getSoutenancesParProf(lastFilieres);

        req.setAttribute("totalEtudiants", totalEtudiants);
        req.setAttribute("totalProfs", totalProfs);
        req.setAttribute("totalSoutenances", totalSoutenances);
        try {
            services.VerificationReport verificationReport = service.verifierFichiersGeneres(lastFilieres);
            req.setAttribute("verificationReport", verificationReport);
        } catch (Exception e) {
            req.setAttribute("verificationError", "Verification indisponible pour le moment : " + e.getMessage());
        }
        
        StringBuilder labelsProf = new StringBuilder("[");
        StringBuilder dataProf = new StringBuilder("[");
        for (Map.Entry<String, Integer> entry : etudiantsParProf.entrySet()) {
            labelsProf.append("'").append(entry.getKey().replace("'", "\\'")).append("',");
            dataProf.append(entry.getValue()).append(",");
        }
        if(labelsProf.length() > 1) { labelsProf.setLength(labelsProf.length()-1); dataProf.setLength(dataProf.length()-1); }
        labelsProf.append("]");
        dataProf.append("]");

        StringBuilder labelsFil = new StringBuilder("[");
        StringBuilder dataFil = new StringBuilder("[");
        for (Map.Entry<String, Integer> entry : etudiantsParFiliere.entrySet()) {
            labelsFil.append("'").append(entry.getKey().replace("'", "\\'")).append("',");
            dataFil.append(entry.getValue()).append(",");
        }
        if(labelsFil.length() > 1) { labelsFil.setLength(labelsFil.length()-1); dataFil.setLength(dataFil.length()-1); }
        labelsFil.append("]");
        dataFil.append("]");

        StringBuilder labelsSoutProf = new StringBuilder("[");
        StringBuilder dataSoutProf = new StringBuilder("[");
        for (Map.Entry<String, Integer> entry : soutenancesParProf.entrySet()) {
            labelsSoutProf.append("'").append(entry.getKey().replace("'", "\\'")).append("',");
            dataSoutProf.append(entry.getValue()).append(",");
        }
        if(labelsSoutProf.length() > 1) { labelsSoutProf.setLength(labelsSoutProf.length()-1); dataSoutProf.setLength(dataSoutProf.length()-1); }
        labelsSoutProf.append("]");
        dataSoutProf.append("]");

        req.setAttribute("labelsProf", labelsProf.toString());
        req.setAttribute("dataProf", dataProf.toString());
        req.setAttribute("labelsFil", labelsFil.toString());
        req.setAttribute("dataFil", dataFil.toString());
        req.setAttribute("labelsSoutProf", labelsSoutProf.toString());
        req.setAttribute("dataSoutProf", dataSoutProf.toString());

        req.getRequestDispatcher("dashboard.jsp").forward(req, resp);
    }

    private void doClearHistory(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type"); // "planning" or "affectation"
        String prefix = "Planning_";
        if ("affectation".equals(type)) prefix = "Affectation_";

        java.io.File historyDir = new java.io.File(getHistoryFolder());
        if (historyDir.exists() && historyDir.isDirectory()) {
            java.io.File[] files = historyDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isFile() && f.getName().startsWith(prefix)) {
                        f.delete();
                    }
                }
            }
        }
        String referer = req.getHeader("referer");
        if (referer != null) resp.sendRedirect(referer);
        else resp.sendRedirect("index.jsp");
    }

    private void doAffectation(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("fichiers", service.getAllFichiers());
        
        // Load History (Group by timestamp)
        java.io.File historyDir = new java.io.File(getHistoryFolder());
        List<String> historyTimestamps = new ArrayList<>();
        if (historyDir.exists() && historyDir.isDirectory()) {
            java.io.File[] files = historyDir.listFiles();
            if (files != null) {
                java.util.Set<String> tsSet = new java.util.HashSet<>();
                for (java.io.File f : files) {
                    if (f.isFile() && f.getName().startsWith("Affectation_")) {
                        // Extract timestamp: Affectation_YYYY-MM-DD_HH-mm-ss.ext
                        String name = f.getName();
                        int extIndex = name.lastIndexOf('.');
                        if (extIndex > 12) {
                            tsSet.add(name.substring(12, extIndex));
                        }
                    }
                }
                historyTimestamps.addAll(tsSet);
            }
        }
        java.util.Collections.sort(historyTimestamps, java.util.Collections.reverseOrder());
        req.setAttribute("historyTimestamps", historyTimestamps);
        // req.setAttribute("historyFiles", historyFiles); replaced by historyTimestamps

        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }
    
    private void doRestoreAffectation(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ts = req.getParameter("timestamp");
        if (ts != null && !ts.isEmpty()) {
            java.io.File txtFile = new java.io.File(getHistoryFolder(), "Affectation_" + ts + ".txt");
            if (txtFile.exists()) {
                try {
                    service.restoreAffectation(txtFile);
                    req.getSession().setAttribute("affectationDone", true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                req.getSession().setAttribute("restoreError", "Désolé, cette affectation est trop ancienne et ne possède pas de sauvegarde de données (uniquement PDF/Word).");
            }
        }
        resp.sendRedirect("affectation.do");
    }
    private void doTemplateEtudiants(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Etudiants");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("CNE");
            header.createCell(1).setCellValue("NOM");
            header.createCell(2).setCellValue("PRÉNOM");
            header.createCell(3).setCellValue("EMAIL");
            header.createCell(4).setCellValue("CNE BINÔME (Optionnel)");

            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setHeader("Content-Disposition", "attachment; filename=modele_etudiants.xlsx");
            wb.write(resp.getOutputStream());
        }
    }

    private void doTemplateProfs(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Professeurs");
            
            // Les deux premières lignes sont ignorées d'après les règles, mais on met des headers sur la ligne 1 pour l'utilisateur
            org.apache.poi.ss.usermodel.Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("En-tête décorative 1");
            
            org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("NOM");
            row1.createCell(1).setCellValue("PRÉNOM");
            row1.createCell(2).setCellValue("SPÉCIALITÉ");

            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setHeader("Content-Disposition", "attachment; filename=modele_professeurs.xlsx");
            wb.write(resp.getOutputStream());
        }
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

                service.saveEtudiants(list, filiere, fileName);
                debug.add( list.size() + " étudiants [" + filiere + "] sauvegardés");

            } catch (Exception e) {
                debug.add("Erreur: " + fileName + " → " + e.getMessage());
                e.printStackTrace();
            }
        }

        req.setAttribute("fichiers", service.getAllFichiers());
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
            
            service.deleteAffectationsAndProfesseurs();
            service.saveProfesseurs(list);
            
            debug.add(list.size() + " professeurs importés avec succès !");

        } catch (Exception e) {
            debug.add("Erreur: " + e.getMessage());
            e.printStackTrace();
        }

        req.setAttribute("debug", debug);
        req.setAttribute("message", "Profs importés ");
        req.setAttribute("fichiers", service.getAllFichiers());
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }

    private void doSupprimerListes(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] selected = req.getParameterValues("selectedFilieres");
        List<String> debug = new ArrayList<>();

        if (selected == null || selected.length == 0) {
            debug.add("Aucune filière sélectionnée");
        } else {
            for (String filiere : selected) {
                service.deleteEtudiantsByFiliere(filiere);
                debug.add("Liste [" + filiere + "] supprimée");
            }
        }

        req.setAttribute("fichiers", service.getAllFichiers());
        req.setAttribute("debug", debug);
        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }

    private void doLancerAffectation(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] selected = req.getParameterValues("selectedFilieres");
        List<String> debug = new ArrayList<>();

        if (selected == null || selected.length == 0) {
            debug.add("⚠️ Sélectionnez au moins une filière");
            req.setAttribute("fichiers", service.getAllFichiers());
            req.setAttribute("debug", debug);
            req.getRequestDispatcher("affectation.jsp").forward(req, resp);
            return;
        }

        List<String> filieres = Arrays.asList(selected);
        debug.add("Filières sélectionnées: " + String.join(", ", filieres));

        service.lancerAffectationGlobale(filieres, debug);
        
        // Stocker les filières sélectionnées en session pour filtrer l'export
        req.getSession().setAttribute("lastFilieres", filieres);
        req.getSession().setAttribute("affectationDone", true);
        req.getSession().setAttribute("affectationDebug", debug);
        
        // Auto-save history files
        java.io.File historyDir = new java.io.File(getHistoryFolder());
        if (!historyDir.exists()) historyDir.mkdirs();
        
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        String pdfName = "Affectation_" + timestamp + ".pdf";
        String docxName = "Affectation_" + timestamp + ".docx";
        String txtName = "Affectation_" + timestamp + ".txt";
        
        try (java.io.FileOutputStream pdfOut = new java.io.FileOutputStream(new java.io.File(historyDir, pdfName));
             java.io.FileOutputStream docxOut = new java.io.FileOutputStream(new java.io.File(historyDir, docxName));
             java.io.PrintWriter txtOut = new java.io.PrintWriter(new java.io.File(historyDir, txtName))) {
            generateAffectationPdfToStream(pdfOut, filieres);
            generateAffectationDocxToStream(docxOut, filieres);
            
            // Save raw data for restore
            for (entities.Affectation a : service.getAllAffectationsWithDetails()) {
                if (a.getEtudiant() != null && a.getEncadrant() != null) {
                    txtOut.println(a.getEtudiant().getIde() + "," + a.getEncadrant().getIdp());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        req.setAttribute("fichiers", service.getAllFichiers());
        req.setAttribute("debug", debug);
        
        // Redirect instead of forward to avoid duplicate history on refresh
        resp.sendRedirect("affectation.do");
    }

    // Couleurs PDF — valeurs float (0-1) pour iText 7
    private static final DeviceRgb COLOR_HEADER = new DeviceRgb(0.000f, 0.000f, 0.000f); // Black
    private static final DeviceRgb COLOR_HEADER_AFFECTATION = new DeviceRgb(18, 52, 153); // #123499
    // Dark colors for Planning
    private static final DeviceRgb COLOR_GI     = new DeviceRgb(0.400f, 0.600f, 0.900f); // Darker Blue
    private static final DeviceRgb COLOR_ID     = new DeviceRgb(0.950f, 0.800f, 0.300f); // Darker Yellow
    private static final DeviceRgb COLOR_TDIA   = new DeviceRgb(0.450f, 0.750f, 0.450f); // Darker Green
    // Light colors for Affectation
    private static final DeviceRgb COLOR_GI_LIGHT     = new DeviceRgb(179, 136, 255); // #b388ff
    private static final DeviceRgb COLOR_ID_LIGHT     = new DeviceRgb(253, 161, 114); // #fda172
    private static final DeviceRgb COLOR_TDIA_LIGHT   = new DeviceRgb(0, 155, 0);     // #009b00
    private static final DeviceRgb COLOR_EMPTY  = new DeviceRgb(0.950f, 0.950f, 0.950f);

    @SuppressWarnings("unchecked")
    private void doExportPdf(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> lastFilieres = (List<String>) req.getSession().getAttribute("lastFilieres");
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=affectations.pdf");
        generateAffectationPdfToStream(resp.getOutputStream(), lastFilieres);
    }

    private void generateAffectationPdfToStream(java.io.OutputStream os, List<String> lastFilieres) throws IOException {
        List<Affectation> all = service.getAllAffectationsWithDetails();

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

        PdfWriter   writer = new PdfWriter(os);
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

        legend.addCell(legendCell("Filière ID", COLOR_ID_LIGHT, normal));
        legend.addCell(legendCell("Filière GI", COLOR_GI_LIGHT, normal));
        legend.addCell(legendCell("Filière TDIA", COLOR_TDIA_LIGHT, normal));
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

        table.addHeaderCell(headerCell("Encadrant", bold, 2, false, COLOR_HEADER_AFFECTATION));
        table.addHeaderCell(headerCell("Etudiants encadrés", bold, maxStudents * 2, false, COLOR_HEADER_AFFECTATION));

        table.addHeaderCell(subHeaderCell("Nom", bold, COLOR_HEADER_AFFECTATION));
        table.addHeaderCell(subHeaderCell("Prénom", bold, COLOR_HEADER_AFFECTATION));
        for (int i = 1; i <= maxStudents; i++) {
            table.addHeaderCell(subHeaderCell("Etudiant " + i + " - Nom", bold, COLOR_HEADER_AFFECTATION));
            table.addHeaderCell(subHeaderCell("Etudiant " + i + " - Prénom", bold, COLOR_HEADER_AFFECTATION));
        }

        for (Map.Entry<Professeur, List<Etudiant>> entry : map.entrySet()) {
            Professeur prof = entry.getKey();
            List<Etudiant> list = entry.getValue();

            table.addCell(profCell(prof.getNom(),    bold, COLOR_HEADER_AFFECTATION));
            table.addCell(profCell(prof.getPrenom(), bold, COLOR_HEADER_AFFECTATION));

            for (int i = 0; i < maxStudents; i++) {
                if (i < list.size()) {
                    Etudiant e = list.get(i);
                    DeviceRgb color = filiereColorPdfAffectation(e.getFiliere());
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

    private Cell headerCell(String text, PdfFont font, int colspan, boolean sub, DeviceRgb bgColor) {
        Cell c = new Cell(1, colspan)
                .add(new Paragraph(text).setFont(font)
                        .setFontColor(ColorConstants.WHITE).setFontSize(10))
                .setBackgroundColor(bgColor)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE)
                .setPadding(4);
        return c;
    }

    private Cell headerCell(String text, PdfFont font, int colspan, boolean sub) {
        return headerCell(text, font, colspan, sub, COLOR_HEADER);
    }

    private Cell subHeaderCell(String text, PdfFont font, DeviceRgb bgColor) {
        return new Cell()
                .add(new Paragraph(text).setFont(font)
                        .setFontColor(ColorConstants.WHITE).setFontSize(8))
                .setBackgroundColor(bgColor)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(3);
    }

    private Cell subHeaderCell(String text, PdfFont font) {
        return subHeaderCell(text, font, COLOR_HEADER);
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

    private DeviceRgb filiereColorPdfAffectation(String filiere) {
        if ("GI".equals(filiere))   return COLOR_GI_LIGHT;
        if ("ID".equals(filiere))   return COLOR_ID_LIGHT;
        if ("TDIA".equals(filiere)) return COLOR_TDIA_LIGHT;
        return COLOR_EMPTY;
    }

    // Couleurs DOCX — hex RGB sans #
    private static final String C_HEADER_DOCX = "000000"; // Black
    private static final String C_HEADER_DOCX_AFFECTATION = "123499"; // #123499
    // Dark colors for Planning
    private static final String C_GI_DOCX     = "4F8AFF"; // Darker Blue
    private static final String C_ID_DOCX     = "FFC107"; // Darker Yellow
    private static final String C_TDIA_DOCX   = "689F38"; // Darker Green
    // Light colors for Affectation
    private static final String C_GI_DOCX_LIGHT     = "B388FF"; // #b388ff
    private static final String C_ID_DOCX_LIGHT     = "FDA172"; // #fda172
    private static final String C_TDIA_DOCX_LIGHT   = "009B00"; // #009b00
    private static final String C_EMPTY_DOCX  = "F0F0F0";
    private static final String C_WHITE_DOCX  = "FFFFFF";

    @SuppressWarnings("unchecked")
    private void doExportDocx(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<String> lastFilieres = (List<String>) req.getSession().getAttribute("lastFilieres");
        resp.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        resp.setHeader("Content-Disposition", "attachment; filename=affectations.docx");
        generateAffectationDocxToStream(resp.getOutputStream(), lastFilieres);
    }

    private void generateAffectationDocxToStream(java.io.OutputStream os, List<String> lastFilieres) throws IOException {
        List<Affectation> all = service.getAllAffectationsWithDetails();

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
            setLegendCellDocx(legend.getRow(0).getCell(0), "Filière ID",   C_ID_DOCX_LIGHT);
            setLegendCellDocx(legend.getRow(0).getCell(1), "Filière GI",   C_GI_DOCX_LIGHT);
            setLegendCellDocx(legend.getRow(0).getCell(2), "Filière TDIA", C_TDIA_DOCX_LIGHT);
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

            setCellMergeH(row0, 0, 1, "Encadrant",          C_HEADER_DOCX_AFFECTATION, true, true, 10);
            setCellMergeH(row0, 2, 1 + maxStudents * 2, "Etudiants encadrés", C_HEADER_DOCX_AFFECTATION, true, true, 10);

            XWPFTableRow row1 = table.createRow();
            while (row1.getTableCells().size() < 2 + maxStudents * 2) row1.addNewTableCell();

            setCellDocx(row1.getCell(0), "Nom",    C_HEADER_DOCX_AFFECTATION, true, true, 9);
            setCellDocx(row1.getCell(1), "Prénom", C_HEADER_DOCX_AFFECTATION, true, true, 9);
            for (int i = 1; i <= maxStudents; i++) {
                setCellDocx(row1.getCell((i - 1) * 2 + 2), "Etudiant " + i, C_HEADER_DOCX_AFFECTATION, true, true, 9);
                setCellDocx(row1.getCell((i - 1) * 2 + 3), "",              C_HEADER_DOCX_AFFECTATION, true, true, 9);
            }

            for (Map.Entry<Professeur, List<Etudiant>> entry : map.entrySet()) {
                Professeur prof = entry.getKey();
                List<Etudiant> list = entry.getValue();

                XWPFTableRow row = table.createRow();
                while (row.getTableCells().size() < 2 + maxStudents * 2) row.addNewTableCell();

                setCellDocx(row.getCell(0), prof.getNom(),    C_HEADER_DOCX_AFFECTATION, true, true,  9);
                setCellDocx(row.getCell(1), prof.getPrenom(), C_HEADER_DOCX_AFFECTATION, true, true,  9);

                for (int i = 0; i < maxStudents; i++) {
                    String nom    = "";
                    String prenom = "";
                    String color  = C_EMPTY_DOCX;

                    if (i < list.size()) {
                        Etudiant e = list.get(i);
                        nom    = e.getNomE();
                        prenom = e.getPrenomE();
                        color  = filiereColorDocxAffectation(e.getFiliere());
                    }

                    setCellDocx(row.getCell(i * 2 + 2), nom,    color, false, false, 8);
                    setCellDocx(row.getCell(i * 2 + 3), prenom, color, false, false, 8);
                }
            }
            doc.write(os);
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
    
    private String filiereColorDocxAffectation(String filiere) {
        if ("GI".equals(filiere))   return C_GI_DOCX_LIGHT;
        if ("ID".equals(filiere))   return C_ID_DOCX_LIGHT;
        if ("TDIA".equals(filiere)) return C_TDIA_DOCX_LIGHT;
        return C_EMPTY_DOCX;
    }


    private DeviceRgb getDateColorPdf(java.util.Date d) {
        if (d == null) return COLOR_EMPTY;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(d);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
        switch (day % 4) {
            case 0: return new DeviceRgb(0.85f, 0.40f, 0.40f); // Dark Red
            case 1: return new DeviceRgb(0.40f, 0.70f, 0.40f); // Dark Green
            case 2: return new DeviceRgb(0.35f, 0.55f, 0.85f); // Dark Blue
            case 3: return new DeviceRgb(0.85f, 0.65f, 0.35f); // Dark Orange
            default: return COLOR_EMPTY;
        }
    }

    private DeviceRgb getSalleColorPdf(String salle) {
        return COLOR_EMPTY;
    }
    

    private String getDateColorDocx(java.util.Date d) {
        if (d == null) return C_EMPTY_DOCX;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(d);
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
        switch (day % 4) {
            case 0: return "D32F2F";
            case 1: return "388E3C";
            case 2: return "1976D2";
            case 3: return "F57C00";
            default: return C_EMPTY_DOCX;
        }
    }

    private String getSalleColorDocx(String salle) {
        return C_EMPTY_DOCX;
    }

    private String getHeureColorDocx(String heure) {
        if (heure == null) return C_EMPTY_DOCX;
        if (heure.contains("9h")) return "00BCD4";   // Cyan
        if (heure.contains("10h")) return "FF9800";  // Amber
        if (heure.contains("11h")) return "795548";  // Brown
        if (heure.contains("14h")) return "E91E63";  // Pink
        if (heure.contains("15h")) return "607D8B";  // Blue Grey
        if (heure.contains("16h")) return "3F51B5";  // Indigo
        if (heure.contains("17h")) return "388E3C";  // Dark Green
        return C_EMPTY_DOCX;
    }

    private DeviceRgb getHeureColorPdf(String heure) {
        if (heure == null) return COLOR_EMPTY;
        if (heure.contains("9h")) return new DeviceRgb(0, 188, 212);   // Cyan
        if (heure.contains("10h")) return new DeviceRgb(255, 152, 0);  // Amber
        if (heure.contains("11h")) return new DeviceRgb(121, 85, 72);  // Brown
        if (heure.contains("14h")) return new DeviceRgb(233, 30, 99);  // Pink
        if (heure.contains("15h")) return new DeviceRgb(96, 125, 139); // Blue Grey
        if (heure.contains("16h")) return new DeviceRgb(63, 81, 181);  // Indigo
        if (heure.contains("17h")) return new DeviceRgb(56, 142, 60);  // Dark Green
        return COLOR_EMPTY;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PLANNING HANDLERS
    // ══════════════════════════════════════════════════════════════════════════

    private void doPlanning(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Soutenance> soutenances = service.getAllSoutenances();
        Map<Long, String> colors = service.getProfessorColors();
        req.setAttribute("soutenances", soutenances);
        req.setAttribute("profColors", colors);
        
        Map<String, String> profLegend = new LinkedHashMap<>();
        for (Soutenance s : soutenances) {
            profLegend.put(s.getJury().getPresident().getNom() + " " + s.getJury().getPresident().getPrenom(), colors.get(s.getJury().getPresident().getIdp()));
            profLegend.put(s.getJury().getRapporteur1().getNom() + " " + s.getJury().getRapporteur1().getPrenom(), colors.get(s.getJury().getRapporteur1().getIdp()));
            profLegend.put(s.getJury().getRapporteur2().getNom() + " " + s.getJury().getRapporteur2().getPrenom(), colors.get(s.getJury().getRapporteur2().getIdp()));
        }
        req.setAttribute("profLegend", profLegend);

        Map<String, String> filiereLegend = new LinkedHashMap<>();
        for (Soutenance s : soutenances) {
            String filiere = s.getEtudiant().getFiliere();
            if (!filiereLegend.containsKey(filiere)) {
                filiereLegend.put(filiere, filiereColorDocx(filiere));
            }
        }
        req.setAttribute("filiereLegend", filiereLegend);

        // Pass whether affectations exist
        boolean hasAffectations = service.getTotalEtudiantsAffectes(null) > 0;
        req.setAttribute("hasAffectations", hasAffectations);

        // Pass all Salles
        List<entities.Salle> salles = service.getAllSalles();
        req.setAttribute("salles", salles);
        
        // Load History
        java.io.File historyDir = new java.io.File(getHistoryFolder());
        List<String> historyFiles = new ArrayList<>();
        if (historyDir.exists() && historyDir.isDirectory()) {
            java.io.File[] files = historyDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isFile() && f.getName().startsWith("Planning_")) {
                        historyFiles.add(f.getName());
                    }
                }
            }
        }
        Collections.sort(historyFiles, Collections.reverseOrder());
        req.setAttribute("historyFiles", historyFiles);

        req.getRequestDispatcher("planning.jsp").forward(req, resp);
    }
    
    private String getHistoryFolder() {
        return System.getProperty("user.home") + java.io.File.separator + "plannings_history";
    }

    private void doDownloadHistory(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String filename = req.getParameter("file");
        if (filename == null || filename.contains("..") || (!filename.startsWith("Planning_") && !filename.startsWith("Affectation_"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        java.io.File file = new java.io.File(getHistoryFolder(), filename);
        if (!file.exists()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (filename.endsWith(".pdf")) {
            resp.setContentType("application/pdf");
        } else if (filename.endsWith(".docx")) {
            resp.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        java.nio.file.Files.copy(file.toPath(), resp.getOutputStream());
    }

    
    private void doAddSalle(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String numSalle = req.getParameter("numSalle");
        if (numSalle != null && !numSalle.trim().isEmpty()) {
            service.addSalle(numSalle.trim());
        }
        resp.sendRedirect("planning.do");
    }

    private void doLancerPlanning(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        // Prevent generation if no affectations
        if (service.getTotalEtudiantsAffectes(null) == 0) {
            resp.sendRedirect("planning.do");
            return;
        }

        String[] selected = req.getParameterValues("selectedSalles");
        List<Long> selectedSalles = new ArrayList<>();
        if (selected != null) {
            for (String sId : selected) {
                try { selectedSalles.add(Long.parseLong(sId)); } catch (Exception ignored) {}
            }
        }

        List<String> debug = new ArrayList<>();

        // Retrieve the filières that were used for the last affectation
        @SuppressWarnings("unchecked")
        List<String> lastFilieres = (List<String>) req.getSession().getAttribute("lastFilieres");

        String startDate = req.getParameter("startDate");

        service.genererPlanning(lastFilieres, debug, selectedSalles, startDate);

        List<Soutenance> soutenances = service.getAllSoutenances();
        Map<Long, String> colors = service.getProfessorColors();
        req.setAttribute("soutenances", soutenances);
        req.setAttribute("profColors", colors);
        
        Map<String, String> profLegend = new LinkedHashMap<>();
        for (Soutenance s : soutenances) {
            profLegend.put(s.getJury().getPresident().getNom() + " " + s.getJury().getPresident().getPrenom(), colors.get(s.getJury().getPresident().getIdp()));
            profLegend.put(s.getJury().getRapporteur1().getNom() + " " + s.getJury().getRapporteur1().getPrenom(), colors.get(s.getJury().getRapporteur1().getIdp()));
            profLegend.put(s.getJury().getRapporteur2().getNom() + " " + s.getJury().getRapporteur2().getPrenom(), colors.get(s.getJury().getRapporteur2().getIdp()));
        }
        req.setAttribute("profLegend", profLegend);

        req.setAttribute("hasAffectations", true);
        req.setAttribute("planningDone", true);
        
        // Auto-save history files
        java.io.File historyDir = new java.io.File(getHistoryFolder());
        if (!historyDir.exists()) historyDir.mkdirs();
        
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        String pdfName = "Planning_" + timestamp + ".pdf";
        String docxName = "Planning_" + timestamp + ".docx";
        
        try (java.io.FileOutputStream pdfOut = new java.io.FileOutputStream(new java.io.File(historyDir, pdfName));
             java.io.FileOutputStream docxOut = new java.io.FileOutputStream(new java.io.File(historyDir, docxName))) {
            generatePdfToStream(pdfOut);
            generateDocxToStream(docxOut);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        resp.sendRedirect("planning.do");
    }

    // ── Planning PDF export ───────────────────────────────────────────────────
    private void doPlanningPdf(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=planning_soutenances.pdf");
        generatePdfToStream(resp.getOutputStream());
    }

    private void generatePdfToStream(java.io.OutputStream os) throws IOException {
        List<Soutenance> soutenances = service.getAllSoutenances();
        Map<Long, String> colorMap   = service.getProfessorColors();

        PdfWriter   writer = new PdfWriter(os);
        PdfDocument pdf    = new PdfDocument(writer);
        pdf.setDefaultPageSize(PageSize.A4.rotate());
        com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);
        doc.setMargins(20, 20, 20, 20);

        PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Header
        doc.add(new Paragraph("École Nationale des Sciences Appliquées – Al Hoceima")
                .setFont(bold).setFontSize(12)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("Département Mathématiques et Informatique")
                .setFont(normal).setFontSize(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("Planning des soutenances des Projets de Fin d'Etude")
                .setFont(bold).setFontSize(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("(Première Session) — Année Universitaire 2025/2026")
                .setFont(normal).setFontSize(9)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(8));

        // Table columns: ID | Encadrant | Jury1 | Jury2 | Date | Heure | Salle | Nom | Prénom | Filière
        float[] cols = {3f, 12f, 12f, 12f, 8f, 6f, 6f, 9f, 9f, 5f};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        // Header row
        String[] headers = {"ID","Encadrant","Membre de jury 1","Membre de jury 2",
                            "Date","Heure","Salle","Nom d'étudiant","Prénom d'étudiant","Filière"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(8)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_HEADER)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setPadding(3));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        int id = 1;
        
        Map<String, List<Soutenance>> groups = new LinkedHashMap<>();
        for (Soutenance s : soutenances) {
            String key = s.getJury().getIdJury() + "_" + s.getDate() + "_" + s.getHeure() + "_" + s.getSalle().getId_salle();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        for (List<Soutenance> group : groups.values()) {
            Soutenance s = group.get(0);
            Professeur enc = s.getJury().getPresident();
            Professeur m1  = s.getJury().getRapporteur1();
            Professeur m2  = s.getJury().getRapporteur2();
            String filiere  = s.getEtudiant().getFiliere();

            DeviceRgb encColor = hexToRgb(colorMap.getOrDefault(enc.getIdp(), "1A56DB"));
            DeviceRgb m1Color  = hexToRgb(colorMap.getOrDefault(m1.getIdp(),  "2ECC71"));
            DeviceRgb m2Color  = hexToRgb(colorMap.getOrDefault(m2.getIdp(),  "E67E22"));
            DeviceRgb filColor = filiereColorPdf(filiere);
            DeviceRgb dateColor = getDateColorPdf(s.getDate());
            DeviceRgb timeColor = getHeureColorPdf(s.getHeure());
            DeviceRgb salleColor = getSalleColorPdf(s.getSalle().getNum_salle());

            String nomEtu = group.get(0).getEtudiant().getNomE();
            String prenomEtu = group.get(0).getEtudiant().getPrenomE();
            boolean isBinome = group.size() > 1;
            if (isBinome) {
                nomEtu += " & " + group.get(1).getEtudiant().getNomE();
                prenomEtu += " & " + group.get(1).getEtudiant().getPrenomE();
            }

            // ID
            table.addCell(planCell(String.valueOf(id++), normal, 8, COLOR_EMPTY, false, isBinome));
            // Encadrant
            table.addCell(planCell(enc.getNom() + " " + enc.getPrenom(), bold, 8, encColor, true, isBinome));
            // Jury 1
            table.addCell(planCell(m1.getNom() + " " + m1.getPrenom(), normal, 8, m1Color, true, isBinome));
            // Jury 2
            table.addCell(planCell(m2.getNom() + " " + m2.getPrenom(), normal, 8, m2Color, true, isBinome));
            // Date
            table.addCell(planCell(sdf.format(s.getDate()), normal, 8, dateColor, false, isBinome));
            // Heure
            table.addCell(planCell(s.getHeure(), bold, 8, timeColor, false, isBinome));
            // Salle
            table.addCell(planCell(s.getSalle().getNum_salle(), normal, 8, salleColor, false, isBinome));
            // Nom étudiant
            table.addCell(planCell(nomEtu, isBinome ? bold : normal, 8, filColor, false, isBinome));
            // Prénom étudiant
            table.addCell(planCell(prenomEtu, isBinome ? bold : normal, 8, filColor, false, isBinome));
            // Filière
            table.addCell(planCell(filiere, normal, 8, filColor, false, isBinome));
        }

        doc.add(table);
        doc.close();
    }

    private Cell planCell(String text, PdfFont font, int size, DeviceRgb bg, boolean white, boolean isBinome) {
        Cell c = new Cell()
                .add(new Paragraph(text == null ? "" : text).setFont(font).setFontSize(size))
                .setBackgroundColor(bg)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(2);
        if (white) c.setFontColor(ColorConstants.WHITE);
        if (isBinome) {
            c.setBorder(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.BLACK, 1.5f));
        }
        return c;
    }

    private DeviceRgb hexToRgb(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new DeviceRgb(r / 255f, g / 255f, b / 255f);
        } catch (Exception e) {
            return COLOR_EMPTY;
        }
    }

    // ── Planning DOCX export ──────────────────────────────────────────────────
    private void doPlanningDocx(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        resp.setHeader("Content-Disposition", "attachment; filename=planning_soutenances.docx");
        generateDocxToStream(resp.getOutputStream());
    }

    private void generateDocxToStream(java.io.OutputStream os) throws IOException {
        List<Soutenance> soutenances = service.getAllSoutenances();
        Map<Long, String> colorMap   = service.getProfessorColors();

        try (XWPFDocument doc = new XWPFDocument()) {
            // Landscape A4
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
            center(doc, "Planning des soutenances des Projets de Fin d'Etude", 11, true);
            center(doc, "(Première Session) — Année Universitaire 2025/2026", 10, false);
            doc.createParagraph();

            // 10 columns
            XWPFTable table = doc.createTable();
            setWidth(table, 13000);

            String[] headers = {"ID","Encadrant","Membre de jury 1","Membre de jury 2",
                                "Date","Heure","Salle","Nom d'étudiant","Prénom d'étudiant","Filière"};

            XWPFTableRow hRow = table.getRow(0);
            while (hRow.getTableCells().size() < headers.length) hRow.addNewTableCell();
            for (int i = 0; i < headers.length; i++) {
                setCellDocx(hRow.getCell(i), headers[i], C_HEADER_DOCX, true, true, 8);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            int id = 1;
            
            Map<String, List<Soutenance>> groups = new LinkedHashMap<>();
            for (Soutenance s : soutenances) {
                String key = s.getJury().getIdJury() + "_" + s.getDate() + "_" + s.getHeure() + "_" + s.getSalle().getId_salle();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }

            for (List<Soutenance> group : groups.values()) {
                Soutenance s = group.get(0);
                Professeur enc = s.getJury().getPresident();
                Professeur m1  = s.getJury().getRapporteur1();
                Professeur m2  = s.getJury().getRapporteur2();
                String filiere = s.getEtudiant().getFiliere();

                String encColor = colorMap.getOrDefault(enc.getIdp(), C_HEADER_DOCX);
                String m1Color  = colorMap.getOrDefault(m1.getIdp(),  "2ECC71");
                String m2Color  = colorMap.getOrDefault(m2.getIdp(),  "E67E22");
                String filColor = filiereColorDocx(filiere);
                String dateColor = getDateColorDocx(s.getDate());
                String timeColor = getHeureColorDocx(s.getHeure());
                String salleColor = getSalleColorDocx(s.getSalle().getNum_salle());

                XWPFTableRow row = table.createRow();
                while (row.getTableCells().size() < headers.length) row.addNewTableCell();

                String nomEtu = group.get(0).getEtudiant().getNomE();
                String prenomEtu = group.get(0).getEtudiant().getPrenomE();
                boolean isBinome = group.size() > 1;
                if (isBinome) {
                    nomEtu += " & " + group.get(1).getEtudiant().getNomE();
                    prenomEtu += " & " + group.get(1).getEtudiant().getPrenomE();
                }

                // ID
                setCellDocx(row.getCell(0), String.valueOf(id++), C_EMPTY_DOCX, false, false, 8);
                // Encadrant
                setCellDocx(row.getCell(1), enc.getNom() + " " + enc.getPrenom(), encColor, true, true, 8);
                // Jury 1
                setCellDocx(row.getCell(2), m1.getNom() + " " + m1.getPrenom(), m1Color, true, false, 8);
                // Jury 2
                setCellDocx(row.getCell(3), m2.getNom() + " " + m2.getPrenom(), m2Color, true, false, 8);
                // Date
                setCellDocx(row.getCell(4), sdf.format(s.getDate()), dateColor, false, false, 8);
                // Heure
                setCellDocx(row.getCell(5), s.getHeure(), timeColor, false, true, 8);
                // Salle
                setCellDocx(row.getCell(6), s.getSalle().getNum_salle(), salleColor, false, false, 8);
                // Nom
                setCellDocx(row.getCell(7), nomEtu, filColor, false, isBinome, 8);
                // Prénom
                setCellDocx(row.getCell(8), prenomEtu, filColor, false, isBinome, 8);
                // Filière
                setCellDocx(row.getCell(9), filiere, filColor, false, false, 8);
            }

            doc.write(os);
        }
    }
}
