package controller;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.apache.xmlbeans.XmlCursor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@WebServlet("*.do")
@MultipartConfig(maxFileSize = 10485760) // 10MB
public class FrontController extends HttpServlet {

    // --------------------------------------------------------------------------
    //  CONFIGURATION
    // --------------------------------------------------------------------------

    private services.PfeService service = services.ServiceFactory.createPfeService();
    private static final String LOGO_FILE_NAME = "t1.png";
    private static final String LOGO_WEB_PATH = "/assets/" + LOGO_FILE_NAME;

    private static final DeviceRgb COLOR_HEADER                = new DeviceRgb(0.000f, 0.000f, 0.000f); // Black
    private static final DeviceRgb COLOR_HEADER_AFFECTATION    = new DeviceRgb(18, 52, 153);             // #123499
    //colors for Planning PDF
    private static final DeviceRgb COLOR_GI                    = new DeviceRgb(0.400f, 0.600f, 0.900f); // Blue
    private static final DeviceRgb COLOR_ID                    = new DeviceRgb(0.950f, 0.800f, 0.300f); // Yellow
    private static final DeviceRgb COLOR_TDIA                  = new DeviceRgb(0.450f, 0.750f, 0.450f); // Green
    //colors for Affectation PDF
    private static final DeviceRgb COLOR_GI_LIGHT              = new DeviceRgb(0.400f, 0.600f, 0.900f);           
    private static final DeviceRgb COLOR_ID_LIGHT              = new DeviceRgb(0.950f, 0.800f, 0.300f);           
    private static final DeviceRgb COLOR_TDIA_LIGHT            = new DeviceRgb(0.450f, 0.750f, 0.450f);               
    private static final DeviceRgb COLOR_EMPTY                 = new DeviceRgb(0.950f, 0.950f, 0.950f);
    // Couleurs DOCX
    private static final String C_HEADER_DOCX                  = "000000"; // Black
    private static final String C_HEADER_DOCX_AFFECTATION      = "123499"; // #123499
    private static final String C_GI_DOCX                      = "4F8AFF"; // Blue
    private static final String C_ID_DOCX                      = "FFC107"; // Yellow
    private static final String C_TDIA_DOCX                    = "689F38"; // Green
    private static final String C_GI_DOCX_LIGHT                = "B388FF"; // #b388ff
    private static final String C_ID_DOCX_LIGHT                = "FDA172"; // #fda172
    private static final String C_TDIA_DOCX_LIGHT              = "009B00"; // #009b00
    private static final String C_EMPTY_DOCX                   = "F0F0F0";
    private static final String C_WHITE_DOCX                   = "FFFFFF";

    // ═--------------------------------------------------------------------------
    //  LOGO
    // --------------------------------------------------------------------------

    private byte[] readLogoBytes() throws IOException {
        if (getServletContext() != null) {
            try (InputStream is = getServletContext().getResourceAsStream(LOGO_WEB_PATH)) {
                if (is != null) return is.readAllBytes();
            }
        }

        try (InputStream is = FrontController.class.getClassLoader().getResourceAsStream(LOGO_FILE_NAME)) {
            if (is != null) return is.readAllBytes();
        }

        String[] candidates = {
                LOGO_FILE_NAME,
                "projet/src/main/webapp/assets/" + LOGO_FILE_NAME,
                "src/main/webapp/assets/" + LOGO_FILE_NAME
        };
        for (String candidate : candidates) {
            java.io.File file = new java.io.File(candidate);
            if (file.exists() && file.isFile()) {
                return java.nio.file.Files.readAllBytes(file.toPath());
            }
        }
        return null;
    }

    private void addPdfLogo(com.itextpdf.layout.Document doc) throws IOException {
        byte[] logoBytes = readLogoBytes();
        if (logoBytes == null) return;

        ImageData imageData = ImageDataFactory.create(logoBytes);
        Image logo = new Image(imageData)
                .scaleToFit(70, 70)
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                .setMarginBottom(4);
        doc.add(logo);
    }

    private void addDocxLogo(XWPFDocument doc) throws IOException {
        byte[] logoBytes = readLogoBytes();
        if (logoBytes == null) return;

        XWPFParagraph paragraph = createTopParagraph(doc);
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        try (ByteArrayInputStream logoStream = new ByteArrayInputStream(logoBytes)) {
            run.addPicture(logoStream,
                    org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG,
                    LOGO_FILE_NAME,
                    Units.toEMU(70),
                    Units.toEMU(70));
        } catch (InvalidFormatException e) {
            throw new IOException("Impossible d'ajouter le logo ENSAH au document Word.", e);
        }
    }

    private XWPFParagraph createTopParagraph(XWPFDocument doc) {
        if (!doc.getParagraphs().isEmpty()) {
            XmlCursor cursor = doc.getParagraphArray(0).getCTP().newCursor();
            try {
                return doc.insertNewParagraph(cursor);
            } finally {
                cursor.dispose();
            }
        }
        return doc.createParagraph();
    }


    // --------------------------------------------------------------------------
    //  ROUTING
    // --------------------------------------------------------------------------

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
            case "/uploadData.do":
                doUploadData(req, resp);
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
            case "/addSalleBulk.do":
                doAddSalleBulk(req, resp);
                break;
            case "/deleteSalle.do":
                doDeleteSalle(req, resp);
                break;
            case "/deleteAllSalles.do":
                doDeleteAllSalles(req, resp);
                break;
            case "/recommendations.do":
                doRecommendations(req, resp);
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
            case "/planningJurySujetPdf.do":
                doPlanningJurySujetPdf(req, resp);
                break;
            case "/planningDocx.do":
                doPlanningDocx(req, resp);
                break;
            case "/pv.do":
                doPV(req, resp);
                break;
            case "/downloadPvDocx.do":
                doDownloadPvDocx(req, resp);
                break;
            case "/downloadPvZip.do":
                doDownloadPvZip(req, resp);
                break;
            case "/templateData.do":
                doTemplateData(req, resp);
                break;
            case "/templateEtudiants.do":
                doTemplateEtudiants(req, resp);
                break;
            case "/templateProfs.do":
                doTemplateProfs(req, resp);
                break;
            case "/config.do":
                doConfig(req, resp);
                break;
            default:
                req.getRequestDispatcher("index.jsp").forward(req, resp);
                break;
        }
    }

    private void doConfig(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean hasAffectations = service.getTotalEtudiantsAffectes(null) > 0;
        req.setAttribute("hasAffectations", hasAffectations);

        List<entities.Salle> salles = service.getAllSalles();
        req.setAttribute("salles", salles);

        List<Soutenance> soutenances = service.getAllSoutenances();
        req.setAttribute("soutenances", soutenances);

        services.PlanningConfig lastConfig = (services.PlanningConfig)
                req.getSession().getAttribute("lastPlanningConfig");
        if (lastConfig == null) lastConfig = services.PlanningConfig.defaults();
        req.setAttribute("planningConfig", lastConfig);
        req.setAttribute("constraints", lastConfig.getConstraints().asList());
        req.setAttribute("totalProjects", service.getTotalProjetsAffectes(null));
        req.setAttribute("totalProfs", service.getTotalProfesseurs());

        // Pass both salle CRUD flash AND planning failure flash attributes
        passFlashFromSession(req,
                "salleFlash", "salleFlashIsError",
                "planningDebug", "planningHardViolations", "planningSoftViolations",
                "planningUnscheduled", "planningSuggestions", "planningFailed");

        req.getRequestDispatcher("config.jsp").forward(req, resp);
    }

    // --------------------------------------------------------------------------
    //  DASHBOARD
    // --------------------------------------------------------------------------

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
            services.PlanningConfig lastConfig = (services.PlanningConfig) req.getSession().getAttribute("lastPlanningConfig");
            services.ConstraintSet constraints = lastConfig != null ? lastConfig.getConstraints() : null;
            services.VerificationReport verificationReport = service.verifierFichiersGeneres(lastFilieres, constraints);
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

    // --------------------------------------------------------------------------
    //  AFFECTATION (Upload, Lancer, Supprimer, Restaurer, Templates)
    // --------------------------------------------------------------------------

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
        
        java.io.File historyDir = new java.io.File(getHistoryFolder());
        List<String> historyTimestamps = new ArrayList<>();
        if (historyDir.exists() && historyDir.isDirectory()) {
            java.io.File[] files = historyDir.listFiles();
            if (files != null) {
                java.util.Set<String> tsSet = new java.util.HashSet<>();
                for (java.io.File f : files) {
                    if (f.isFile() && f.getName().startsWith("Affectation_")) {
                        //Affectation_YYYY-MM-DD_HH-mm-ss.ext
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
            debug.add("Fichier reçu: " + fileName + " → Filière: " + filiere);

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
            service.saveProfesseurs(list);
            
            debug.add(list.size() + " professeurs traités avec succès. Les affectations existantes sont préservées.");

        } catch (Exception e) {
            debug.add("Erreur: " + e.getMessage());
            e.printStackTrace();
        }

        req.setAttribute("debug", debug);
        req.setAttribute("message", "Profs importés ");
        req.setAttribute("fichiers", service.getAllFichiers());
        resp.sendRedirect("affectation.do");
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
            debug.add(" Sélectionnez au moins une filière");
            req.setAttribute("fichiers", service.getAllFichiers());
            req.setAttribute("debug", debug);
            req.getRequestDispatcher("affectation.jsp").forward(req, resp);
            return;
        }

        List<String> filieres = Arrays.asList(selected);
        debug.add("Filières sélectionnées: " + String.join(", ", filieres));

        service.lancerAffectationGlobale(filieres, debug);
        
        req.getSession().setAttribute("lastFilieres", filieres);
        req.getSession().setAttribute("affectationDone", true);
        req.getSession().setAttribute("affectationDebug", debug);
        
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
        
        resp.sendRedirect("affectation.do");
    }



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

        addPdfLogo(doc);

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

            list.sort((e1, e2) -> {
                String cne1 = e1.getCne() != null ? e1.getCne() : "";
                String bin1 = e1.getBinome_cne() != null ? e1.getBinome_cne() : "";
                String b1 = e1.hasBinome() ? (cne1.compareTo(bin1) < 0 ? cne1 + "|" + bin1 : bin1 + "|" + cne1) : cne1;
                
                String cne2 = e2.getCne() != null ? e2.getCne() : "";
                String bin2 = e2.getBinome_cne() != null ? e2.getBinome_cne() : "";
                String b2 = e2.hasBinome() ? (cne2.compareTo(bin2) < 0 ? cne2 + "|" + bin2 : bin2 + "|" + cne2) : cne2;
                
                return b1.compareTo(b2);
            });

            table.addCell(profCell(prof.getNom(),    bold, COLOR_HEADER_AFFECTATION));
            table.addCell(profCell(prof.getPrenom(), bold, COLOR_HEADER_AFFECTATION));

            for (int i = 0; i < maxStudents; i++) {
                if (i < list.size()) {
                    Etudiant e = list.get(i);
                    DeviceRgb color = filiereColorPdfAffectation(e.getFiliere());
                    boolean isBinome = e.hasBinome();
                    table.addCell(etuCell(e.getNomE(),    normal, color, isBinome));
                    table.addCell(etuCell(e.getPrenomE(), normal, color, isBinome));
                } else {
                    table.addCell(etuCell("", normal, COLOR_EMPTY, false));
                    table.addCell(etuCell("", normal, COLOR_EMPTY, false));
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

    private Cell subHeaderCell(String text, PdfFont font, DeviceRgb bgColor) {
        return new Cell()
                .add(new Paragraph(text).setFont(font)
                        .setFontColor(ColorConstants.WHITE).setFontSize(8))
                .setBackgroundColor(bgColor)
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

    private Cell etuCell(String text, PdfFont font, DeviceRgb bg, boolean isBinome) {
        Cell c = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(8))
                .setBackgroundColor(bg)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(3);
        if (isBinome) {
            c.setBorder(new com.itextpdf.layout.borders.SolidBorder(com.itextpdf.kernel.colors.ColorConstants.BLACK, 1.5f));
        }
        return c;
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

    // --------------------------------------------------------------------------
    //  EXPORT AFFECTATION (docx)
    // --------------------------------------------------------------------------

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

            addDocxLogo(doc);

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

                list.sort((e1, e2) -> {
                    String cne1 = e1.getCne() != null ? e1.getCne() : "";
                    String bin1 = e1.getBinome_cne() != null ? e1.getBinome_cne() : "";
                    String b1 = e1.hasBinome() ? (cne1.compareTo(bin1) < 0 ? cne1 + "|" + bin1 : bin1 + "|" + cne1) : cne1;
                    
                    String cne2 = e2.getCne() != null ? e2.getCne() : "";
                    String bin2 = e2.getBinome_cne() != null ? e2.getBinome_cne() : "";
                    String b2 = e2.hasBinome() ? (cne2.compareTo(bin2) < 0 ? cne2 + "|" + bin2 : bin2 + "|" + cne2) : cne2;
                    
                    return b1.compareTo(b2);
                });

                XWPFTableRow row = table.createRow();
                while (row.getTableCells().size() < 2 + maxStudents * 2) row.addNewTableCell();

                setCellDocx(row.getCell(0), prof.getNom(),    C_HEADER_DOCX_AFFECTATION, true, true,  9);
                setCellDocx(row.getCell(1), prof.getPrenom(), C_HEADER_DOCX_AFFECTATION, true, true,  9);

                for (int i = 0; i < maxStudents; i++) {
                    String nom    = "";
                    String prenom = "";
                    String color  = C_EMPTY_DOCX;
                    boolean isBinome = false;

                    if (i < list.size()) {
                        Etudiant e = list.get(i);
                        nom    = e.getNomE();
                        prenom = e.getPrenomE();
                        color  = filiereColorDocxAffectation(e.getFiliere());
                        isBinome = e.hasBinome();
                    }

                    setCellDocxBinome(row.getCell(i * 2 + 2), nom,    color, false, false, 8, isBinome);
                    setCellDocxBinome(row.getCell(i * 2 + 3), prenom, color, false, false, 8, isBinome);
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

    private void setCellDocxBinome(XWPFTableCell cell, String text, String bgColor, boolean whiteText, boolean bold, int size, boolean isBinome) {
        setCellDocx(cell, text, bgColor, whiteText, bold, size);
        if (isBinome) {
            setCellBorderBold(cell);
        }
    }

    private void setCellBorderBold(XWPFTableCell cell) {
        CTTcPr tcPr = getTcPr(cell);
        CTTcBorders borders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
        CTBorder top = borders.isSetTop() ? borders.getTop() : borders.addNewTop();
        top.setVal(STBorder.SINGLE);
        top.setSz(java.math.BigInteger.valueOf(12));
        top.setColor("000000");
        CTBorder bottom = borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom();
        bottom.setVal(STBorder.SINGLE);
        bottom.setSz(java.math.BigInteger.valueOf(12));
        bottom.setColor("000000");
        CTBorder left = borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft();
        left.setVal(STBorder.SINGLE);
        left.setSz(java.math.BigInteger.valueOf(12));
        left.setColor("000000");
        CTBorder right = borders.isSetRight() ? borders.getRight() : borders.addNewRight();
        right.setVal(STBorder.SINGLE);
        right.setSz(java.math.BigInteger.valueOf(12));
        right.setColor("000000");
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

    // --------------------------------------------------------------------------
    //  PLANNING
    // --------------------------------------------------------------------------

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

        
        boolean hasAffectations = service.getTotalEtudiantsAffectes(null) > 0;
        req.setAttribute("hasAffectations", hasAffectations);

        List<entities.Salle> salles = service.getAllSalles();
        req.setAttribute("salles", salles);
        
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

        // Expose the planning configuration (last used or defaults) to the JSP
        services.PlanningConfig lastConfig = (services.PlanningConfig)
                req.getSession().getAttribute("lastPlanningConfig");
        if (lastConfig == null) lastConfig = services.PlanningConfig.defaults();
        req.setAttribute("planningConfig", lastConfig);
        req.setAttribute("constraints", lastConfig.getConstraints().asList());
        req.setAttribute("totalProjects", service.getTotalProjetsAffectes(null));
        req.setAttribute("totalProfs", service.getTotalProfesseurs());

        // Pull any flash messages set by salle CRUD or planning failure
        passFlashFromSession(req,
                "salleFlash", "salleFlashIsError",
                "planningDebug", "planningHardViolations", "planningSoftViolations",
                "planningUnscheduled", "planningSuggestions", "planningFailed");

        req.getRequestDispatcher("planning.jsp").forward(req, resp);
    }

    private void passFlashFromSession(HttpServletRequest req, String... attrs) {
        for (String attr : attrs) {
            Object v = req.getSession().getAttribute(attr);
            if (v != null) {
                req.setAttribute(attr, v);
                req.getSession().removeAttribute(attr);
            }
        }
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
            boolean added = service.addSalle(numSalle.trim());
            if (!added) {
                resp.sendRedirect("planning.do?salleExists=true");
                return;
            }
        }
        resp.sendRedirect("planning.do");
    }

    private void doLancerPlanning(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
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

        @SuppressWarnings("unchecked")
        List<String> lastFilieres = (List<String>) req.getSession().getAttribute("lastFilieres");

        services.PlanningConfig config = buildPlanningConfigFromRequest(req);
        services.PlanningResult planningResult = service.genererPlanning(lastFilieres, selectedSalles, config);
        debug.addAll(planningResult.getDebugLog());

        // Persist the constraints used so the planning page can re-display them
        req.getSession().setAttribute("lastPlanningConfig", config);

        if (!planningResult.isSuccess()) {
            // Redirect to the config page so the user can fix settings immediately
            req.getSession().setAttribute("planningDebug", debug);
            req.getSession().setAttribute("planningHardViolations", planningResult.getHardViolations());
            req.getSession().setAttribute("planningSoftViolations", planningResult.getSoftViolations());
            req.getSession().setAttribute("planningUnscheduled", planningResult.getUnscheduledProjects());
            req.getSession().setAttribute("planningSuggestions", planningResult.getSuggestions());
            req.getSession().setAttribute("planningFailed", true);
            resp.sendRedirect("config.do");  // <-- send to config, not planning
            return;
        }

        // Success path: refresh data, build legends, persist exports
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

        req.getSession().setAttribute("planningDebug", debug);
        req.getSession().setAttribute("planningSoftViolations", planningResult.getSoftViolations());
        req.getSession().removeAttribute("planningHardViolations");
        req.getSession().removeAttribute("planningUnscheduled");
        req.getSession().removeAttribute("planningSuggestions");
        req.getSession().removeAttribute("planningFailed");

        req.setAttribute("hasAffectations", true);
        req.setAttribute("planningDone", true);
        
        
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

    // --------------------------------------------------------------------------
    //  PLANNING EXPORT (pdf)
    // --------------------------------------------------------------------------
    private void doPlanningPdf(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=planning_soutenances.pdf");
        generatePdfToStream(resp.getOutputStream());
    }

    private void doPlanningJurySujetPdf(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=jury_sujet_soutenances.pdf");
        generateJurySujetPdfToStream(resp.getOutputStream());
    }

    private void generateJurySujetPdfToStream(java.io.OutputStream os) throws IOException {
        List<Soutenance> soutenances = service.getAllSoutenances();
        Map<Long, String> colorMap = service.getProfessorColors();

        PdfWriter writer = new PdfWriter(os);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.setDefaultPageSize(PageSize.A4.rotate());
        com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);
        doc.setMargins(20, 20, 20, 20);

        PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        addPdfLogo(doc);
        doc.add(new Paragraph("École Nationale des Sciences Appliquées – Al Hoceima")
                .setFont(bold).setFontSize(12)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("Département Mathématiques et Informatique")
                .setFont(normal).setFontSize(10)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("Jury + Sujet - Visualisation de l'affectation intelligente")
                .setFont(bold).setFontSize(11)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        doc.add(new Paragraph("Année Universitaire 2025/2026")
                .setFont(normal).setFontSize(9)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                .setMarginBottom(8));

        float[] cols = {3f, 13f, 13f, 13f, 16f, 28f, 18f};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();
        String[] headers = {"ID", "Encadrant", "Membre de jury 1", "Membre de jury 2",
                "Etudiant(s)", "Sujet stage", "Spécialités profs"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(COLOR_HEADER)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(3));
        }

        int id = 1;
        Map<String, List<Soutenance>> groups = new LinkedHashMap<>();
        for (Soutenance s : soutenances) {
            String key = s.getJury().getIdJury() + "_" + s.getDate() + "_" + s.getHeure() + "_" + s.getSalle().getId_salle();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        DeviceRgb subjectColor = new DeviceRgb(245, 247, 250);
        DeviceRgb specialityColor = new DeviceRgb(235, 248, 255);
        for (List<Soutenance> group : groups.values()) {
            Soutenance s = group.get(0);
            Professeur enc = s.getJury().getPresident();
            Professeur m1 = s.getJury().getRapporteur1();
            Professeur m2 = s.getJury().getRapporteur2();
            boolean isBinome = group.size() > 1;
            String filiere = s.getEtudiant() != null ? s.getEtudiant().getFiliere() : "";

            DeviceRgb encColor = hexToRgb(colorMap.getOrDefault(enc.getIdp(), "1A56DB"));
            DeviceRgb m1Color = hexToRgb(colorMap.getOrDefault(m1.getIdp(), "2ECC71"));
            DeviceRgb m2Color = hexToRgb(colorMap.getOrDefault(m2.getIdp(), "E67E22"));
            DeviceRgb filColor = filiereColorPdf(filiere);

            table.addCell(planCell(String.valueOf(id++), normal, 8, COLOR_EMPTY, false, isBinome));
            table.addCell(planCell(profName(enc), bold, 8, encColor, true, isBinome));
            table.addCell(planCell(profName(m1), normal, 8, m1Color, true, isBinome));
            table.addCell(planCell(profName(m2), normal, 8, m2Color, true, isBinome));
            table.addCell(planCell(studentNames(group), isBinome ? bold : normal, 8, filColor, false, isBinome));
            table.addCell(planCell(projectSubjects(group), normal, 8, subjectColor, false, isBinome));
            table.addCell(planCell(professorSpecialites(enc, m1, m2), normal, 8, specialityColor, false, isBinome));
        }

        doc.add(table);
        doc.close();
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
        addPdfLogo(doc);
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

        //ID | Encadrant | Jury1 | Jury2 | Date | Heure | Salle | Nom | Prénom | Filière
        float[] cols = {3f, 12f, 12f, 12f, 8f, 6f, 6f, 9f, 9f, 5f};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

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

    // --------------------------------------------------------------------------
    //  PLANNING EXPORT (docx)
    // --------------------------------------------------------------------------
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

            addDocxLogo(doc);
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

    // ══════════════════════════════════════════════════════════════════════════
    //  PV
    // ══════════════════════════════════════════════════════════════════════════

    private void doPV(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Soutenance> soutenances = service.getAllSoutenances();
        List<List<Soutenance>> pvGroups = groupSoutenancesForPv(soutenances);
        List<Pv> pvItems = buildPvItems(pvGroups);
        Map<String, Map<String, Object>> professorGroups = new LinkedHashMap<>();

        for (Pv item : pvItems) {
            Map<String, Object> group = professorGroups.get(item.getProfessorId());
            if (group == null) {
                group = new LinkedHashMap<>();
                group.put("professorId", item.getProfessorId());
                group.put("professorName", item.getProfessorName());
                group.put("pvs", new ArrayList<Pv>());
                professorGroups.put(item.getProfessorId(), group);
            }
            @SuppressWarnings("unchecked")
            List<Pv> pvs = (List<Pv>) group.get("pvs");
            pvs.add(item);
        }

        List<Map<String, Object>> sortedProfGroups = new ArrayList<>(professorGroups.values());
        sortedProfGroups.sort((g1, g2) -> {
            String name1 = (String) g1.get("professorName");
            String name2 = (String) g2.get("professorName");
            if (name1 == null) return -1;
            if (name2 == null) return 1;
            return name1.compareToIgnoreCase(name2);
        });

        String selectedProfessorId = req.getParameter("profId");
        if ((selectedProfessorId == null || selectedProfessorId.trim().isEmpty()) && !sortedProfGroups.isEmpty()) {
            selectedProfessorId = (String) sortedProfGroups.get(0).get("professorId");
        }

        req.setAttribute("soutenances", soutenances);
        req.setAttribute("totalPVs", pvItems.size());
        req.setAttribute("pvItems", pvItems);
        req.setAttribute("professorGroups", sortedProfGroups);
        req.setAttribute("selectedProfessorId", selectedProfessorId);
        req.setAttribute("selectedProfessorGroup", selectedProfessorId != null ? professorGroups.get(selectedProfessorId) : null);
        req.getRequestDispatcher("pv.jsp").forward(req, resp);
    }

    private void doDownloadPvDocx(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String pvId = req.getParameter("pvId");
        List<Soutenance> group = findPvGroupById(pvId);
        if (group == null || group.isEmpty()) {
            resp.sendRedirect("pv.do");
            return;
        }

        Pv item = buildPvItem(group);
        resp.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + encodeDownloadFileName(item.getFileName()) + "\"");
        generatePvDocx(resp.getOutputStream(), group);
    }

    private void doDownloadPvZip(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String professorId = req.getParameter("profId");
        List<List<Soutenance>> groups = groupSoutenancesForPv(service.getAllSoutenances());
        if (professorId != null && !professorId.trim().isEmpty()) {
            List<List<Soutenance>> filtered = new ArrayList<>();
            for (List<Soutenance> group : groups) {
                Soutenance first = group.get(0);
                if (first.getJury() != null
                        && first.getJury().getPresident() != null
                        && professorId.equals(String.valueOf(first.getJury().getPresident().getIdp()))) {
                    filtered.add(group);
                }
            }
            groups = filtered;
        }

        if (groups.isEmpty()) {
            resp.sendRedirect("pv.do");
            return;
        }

        String zipName = professorId == null || professorId.trim().isEmpty()
                ? "PVs_Soutenances.zip"
                : "PVs_Professeur_" + professorId + ".zip";
        resp.setContentType("application/zip");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + encodeDownloadFileName(zipName) + "\"");

        Set<String> usedNames = new HashSet<>();
        Set<String> createdFolders = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(resp.getOutputStream())) {
            for (List<Soutenance> group : groups) {
                Pv item = buildPvItem(group);
                String folderName = pvProfessorFolderName(group) + "/";
                if (createdFolders.add(folderName)) {
                    zip.putNextEntry(new ZipEntry(folderName));
                    zip.closeEntry();
                }

                String entryName = uniqueZipName(folderName + item.getFileName(), usedNames);
                zip.putNextEntry(new ZipEntry(entryName));
                ByteArrayOutputStream docx = new ByteArrayOutputStream();
                generatePvDocx(docx, group);
                zip.write(docx.toByteArray());
                zip.closeEntry();
            }
        }
    }

    private List<List<Soutenance>> groupSoutenancesForPv(List<Soutenance> soutenances) {
        Map<String, List<Soutenance>> groups = new LinkedHashMap<>();
        for (Soutenance s : soutenances) {
            if (s == null || s.getJury() == null || s.getDate() == null || s.getSalle() == null) continue;
            groups.computeIfAbsent(pvGroupKey(s), k -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(groups.values());
    }

    private List<Soutenance> findPvGroupById(String pvId) {
        if (pvId == null || pvId.trim().isEmpty()) return null;
        for (List<Soutenance> group : groupSoutenancesForPv(service.getAllSoutenances())) {
            if (!group.isEmpty() && pvId.equals(pvGroupKey(group.get(0)))) {
                return group;
            }
        }
        return null;
    }

    private String pvGroupKey(Soutenance s) {
        return s.getJury().getIdJury() + "_" + s.getDate().getTime() + "_" + s.getHeure() + "_" + s.getSalle().getId_salle();
    }

    private List<Pv> buildPvItems(List<List<Soutenance>> groups) {
        List<Pv> items = new ArrayList<>();
        for (List<Soutenance> group : groups) {
            if (!group.isEmpty()) items.add(buildPvItem(group));
        }
        return items;
    }

    private Pv buildPvItem(List<Soutenance> group) {
        Soutenance first = group.get(0);
        Professeur professor = first.getJury().getPresident();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        Pv item = new Pv();
        item.setId(pvGroupKey(first));
        item.setProfessorId(professor != null && professor.getIdp() != null ? String.valueOf(professor.getIdp()) : "0");
        item.setProfessorName(profName(professor));
        item.setStudentName(studentNames(group));
        item.setFiliere(first.getEtudiant() != null ? first.getEtudiant().getFiliere() : "");
        item.setDate(first.getDate() != null ? sdf.format(first.getDate()) : "");
        item.setHeure(first.getHeure());
        item.setSalle(first.getSalle() != null ? first.getSalle().getNum_salle() : "");
        item.setFileName(pvFileName(group));
        return item;
    }

    private void generatePvDocx(java.io.OutputStream os, List<Soutenance> group)
            throws IOException {
        try (InputStream template = FrontController.class.getClassLoader().getResourceAsStream("templates/template_pv.docx")) {
            if (template == null) {
                throw new IOException("Template PV introuvable: templates/template_pv.docx");
            }

            try (XWPFDocument doc = new XWPFDocument(template)) {
                Map<String, String> values = pvTemplateValues(group);
                replacePlaceholders(doc, values);
                doc.write(os);
            }
        }
    }

    private Map<String, String> pvTemplateValues(List<Soutenance> group) {
        Soutenance s = group.get(0);
        Etudiant e = s.getEtudiant();
        Professeur encadrant = s.getJury().getPresident();
        Professeur jury1 = s.getJury().getRapporteur1();
        Professeur jury2 = s.getJury().getRapporteur2();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        Map<String, String> values = new HashMap<>();
        String filiere = e != null ? safe(e.getFiliere()) : "";
        values.put("annee_univ", "2025/2026");
        values.put("nom_etudiant", studentNames(group));
        values.putAll(pvFiliereBoxes(filiere));
        values.put("intitule_rapport", e != null ? safe(e.getSujet_stage()) : "");
        values.put("nom_encadrant", profName(encadrant));
        values.put("nom_jury", profName(jury1));
        values.put("jury_role", "Examinateur\nPr.       " + profName(jury2)
                + "\tExaminateur");
        values.put("date_soutenance", s.getDate() != null ? sdf.format(s.getDate()) : "");
        values.put("signature1", profName(encadrant));
        values.put("signature2", profName(jury1));
        values.put("signature3", profName(jury2));
        return values;
    }

    private void replacePlaceholders(XWPFDocument doc, Map<String, String> values) {
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            replaceInParagraph(paragraph, values);
        }
        for (XWPFTable table : doc.getTables()) {
            replaceInTable(table, values);
        }
        for (org.apache.poi.xwpf.usermodel.XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph paragraph : header.getParagraphs()) {
                replaceInParagraph(paragraph, values);
            }
            for (XWPFTable table : header.getTables()) {
                replaceInTable(table, values);
            }
        }
    }

    private void replaceInTable(XWPFTable table, Map<String, String> values) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    replaceInParagraph(paragraph, values);
                }
                for (XWPFTable nested : cell.getTables()) {
                    replaceInTable(nested, values);
                }
            }
        }
    }

    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> values) {
        String text = paragraph.getText();
        if (text == null || !text.contains("${")) return;

        String replaced = replaceTemplateText(text, values);
        if (replaced.equals(text)) return;

        boolean universityYearLine = replaced.contains("Année Universitaire")
                || replaced.contains("Annee Universitaire");
        if (universityYearLine) {
            replaced = replaced.replace('\u00A0', ' ').trim();
            paragraph.setAlignment(ParagraphAlignment.CENTER);
        }
        boolean juryRoleLine = replaced.contains("Président")
                || replaced.contains("President")
                || replaced.contains("Examinateur");
        if (juryRoleLine) {
            replaced = alignJuryRoleText(replaced);
            setJuryRoleTabStop(paragraph);
        }

        int runs = paragraph.getRuns().size();
        for (int i = runs - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        XWPFRun run = paragraph.createRun();
        if (universityYearLine) {
            run.setUnderline(UnderlinePatterns.NONE);
        }
        String[] lines = replaced.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) run.addBreak();
            addRunTextWithTabs(run, lines[i]);
        }
    }

    private String alignJuryRoleText(String text) {
        return text.replaceAll("[ \\u00A0]{10,}(Président|President|Examinateur)", "\t$1");
    }

    private void setJuryRoleTabStop(XWPFParagraph paragraph) {
        CTPPr pPr = paragraph.getCTP().isSetPPr()
                ? paragraph.getCTP().getPPr()
                : paragraph.getCTP().addNewPPr();
        if (pPr.isSetTabs()) {
            pPr.unsetTabs();
        }
        CTTabStop tab = pPr.addNewTabs().addNewTab();
        tab.setVal(STTabJc.LEFT);
        tab.setPos(BigInteger.valueOf(6800));
    }

    private void addRunTextWithTabs(XWPFRun run, String line) {
        String[] parts = line.split("\\t", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                run.addTab();
            }
            if (!parts[i].isEmpty()) {
                run.setText(parts[i]);
            }
        }
    }

    private String replaceTemplateText(String text, Map<String, String> values) {
        String result = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replaceAll(placeholderRegex(entry.getKey()), java.util.regex.Matcher.quoteReplacement(entry.getValue()));
        }
        return result;
    }

    private String placeholderRegex(String key) {
        StringBuilder pattern = new StringBuilder("\\$\\{\\s*");
        for (int i = 0; i < key.length(); i++) {
            pattern.append(java.util.regex.Pattern.quote(String.valueOf(key.charAt(i)))).append("\\s*");
        }
        pattern.append("\\}");
        return pattern.toString();
    }

    private Map<String, String> pvFiliereBoxes(String filiere) {
        Map<String, String> boxes = new HashMap<>();
        boxes.put("box_id", "\u2610");
        boxes.put("box_gi", "\u2610");
        boxes.put("box_tdia", "\u2610");

        String normalized = safe(filiere).toUpperCase(Locale.ROOT);
        if (normalized.contains("TDIA") || normalized.contains("TRANSFORMATION")) {
            boxes.put("box_tdia", "\u2612");
        } else if (normalized.contains("GI") || normalized.contains("INFORMATIQUE")) {
            boxes.put("box_gi", "\u2612");
        } else if (normalized.contains("ID") || normalized.contains("DONN")) {
            boxes.put("box_id", "\u2612");
        }
        return boxes;
    }

    private String studentNames(List<Soutenance> group) {
        List<String> names = new ArrayList<>();
        for (Soutenance s : group) {
            Etudiant e = s.getEtudiant();
            if (e != null) names.add(safe(e.getNomE()) + " " + safe(e.getPrenomE()));
        }
        return String.join(" / ", names);
    }

    private String pvFileName(List<Soutenance> group) {
        Soutenance first = group.get(0);
        Etudiant etudiant = first.getEtudiant();
        String nom = etudiant != null ? safe(etudiant.getNomE()) : "";
        String prenom = etudiant != null ? safe(etudiant.getPrenomE()) : "";
        String student = sanitizeFilePart(nom) + "_" + sanitizeFilePart(prenom);
        return "Fiche_Evaluation_PFE_" + student + ".docx";
    }

    private String pvProfessorFolderName(List<Soutenance> group) {
        Soutenance first = group.get(0);
        String professor = first.getJury() != null
                ? profName(first.getJury().getPresident())
                : "";
        return sanitizeFilePart(professor);
    }

    private String normalizeName(String value) {
        String normalized = Normalizer.normalize(safe(value), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private String sanitizeFilePart(String value) {
        String cleaned = normalizeName(value).replaceAll("[^A-Za-z0-9_-]+", "_");
        cleaned = cleaned.replaceAll("_+", "_").replaceAll("^_|_$", "");
        return cleaned.isEmpty() ? "PV" : cleaned;
    }

    private String uniqueZipName(String fileName, Set<String> usedNames) {
        if (usedNames.add(fileName)) return fileName;
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        int i = 2;
        while (!usedNames.add(base + "_" + i + ext)) {
            i++;
        }
        return base + "_" + i + ext;
    }

    private String encodeDownloadFileName(String fileName) {
        try {
            return URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return fileName;
        }
    }

    // --------------------------------------------------------------------------
    //  HELPERS
    // --------------------------------------------------------------------------

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String projectSubjects(List<Soutenance> group) {
        Set<String> subjects = new LinkedHashSet<>();
        for (Soutenance soutenance : group) {
            if (soutenance.getEtudiant() != null) {
                String subject = safe(soutenance.getEtudiant().getSujet_stage());
                if (!subject.isEmpty()) subjects.add(subject);
            }
        }
        return subjects.isEmpty() ? "-" : String.join(" / ", subjects);
    }

    private String professorSpecialites(Professeur... professeurs) {
        Set<String> specialites = new LinkedHashSet<>();
        for (Professeur professeur : professeurs) {
            if (professeur == null) continue;
            String specialite = safe(professeur.getSpecialite());
            if (specialite.isEmpty()) {
                specialite = safe(professeur.getDiscipline());
            }
            if (!specialite.isEmpty()) specialites.add(specialite);
        }
        return specialites.isEmpty() ? "-" : String.join(" + ", specialites);
    }

    private String profName(Professeur professeur) {
        if (professeur == null) return "-";
        return professeur.getNom() + " " + professeur.getPrenom();
    }

    // --------------------------------------------------------------------------
    //  UNIFIED EXCEL IMPORT (single workbook, multiple sheets)
    // --------------------------------------------------------------------------

    private void doUploadData(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<String> debug = new ArrayList<>();
        Part file = req.getPart("file");
        if (file == null || file.getSize() == 0) {
            debug.add("Aucun fichier reçu.");
            req.setAttribute("debug", debug);
            req.setAttribute("fichiers", service.getAllFichiers());
            req.getRequestDispatcher("affectation.jsp").forward(req, resp);
            return;
        }

        String fileName = file.getSubmittedFileName();
        try {
            util.ExcelImporter.ImportResult result = service.importWorkbook(file.getInputStream(), fileName);
            debug.add("Fichier importé : " + fileName);
            debug.add("Étudiants : " + result.getStudentCount()
                    + " sur " + result.getStudentsByFiliere().size() + " filière(s) ("
                    + String.join(", ", result.getStudentsByFiliere().keySet()) + ")");
            debug.add("Professeurs : " + result.getProfesseurs().size());
            debug.add("Salles : " + result.getSalles().size());
            for (String w : result.getWarnings()) {
                debug.add(w);
            }
        } catch (Exception e) {
            debug.add("Erreur lors de l'import : " + e.getMessage());
            e.printStackTrace();
        }

        req.getSession().setAttribute("affectationDebug", debug);
        resp.sendRedirect("affectation.do");
    }

    private void doTemplateData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            String[] filieres = {"GI", "ID", "TDIA"};
            for (String filiere : filieres) {
                org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet(filiere);
                org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
                String[] cols = {"CNE", "NOM", "PRÉNOM", "EMAIL", "CNE BINÔME (Optionnel)", "SUJET PFE"};
                for (int i = 0; i < cols.length; i++) {
                    header.createCell(i).setCellValue(cols[i]);
                }
            }

            org.apache.poi.ss.usermodel.Sheet profSheet = wb.createSheet("Professeurs");
            org.apache.poi.ss.usermodel.Row profHeader = profSheet.createRow(0);
            String[] profCols = {"NOM", "PRÉNOM", "DISCIPLINE", "MODULE ENSEIGNÉ"};
            for (int i = 0; i < profCols.length; i++) {
                profHeader.createCell(i).setCellValue(profCols[i]);
            }

            org.apache.poi.ss.usermodel.Sheet salleSheet = wb.createSheet("Salles");
            org.apache.poi.ss.usermodel.Row salleHeader = salleSheet.createRow(0);
            String[] salleCols = {"NUM_SALLE", "BLOCK", "STATUS"};
            for (int i = 0; i < salleCols.length; i++) {
                salleHeader.createCell(i).setCellValue(salleCols[i]);
            }

            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setHeader("Content-Disposition", "attachment; filename=modele_gestion_pfe.xlsx");
            wb.write(resp.getOutputStream());
        }
    }

    // --------------------------------------------------------------------------
    //  SALLE MANAGEMENT (delete, bulk add, delete all)
    // --------------------------------------------------------------------------

    private void doAddSalleBulk(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String names = req.getParameter("salleNames");
        int added = service.addSalleBulk(names);
        req.getSession().setAttribute("salleFlash",
                added > 0 ? added + " salle(s) ajoutée(s)." : "Aucune nouvelle salle ajoutée.");
        resp.sendRedirect("planning.do");
    }

    private void doDeleteSalle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            resp.sendRedirect("planning.do");
            return;
        }
        try {
            Long id = Long.parseLong(idStr);
            if (service.isSalleUsedInPlanning(id)) {
                req.getSession().setAttribute("salleFlash",
                        "Cette salle est utilisée par un planning existant. Supprimez le planning d'abord.");
                req.getSession().setAttribute("salleFlashIsError", true);
            } else if (service.deleteSalle(id)) {
                req.getSession().setAttribute("salleFlash", "Salle supprimée.");
            } else {
                req.getSession().setAttribute("salleFlash", "Suppression impossible.");
                req.getSession().setAttribute("salleFlashIsError", true);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        resp.sendRedirect("planning.do");
    }

    private void doDeleteAllSalles(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int removed = service.deleteAllSalles();
        int remaining = service.getAllSalles().size();
        StringBuilder msg = new StringBuilder();
        msg.append(removed).append(" salle(s) supprimée(s).");
        if (remaining > 0) {
            msg.append(" ").append(remaining)
                    .append(" salle(s) conservée(s) car utilisée(s) dans un planning.");
            req.getSession().setAttribute("salleFlashIsError", true);
        }
        req.getSession().setAttribute("salleFlash", msg.toString());
        resp.sendRedirect("planning.do");
    }

    // --------------------------------------------------------------------------
    //  RECOMMENDATIONS (AJAX endpoint, returns JSON)
    // --------------------------------------------------------------------------

    private void doRecommendations(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> filieres = (List<String>) req.getSession().getAttribute("lastFilieres");
        services.PlanningConfig config = buildPlanningConfigFromRequest(req);
        int rooms = parseIntParam(req, "numberOfRooms", service.getAllSalles().size());
        java.util.List<services.Recommendation> recs = service.generateRecommendations(filieres, rooms, config);

        resp.setContentType("application/json; charset=UTF-8");
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"slotsPerDay\":").append(config.getSlotsPerDay()).append(",");
        sb.append("\"recommendations\":[");
        for (int i = 0; i < recs.size(); i++) {
            services.Recommendation r = recs.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
                    .append("\"type\":\"").append(jsonEscape(r.getType().name())).append("\",")
                    .append("\"bootstrap\":\"").append(jsonEscape(r.getBootstrapClass())).append("\",")
                    .append("\"icon\":\"").append(jsonEscape(r.getIconClass())).append("\",")
                    .append("\"title\":\"").append(jsonEscape(r.getTitle())).append("\",")
                    .append("\"message\":\"").append(jsonEscape(r.getMessage())).append("\",")
                    .append("\"suggestion\":\"").append(jsonEscape(r.getSuggestion())).append("\"")
                    .append("}");
        }
        sb.append("]}");
        resp.getWriter().write(sb.toString());
    }

    private static String jsonEscape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.toString();
    }

    // --------------------------------------------------------------------------
    //  PLANNING CONFIG BUILDER (from request parameters)
    // --------------------------------------------------------------------------

    private services.PlanningConfig buildPlanningConfigFromRequest(HttpServletRequest req) {
        services.PlanningConfig.Builder b = services.PlanningConfig.defaults().toBuilder();
        b.numberOfDays(parseIntParam(req, "numberOfDays", 4));
        b.startDate(req.getParameter("startDate"));
        b.startHourMorning(parseIntParam(req, "startHourMorning", 9));
        b.endHourMorning(parseIntParam(req, "endHourMorning", 12));
        b.startHourAfternoon(parseIntParam(req, "startHourAfternoon", 14));
        b.endHourAfternoon(parseIntParam(req, "endHourAfternoon", 18));
        b.soutenanceDurationMinutes(parseIntParam(req, "soutenanceDurationMinutes", 60));
        b.breakBetweenMinutes(parseIntParam(req, "breakBetweenMinutes", 0));

        services.ConstraintSet constraints = services.ConstraintSet.defaults();
        for (services.Constraint c : constraints.asList()) {
            String value = req.getParameter("constraint_value_" + c.getId());
            String priority = req.getParameter("constraint_priority_" + c.getId());
            services.ConstraintPriority p = priority == null
                    ? c.getPriority()
                    : services.ConstraintPriority.fromString(priority);
            constraints.update(c.getId(), value, p);
        }
        b.constraints(constraints);

        return b.build();
    }

    private int parseIntParam(HttpServletRequest req, String name, int defaultValue) {
        String v = req.getParameter(name);
        if (v == null || v.trim().isEmpty()) return defaultValue;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

}
