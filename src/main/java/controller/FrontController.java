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

import entities.Affectation;
import entities.Etudiant;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static final DeviceRgb COLOR_HEADER                = new DeviceRgb(0.000f, 0.000f, 0.000f); // Black
    private static final DeviceRgb COLOR_HEADER_AFFECTATION    = new DeviceRgb(47, 84, 150);             // #2F5496
    private static final DeviceRgb COLOR_NAMES_AFFECTATION     = new DeviceRgb(255, 255, 255);
    //colors for Affectation PDF
    private static final DeviceRgb COLOR_EMPTY                 = new DeviceRgb(0.950f, 0.950f, 0.950f);
    // Couleurs DOCX
    private static final String C_HEADER_DOCX                  = "000000"; // Black
    private static final String C_HEADER_DOCX_AFFECTATION      = "2F5496"; // #2F5496
    private static final String C_NAMES_DOCX_AFFECTATION       = "D9E1F2"; // #D9E1F2
    private static final String C_EMPTY_DOCX                   = "F0F0F0";
    private static final String C_WHITE_DOCX                   = "FFFFFF";

    // ═--------------------------------------------------------------------------
    //  LOGO
    // --------------------------------------------------------------------------

    private byte[] readLogoBytes() throws IOException {
        // 1) User-uploaded logo persisted in AppSettings (preferred)
        try {
            entities.AppSettings settings = services.AppSettingsService.getInstance().get();
            if (settings != null && settings.getLogoBytes() != null && settings.getLogoBytes().length > 0) {
                return settings.getLogoBytes();
            }
        } catch (Exception ignored) {
        }

        // 2) Fallback to a bundled placeholder if any (legacy /images/logo.png or /assets/t1.png)
        if (getServletContext() != null) {
            try (InputStream is = getServletContext().getResourceAsStream("/images/logo.png")) {
                if (is != null) return is.readAllBytes();
            }
            try (InputStream is = getServletContext().getResourceAsStream("/assets/t1.png")) {
                if (is != null) return is.readAllBytes();
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

    /**
     * Renders the establishment header (institution name, sub-title, document
     * title and academic year) in a PDF document. Skips empty values so users
     * who only configure a subset still get a clean layout.
     */
    private void addBrandingHeaderPdf(com.itextpdf.layout.Document doc, PdfFont bold, PdfFont normal,
                                      String explicitTitle,
                                      int instSize, int subtSize, int titleSize, int yearSize,
                                      int marginBottom) {
        entities.AppSettings brand = services.AppSettingsService.getInstance().get();
        String inst    = nullToEmpty(brand.getInstitutionName());
        String subt    = nullToEmpty(brand.getInstitutionSubtitle());
        String title   = explicitTitle;
        String year    = nullToEmpty(brand.getAcademicYear());

        if (!inst.isEmpty()) {
            doc.add(new Paragraph(inst).setFont(bold).setFontSize(instSize)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        }
        if (!subt.isEmpty()) {
            doc.add(new Paragraph(subt).setFont(normal).setFontSize(subtSize)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        }
        if (title != null && !title.isEmpty()) {
            doc.add(new Paragraph(title).setFont(bold).setFontSize(titleSize)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        }
        if (!year.isEmpty()) {
            doc.add(new Paragraph("Annee Universitaire " + year).setFont(normal).setFontSize(yearSize)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setMarginBottom(marginBottom));
        }
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
                    "logo.png",
                    Units.toEMU(70),
                    Units.toEMU(70));
        } catch (InvalidFormatException e) {
            throw new IOException("Impossible d'ajouter le logo au document Word.", e);
        }
    }

    /** DOCX equivalent of {@link #addBrandingHeaderPdf}. */
    private void addBrandingHeaderDocx(XWPFDocument doc, String explicitTitle,
                                       int instSize, int subtSize, int titleSize, int yearSize) {
        entities.AppSettings brand = services.AppSettingsService.getInstance().get();
        String inst    = nullToEmpty(brand.getInstitutionName());
        String subt    = nullToEmpty(brand.getInstitutionSubtitle());
        String title   = explicitTitle;
        String year    = nullToEmpty(brand.getAcademicYear());

        if (!inst.isEmpty()) center(doc, inst, instSize, true);
        if (!subt.isEmpty()) center(doc, subt, subtSize, false);
        if (title != null && !title.isEmpty()) center(doc, title, titleSize, true);
        if (!year.isEmpty()) center(doc, "Annee Universitaire " + year, yearSize, false);
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

        // Make branding available to every JSP without each handler having to remember.
        try {
            req.setAttribute("appSettings", services.AppSettingsService.getInstance().get());
        } catch (Exception ignored) {
            // Settings table may not exist yet on first boot; pages have safe defaults.
        }

        // Make the local audit actor and active session available to every JSP.
        try {
            entities.AppUser auditActor = AuditContext.currentActor(req);
            req.setAttribute("auditActor", auditActor);
            req.setAttribute("activeSession", services.SessionService.getInstance().getActive());
            req.setAttribute("currentVersion",
                    services.SessionService.getInstance().getCurrentVersion(
                            req.getAttribute("activeSession") instanceof entities.AcademicSession
                                    ? ((entities.AcademicSession) req.getAttribute("activeSession")).getId()
                                    : null));
        } catch (Exception ignored) {}

        // Promote any flash attributes set by sibling controllers (login, overrides, etc.)
        for (String name : new String[]{"flashOk", "flashError", "flashInfo"}) {
            javax.servlet.http.HttpSession session = req.getSession(false);
            if (session != null) {
                Object v = session.getAttribute(name);
                if (v != null) {
                    req.setAttribute(name, v);
                    session.removeAttribute(name);
                }
            }
        }

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
            case "/settings.do":
                doSettings(req, resp);
                break;
            case "/saveBranding.do":
                doSaveBranding(req, resp);
                break;
            case "/saveStorage.do":
                doSaveStorage(req, resp);
                break;
            case "/testStorage.do":
                doTestStorage(req, resp);
                break;
            case "/saveNlp.do":
                doSaveNlp(req, resp);
                break;
            case "/logo.do":
                doLogo(req, resp);
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
        if (lastConfig == null) {
            // Load persisted planning configuration so user choices survive restarts.
            lastConfig = services.AppSettingsService.getInstance().loadPlanningConfig();
            req.getSession().setAttribute("lastPlanningConfig", lastConfig);
        }
        req.setAttribute("planningConfig", lastConfig);
        req.setAttribute("constraints", lastConfig.getConstraints().asList());
        req.setAttribute("totalProjects", service.getTotalProjetsAffectes(null));
        req.setAttribute("totalProfs", service.getTotalProfesseurs());

        // Pass both salle CRUD flash AND planning failure flash attributes
        passFlashFromSession(req,
                "salleFlash", "salleFlashIsError",
                "planningDebug", "planningHardViolations", "planningSoftViolations",
                "planningUnscheduled", "planningSuggestions", "planningFailed",
                "planningRecommendedValues");

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

        util.HistoryStorage storage = util.HistoryStorage.getInstance();
        for (String name : storage.listFilesWithPrefix(prefix)) {
            storage.delete(name);
        }
        String referer = req.getHeader("referer");
        if (referer != null) resp.sendRedirect(referer);
        else resp.sendRedirect("index.jsp");
    }

    private void doAffectation(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("fichiers", service.getAllFichiers());

        util.HistoryStorage storage = util.HistoryStorage.getInstance();
        java.util.Set<String> tsSet = new java.util.HashSet<>();
        for (String name : storage.listFilesWithPrefix("Affectation_")) {
            // Affectation_YYYY-MM-DD_HH-mm-ss.ext
            int extIndex = name.lastIndexOf('.');
            if (extIndex > 12) {
                tsSet.add(name.substring(12, extIndex));
            }
        }
        List<String> historyTimestamps = new ArrayList<>(tsSet);
        java.util.Collections.sort(historyTimestamps, java.util.Collections.reverseOrder());
        req.setAttribute("historyTimestamps", historyTimestamps);

        // Operational extensions: list of all affectations (for lifecycle/force ops),
        // and the available professor pool for force-affectation modal.
        try {
            req.setAttribute("allAffectations", service.getAllAffectationsWithDetails());
            req.setAttribute("allProfs", new dao.ProfesseurDAOImpl().findAll());
            req.setAttribute("lifecycleStates", entities.LifecycleState.values());
        } catch (Exception ignored) {}

        req.getRequestDispatcher("affectation.jsp").forward(req, resp);
    }
    
    private void doRestoreAffectation(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ts = req.getParameter("timestamp");
        if (ts != null && !ts.isEmpty()) {
            String fileName = "Affectation_" + ts + ".txt";
            util.HistoryStorage storage = util.HistoryStorage.getInstance();
            if (storage.exists(fileName)) {
                java.io.File tempTxt = null;
                try {
                    tempTxt = java.io.File.createTempFile("affectation_restore_", ".txt");
                    tempTxt.deleteOnExit();
                    byte[] data = storage.read(fileName);
                    java.nio.file.Files.write(tempTxt.toPath(), data);
                    service.restoreAffectation(tempTxt);
                    req.getSession().setAttribute("affectationDone", true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (tempTxt != null && tempTxt.exists()) tempTxt.delete();
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

        // Audit
        try {
            services.AuditService.getInstance().record(AuditContext.currentActor(req),
                    entities.AuditAction.AFFECTATION_CREATED, "Affectation", null,
                    "Affectation lancée pour " + String.join(", ", filieres));
        } catch (Exception ignored) {}
        
        req.getSession().setAttribute("lastFilieres", filieres);
        req.getSession().setAttribute("affectationDone", true);
        req.getSession().setAttribute("affectationDebug", debug);

        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        String pdfName  = "Affectation_" + timestamp + ".pdf";
        String docxName = "Affectation_" + timestamp + ".docx";
        String txtName  = "Affectation_" + timestamp + ".txt";

        util.HistoryStorage storage = util.HistoryStorage.getInstance();
        try {
            ByteArrayOutputStream pdfBuf = new ByteArrayOutputStream();
            generateAffectationPdfToStream(pdfBuf, filieres);
            storage.write(pdfName, pdfBuf.toByteArray(), util.HistoryStorage.guessContentType(pdfName));

            ByteArrayOutputStream docxBuf = new ByteArrayOutputStream();
            generateAffectationDocxToStream(docxBuf, filieres);
            storage.write(docxName, docxBuf.toByteArray(), util.HistoryStorage.guessContentType(docxName));

            ByteArrayOutputStream txtBuf = new ByteArrayOutputStream();
            try (java.io.PrintWriter txtOut = new java.io.PrintWriter(txtBuf, true, java.nio.charset.StandardCharsets.UTF_8)) {
                for (entities.Affectation a : service.getAllAffectationsWithDetails()) {
                    if (a.getEtudiant() != null && a.getEncadrant() != null) {
                        txtOut.println(a.getEtudiant().getIde() + "," + a.getEncadrant().getIdp());
                    }
                }
            }
            storage.write(txtName, txtBuf.toByteArray(), util.HistoryStorage.guessContentType(txtName));
        } catch (Exception e) {
            e.printStackTrace();
            debug.add("Avertissement: ecriture historique echouee: " + e.getMessage());
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

        entities.AppSettings brand = services.AppSettingsService.getInstance().get();
        String docTitle = brand.getDocumentTitleAffectation() != null && !brand.getDocumentTitleAffectation().isEmpty()
                ? brand.getDocumentTitleAffectation()
                : "Affectation des encadrants de Projet de Fin d'Etudes";
        
        addBrandingHeaderPdf(doc, bold, normal, docTitle, 13, 11, 11, 10, 10);

        // Build the legend dynamically from the filieres present in the affectations.
        java.util.LinkedHashSet<String> filieresPresent = new java.util.LinkedHashSet<>();
        for (Affectation a : affectations) {
            if (a.getEtudiant() != null && a.getEtudiant().getFiliere() != null) {
                filieresPresent.add(a.getEtudiant().getFiliere());
            }
        }
        if (!filieresPresent.isEmpty()) {
            float[] cols = new float[filieresPresent.size()];
            java.util.Arrays.fill(cols, 10f);
            Table legend = new Table(UnitValue.createPercentArray(cols))
                    .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                    .setMarginBottom(10);
            for (String fil : filieresPresent) {
                legend.addCell(legendCell("Filiere " + fil, filiereColorPdfAffectation(fil), normal));
            }
            doc.add(legend);
        }

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

            table.addCell(profCell(prof.getNom(),    bold, COLOR_NAMES_AFFECTATION));
            table.addCell(profCell(prof.getPrenom(), bold, COLOR_NAMES_AFFECTATION));

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
                        .setFontColor(ColorConstants.BLACK).setFontSize(9))
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
        if (filiere == null || filiere.isEmpty()) return COLOR_EMPTY;
        float[] rgb = util.FiliereColors.rgbFloat(filiere);
        return new DeviceRgb(rgb[0], rgb[1], rgb[2]);
    }

    private DeviceRgb filiereColorPdfAffectation(String filiere) {
        // Same palette as the planning PDF — gives a consistent visual identity.
        return filiereColorPdf(filiere);
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

            entities.AppSettings brand = services.AppSettingsService.getInstance().get();
            String docTitle = brand.getDocumentTitleAffectation() != null && !brand.getDocumentTitleAffectation().isEmpty()
                    ? brand.getDocumentTitleAffectation()
                    : "Affectation des encadrants de Projet de Fin d'Etudes";
            
            addBrandingHeaderDocx(doc, docTitle, 14, 12, 11, 10);
            doc.createParagraph();

            // Build the legend dynamically from the filieres present in the affectations.
            java.util.LinkedHashSet<String> filieresPresent = new java.util.LinkedHashSet<>();
            for (Affectation a : affectations) {
                if (a.getEtudiant() != null && a.getEtudiant().getFiliere() != null) {
                    filieresPresent.add(a.getEtudiant().getFiliere());
                }
            }
            if (!filieresPresent.isEmpty()) {
                XWPFTable legend = doc.createTable(1, filieresPresent.size());
                setWidth(legend, 4000);
                int idx = 0;
                for (String fil : filieresPresent) {
                    setLegendCellDocx(legend.getRow(0).getCell(idx++),
                            "Filiere " + fil, filiereColorDocxAffectation(fil));
                }
                doc.createParagraph();
            }

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

                setCellDocx(row.getCell(0), prof.getNom(),    C_NAMES_DOCX_AFFECTATION, false, true,  9);
                setCellDocx(row.getCell(1), prof.getPrenom(), C_NAMES_DOCX_AFFECTATION, false, true,  9);

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
        if (filiere == null || filiere.isEmpty()) return C_EMPTY_DOCX;
        return util.FiliereColors.hex(filiere);
    }

    private String filiereColorDocxAffectation(String filiere) {
        return filiereColorDocx(filiere);
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
            Jury j = s.getJury();
            if (j.getPresident() != null) profLegend.put(j.getPresident().getNom() + " " + j.getPresident().getPrenom(), colors.get(j.getPresident().getIdp()));
            if (j.getRapporteur1() != null) profLegend.put(j.getRapporteur1().getNom() + " " + j.getRapporteur1().getPrenom(), colors.get(j.getRapporteur1().getIdp()));
            if (j.getRapporteur2() != null) profLegend.put(j.getRapporteur2().getNom() + " " + j.getRapporteur2().getPrenom(), colors.get(j.getRapporteur2().getIdp()));
            if (j.getInvite() != null) profLegend.put(j.getInvite().getNom() + " " + j.getInvite().getPrenom(), colors.get(j.getInvite().getIdp()));
            if (j.getExtraMembers() != null) for (Professeur xm : j.getExtraMembers()) { if (xm != null) profLegend.put(xm.getNom() + " " + xm.getPrenom(), colors.get(xm.getIdp())); }
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

        List<String> historyFiles = util.HistoryStorage.getInstance().listFilesWithPrefix("Planning_");
        // listFilesWithPrefix already returns files in reverse order, but
        // sort defensively in case the backend returned an unsorted list.
        Collections.sort(historyFiles, Collections.reverseOrder());
        req.setAttribute("historyFiles", historyFiles);

        // Expose the planning configuration (last used or defaults) to the JSP
        services.PlanningConfig lastConfig = (services.PlanningConfig)
                req.getSession().getAttribute("lastPlanningConfig");
        if (lastConfig == null) {
            // Load persisted planning configuration so user choices survive restarts.
            lastConfig = services.AppSettingsService.getInstance().loadPlanningConfig();
            req.getSession().setAttribute("lastPlanningConfig", lastConfig);
        }
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
    
    private void doDownloadHistory(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String filename = req.getParameter("file");
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")
                || (!filename.startsWith("Planning_") && !filename.startsWith("Affectation_"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        util.HistoryStorage storage = util.HistoryStorage.getInstance();
        if (!storage.exists(filename)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType(util.HistoryStorage.guessContentType(filename));
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try (java.io.InputStream is = storage.openInputStream(filename)) {
            is.transferTo(resp.getOutputStream());
        }
    }

    
    private void doAddSalle(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String numSalle = req.getParameter("numSalle");
        if (numSalle != null && !numSalle.trim().isEmpty()) {
            boolean added = service.addSalle(numSalle.trim());
            if (!added) {
                req.getSession().setAttribute("salleFlash",
                        "La salle '" + numSalle.trim() + "' existe déjà.");
                req.getSession().setAttribute("salleFlashIsError", true);
                resp.sendRedirect("config.do");
                return;
            }
            req.getSession().setAttribute("salleFlash",
                    "Salle '" + numSalle.trim() + "' ajoutée.");
        }
        resp.sendRedirect("config.do");
    }

    private void doLancerPlanning(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        if (service.getTotalEtudiantsAffectes(null) == 0) {
            // No affectations yet — send the user to the affectation page.
            resp.sendRedirect("affectation.do");
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
        // Persist into AppSettings so values survive a server restart.
        try { services.AppSettingsService.getInstance().savePlanningConfig(config); }
        catch (Exception ignored) {}

        // Audit
        try {
            entities.AppUser actor = AuditContext.currentActor(req);
            services.AuditService.getInstance().record(actor,
                    entities.AuditAction.SOUTENANCE_GENERATED, "Soutenance", null,
                    "Génération du planning : " + (planningResult.isSuccess() ? "succès" : "échec")
                            + " (" + planningResult.getTotalSoutenances() + " soutenance(s))");
        } catch (Exception ignored) {}

        if (!planningResult.isSuccess()) {
            // Redirect to the config page so the user can fix settings immediately
            req.getSession().setAttribute("planningDebug", debug);
            req.getSession().setAttribute("planningHardViolations", planningResult.getHardViolations());
            req.getSession().setAttribute("planningSoftViolations", planningResult.getSoftViolations());
            req.getSession().setAttribute("planningUnscheduled", planningResult.getUnscheduledProjects());
            req.getSession().setAttribute("planningSuggestions", planningResult.getSuggestions());
            req.getSession().setAttribute("planningFailed", true);

            // Compute specific recommended values so the config page can
            // highlight the exact fields that need changing.
            int totalProjects = service.getTotalProjetsAffectes(lastFilieres);
            int totalProfs = service.getTotalProfesseurs();
            int rooms = selectedSalles.isEmpty() ? service.getAllSalles().size() : selectedSalles.size();
            int slotsPerDay = config.getSlotsPerDay() > 0 ? config.getSlotsPerDay() : 8;
            int dailyCap = Math.max(1, rooms) * slotsPerDay;
            int recDaysRoom = dailyCap > 0 ? (int) Math.ceil(totalProjects / (double) dailyCap) : 4;
            int maxProfDay = config.getConstraints().getMaxSoutenancesPerProfPerDay();
            int profCap = Math.max(1, totalProfs) * Math.max(1, maxProfDay);
            int recDaysProf = profCap > 0 ? (int) Math.ceil((totalProjects * 3.0) / profCap) : 4;
            int recDays = Math.max(1, Math.max(recDaysRoom, recDaysProf));
            int recRooms = slotsPerDay > 0 ? (int) Math.ceil(totalProjects / (double) (slotsPerDay * recDays)) : 2;
            int recMaxProfDay = totalProfs > 0 ? (int) Math.ceil((totalProjects * 3.0) / (totalProfs * recDays)) : 4;
            int recMaxRoomDay = recRooms > 0 ? (int) Math.ceil(totalProjects / (double) (recRooms * recDays)) : 7;

            Map<String, String> recValues = new java.util.LinkedHashMap<>();
            recValues.put("numberOfDays", String.valueOf(recDays));
            recValues.put("numberOfRooms", String.valueOf(Math.max(recRooms, rooms)));
            recValues.put("MAX_SOUTENANCES_PER_PROF_PER_DAY", String.valueOf(Math.max(recMaxProfDay, maxProfDay)));
            recValues.put("MAX_SOUTENANCES_PER_ROOM_PER_DAY", String.valueOf(Math.max(recMaxRoomDay, config.getConstraints().getMaxSoutenancesPerRoomPerDay())));
            req.getSession().setAttribute("planningRecommendedValues", recValues);

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
            Jury j = s.getJury();
            if (j.getPresident() != null) profLegend.put(j.getPresident().getNom() + " " + j.getPresident().getPrenom(), colors.get(j.getPresident().getIdp()));
            if (j.getRapporteur1() != null) profLegend.put(j.getRapporteur1().getNom() + " " + j.getRapporteur1().getPrenom(), colors.get(j.getRapporteur1().getIdp()));
            if (j.getRapporteur2() != null) profLegend.put(j.getRapporteur2().getNom() + " " + j.getRapporteur2().getPrenom(), colors.get(j.getRapporteur2().getIdp()));
            if (j.getInvite() != null) profLegend.put(j.getInvite().getNom() + " " + j.getInvite().getPrenom(), colors.get(j.getInvite().getIdp()));
            if (j.getExtraMembers() != null) for (Professeur xm : j.getExtraMembers()) { if (xm != null) profLegend.put(xm.getNom() + " " + xm.getPrenom(), colors.get(xm.getIdp())); }
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

        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        String pdfName  = "Planning_" + timestamp + ".pdf";
        String docxName = "Planning_" + timestamp + ".docx";

        util.HistoryStorage storage = util.HistoryStorage.getInstance();
        try {
            ByteArrayOutputStream pdfBuf = new ByteArrayOutputStream();
            generatePdfToStream(pdfBuf);
            storage.write(pdfName, pdfBuf.toByteArray(), util.HistoryStorage.guessContentType(pdfName));

            ByteArrayOutputStream docxBuf = new ByteArrayOutputStream();
            generateDocxToStream(docxBuf);
            storage.write(docxName, docxBuf.toByteArray(), util.HistoryStorage.guessContentType(docxName));
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
        entities.AppSettings brand = services.AppSettingsService.getInstance().get();
        String title = brand.getDocumentTitleAffectation() != null && !brand.getDocumentTitleAffectation().isEmpty()
                ? brand.getDocumentTitleAffectation() : "Jury + Sujet - Visualisation de l'affectation intelligente";
        addBrandingHeaderPdf(doc, bold, normal, title, 12, 10, 11, 9, 8);

        int id = 1;
        Map<String, List<Soutenance>> groups = new LinkedHashMap<>();
        for (Soutenance s : soutenances) {
            String key = s.getJury().getIdJury() + "_" + s.getDate() + "_" + s.getHeure() + "_" + s.getSalle().getId_salle();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        // Determine max jury size for dynamic columns
        int maxJuryMembers = 2;
        for (List<Soutenance> group : groups.values()) {
            Jury j = group.get(0).getJury();
            int count = 1;
            if (j.getRapporteur1() != null) count++;
            if (j.getRapporteur2() != null) count++;
            if (j.getInvite() != null) count++;
            if (j.getExtraMembers() != null) count += j.getExtraMembers().size();
            maxJuryMembers = Math.max(maxJuryMembers, count);
        }

        int totalCols = 1 + maxJuryMembers + 3; // ID + jury + Etudiant(s) + Sujet + Spécialités
        float[] cols = new float[totalCols];
        cols[0] = 3f;
        float juryW = 42f / maxJuryMembers;
        for (int ci = 1; ci <= maxJuryMembers; ci++) cols[ci] = juryW;
        cols[maxJuryMembers + 1] = 16f;
        cols[maxJuryMembers + 2] = 28f;
        cols[maxJuryMembers + 3] = 11f;
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        // Header
        table.addHeaderCell(new Cell().add(new Paragraph("ID").setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)).setBackgroundColor(COLOR_HEADER).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setPadding(3));
        table.addHeaderCell(new Cell().add(new Paragraph("Encadrant").setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)).setBackgroundColor(COLOR_HEADER).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setPadding(3));
        for (int ci = 2; ci <= maxJuryMembers; ci++) {
            table.addHeaderCell(new Cell().add(new Paragraph("Jury " + (ci-1)).setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)).setBackgroundColor(COLOR_HEADER).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setPadding(3));
        }
        for (String h : new String[]{"Etudiant(s)", "Sujet stage", "Spécialités profs"}) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)).setBackgroundColor(COLOR_HEADER).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE).setPadding(3));
        }

        DeviceRgb subjectColor = new DeviceRgb(245, 247, 250);
        DeviceRgb specialityColor = new DeviceRgb(235, 248, 255);
        for (List<Soutenance> group : groups.values()) {
            Soutenance s = group.get(0);
            boolean isBinome = group.size() > 1;

            // Collect all jury members
            List<Professeur> juryMembers = new ArrayList<>();
            juryMembers.add(s.getJury().getPresident());
            if (s.getJury().getRapporteur1() != null) juryMembers.add(s.getJury().getRapporteur1());
            if (s.getJury().getRapporteur2() != null) juryMembers.add(s.getJury().getRapporteur2());
            if (s.getJury().getInvite() != null) juryMembers.add(s.getJury().getInvite());
            if (s.getJury().getExtraMembers() != null) {
                for (Professeur xm : s.getJury().getExtraMembers()) { if (xm != null) juryMembers.add(xm); }
            }

            table.addCell(planCell(String.valueOf(id++), normal, 8, COLOR_EMPTY, false, isBinome));
            for (int ji = 0; ji < maxJuryMembers; ji++) {
                if (ji < juryMembers.size()) {
                    Professeur p = juryMembers.get(ji);
                    DeviceRgb pColor = hexToRgb(colorMap.getOrDefault(p.getIdp(), "1A56DB"));
                    table.addCell(planCell(profName(p), ji == 0 ? bold : normal, 8, pColor, true, isBinome));
                } else {
                    table.addCell(planCell("-", normal, 8, COLOR_EMPTY, false, isBinome));
                }
            }
            table.addCell(planCell(studentNames(group), isBinome ? bold : normal, 8, filiereColorPdf(s.getEtudiant() != null ? s.getEtudiant().getFiliere() : ""), false, isBinome));
            table.addCell(planCell(projectSubjects(group), normal, 8, subjectColor, false, isBinome));
            table.addCell(planCell(professorSpecialites(juryMembers.toArray(new Professeur[0])), normal, 8, specialityColor, false, isBinome));
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
        entities.AppSettings brand = services.AppSettingsService.getInstance().get();
        String title = brand.getDocumentTitlePlanning() != null && !brand.getDocumentTitlePlanning().isEmpty()
                ? brand.getDocumentTitlePlanning() : "Planning des soutenances des Projets de Fin d'Etudes";
        addBrandingHeaderPdf(doc, bold, normal, title, 12, 10, 10, 9, 8);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        int id = 1;
        Map<String, List<Soutenance>> groups = new LinkedHashMap<>();
        for (Soutenance s : soutenances) {
            String key = s.getJury().getIdJury() + "_" + s.getDate() + "_" + s.getHeure() + "_" + s.getSalle().getId_salle();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        //ID | Encadrant | Jury members (dynamic) | Date | Heure | Salle | Nom | Prénom | Filière
        // Determine max jury size across all soutenances
        int maxJuryMembers = 2; // minimum: encadrant + 1 rapporteur shown as 2 columns
        for (List<Soutenance> group : groups.values()) {
            Jury j = group.get(0).getJury();
            int count = 0;
            if (j.getRapporteur1() != null) count++;
            if (j.getRapporteur2() != null) count++;
            if (j.getInvite() != null) count++;
            if (j.getExtraMembers() != null) count += j.getExtraMembers().size();
            maxJuryMembers = Math.max(maxJuryMembers, count + 1); // +1 for encadrant
        }

        int fixedCols = 6; // ID, Date, Heure, Salle, Nom, Prénom, Filière → actually 7
        int totalCols = 1 + maxJuryMembers + 6; // ID + jury + Date+Heure+Salle+Nom+Prenom+Filiere
        float[] cols = new float[totalCols];
        cols[0] = 3f; // ID
        float juryColWidth = 48f / maxJuryMembers;
        for (int ci = 1; ci <= maxJuryMembers; ci++) cols[ci] = juryColWidth;
        cols[maxJuryMembers + 1] = 7f;  // Date
        cols[maxJuryMembers + 2] = 5f;  // Heure
        cols[maxJuryMembers + 3] = 5f;  // Salle
        cols[maxJuryMembers + 4] = 9f;  // Nom
        cols[maxJuryMembers + 5] = 9f;  // Prénom
        cols[maxJuryMembers + 6] = 5f;  // Filière
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        // Header row
        table.addHeaderCell(new Cell().add(new Paragraph("ID").setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)).setBackgroundColor(COLOR_HEADER).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(3));
        table.addHeaderCell(new Cell().add(new Paragraph("Encadrant").setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)).setBackgroundColor(COLOR_HEADER).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(3));
        for (int ci = 2; ci <= maxJuryMembers; ci++) {
            table.addHeaderCell(new Cell().add(new Paragraph("Jury " + (ci - 1)).setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)).setBackgroundColor(COLOR_HEADER).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(3));
        }
        String[] tailHeaders = {"Date","Heure","Salle","Nom d'étudiant","Prénom d'étudiant","Filière"};
        for (String h : tailHeaders) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)).setBackgroundColor(COLOR_HEADER).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER).setPadding(3));
        }

        for (List<Soutenance> group : groups.values()) {
            Soutenance s = group.get(0);
            Professeur enc = s.getJury().getPresident();
            String filiere  = s.getEtudiant().getFiliere();

            // Collect all jury members in order: encadrant, r1, r2, invite, extras
            List<Professeur> juryMembers = new ArrayList<>();
            juryMembers.add(enc);
            if (s.getJury().getRapporteur1() != null) juryMembers.add(s.getJury().getRapporteur1());
            if (s.getJury().getRapporteur2() != null) juryMembers.add(s.getJury().getRapporteur2());
            if (s.getJury().getInvite() != null) juryMembers.add(s.getJury().getInvite());
            if (s.getJury().getExtraMembers() != null) {
                for (Professeur xm : s.getJury().getExtraMembers()) { if (xm != null) juryMembers.add(xm); }
            }

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
            // Jury members (fill up to maxJuryMembers columns)
            for (int ji = 0; ji < maxJuryMembers; ji++) {
                if (ji < juryMembers.size()) {
                    Professeur p = juryMembers.get(ji);
                    DeviceRgb pColor = hexToRgb(colorMap.getOrDefault(p.getIdp(), "1A56DB"));
                    table.addCell(planCell(p.getNom() + " " + p.getPrenom(), ji == 0 ? bold : normal, 8, pColor, true, isBinome));
                } else {
                    table.addCell(planCell("-", normal, 8, COLOR_EMPTY, false, isBinome));
                }
            }
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
            entities.AppSettings brand = services.AppSettingsService.getInstance().get();
            String title = brand.getDocumentTitlePlanning() != null && !brand.getDocumentTitlePlanning().isEmpty()
                    ? brand.getDocumentTitlePlanning() : "Planning des soutenances des Projets de Fin d'Etudes";
            addBrandingHeaderDocx(doc, title, 14, 12, 11, 10);
            doc.createParagraph();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            int id = 1;
            Map<String, List<Soutenance>> groups = new LinkedHashMap<>();
            for (Soutenance s : soutenances) {
                String key = s.getJury().getIdJury() + "_" + s.getDate() + "_" + s.getHeure() + "_" + s.getSalle().getId_salle();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }

            // Dynamic columns: determine max jury size
            int maxJuryMembers = 2;
            for (List<Soutenance> group : groups.values()) {
                Jury j = group.get(0).getJury();
                int count = 1; // encadrant
                if (j.getRapporteur1() != null) count++;
                if (j.getRapporteur2() != null) count++;
                if (j.getInvite() != null) count++;
                if (j.getExtraMembers() != null) count += j.getExtraMembers().size();
                maxJuryMembers = Math.max(maxJuryMembers, count);
            }

            int totalCols = 1 + maxJuryMembers + 6; // ID + jury + Date+Heure+Salle+Nom+Prenom+Filiere
            List<String> headerList = new ArrayList<>();
            headerList.add("ID");
            headerList.add("Encadrant");
            for (int ci = 2; ci <= maxJuryMembers; ci++) headerList.add("Jury " + (ci - 1));
            headerList.add("Date"); headerList.add("Heure"); headerList.add("Salle");
            headerList.add("Nom d'étudiant"); headerList.add("Prénom d'étudiant"); headerList.add("Filière");
            String[] headers = headerList.toArray(new String[0]);

            XWPFTable table = doc.createTable();
            setWidth(table, 13000);

            XWPFTableRow hRow = table.getRow(0);
            while (hRow.getTableCells().size() < headers.length) hRow.addNewTableCell();
            for (int i = 0; i < headers.length; i++) {
                setCellDocx(hRow.getCell(i), headers[i], C_HEADER_DOCX, true, true, 8);
            }

            for (List<Soutenance> group : groups.values()) {
                Soutenance s = group.get(0);
                Professeur enc = s.getJury().getPresident();
                String filiere = s.getEtudiant().getFiliere();

                // Collect all jury members
                List<Professeur> juryMembers = new ArrayList<>();
                juryMembers.add(enc);
                if (s.getJury().getRapporteur1() != null) juryMembers.add(s.getJury().getRapporteur1());
                if (s.getJury().getRapporteur2() != null) juryMembers.add(s.getJury().getRapporteur2());
                if (s.getJury().getInvite() != null) juryMembers.add(s.getJury().getInvite());
                if (s.getJury().getExtraMembers() != null) {
                    for (Professeur xm : s.getJury().getExtraMembers()) { if (xm != null) juryMembers.add(xm); }
                }

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

                int col = 0;
                // ID
                setCellDocx(row.getCell(col++), String.valueOf(id++), C_EMPTY_DOCX, false, false, 8);
                // Jury members
                for (int ji = 0; ji < maxJuryMembers; ji++) {
                    if (ji < juryMembers.size()) {
                        Professeur p = juryMembers.get(ji);
                        String pColor = colorMap.getOrDefault(p.getIdp(), C_HEADER_DOCX);
                        setCellDocx(row.getCell(col++), p.getNom() + " " + p.getPrenom(), pColor, true, ji == 0, 8);
                    } else {
                        setCellDocx(row.getCell(col++), "-", C_EMPTY_DOCX, false, false, 8);
                    }
                }
                // Date
                setCellDocx(row.getCell(col++), sdf.format(s.getDate()), dateColor, false, false, 8);
                // Heure
                setCellDocx(row.getCell(col++), s.getHeure(), timeColor, false, true, 8);
                // Salle
                setCellDocx(row.getCell(col++), s.getSalle().getNum_salle(), salleColor, false, false, 8);
                // Nom
                setCellDocx(row.getCell(col++), nomEtu, filColor, false, isBinome, 8);
                // Prénom
                setCellDocx(row.getCell(col++), prenomEtu, filColor, false, isBinome, 8);
                // Filière
                setCellDocx(row.getCell(col++), filiere, filColor, false, false, 8);
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
        entities.AppSettings _brand = services.AppSettingsService.getInstance().get();
        values.put("annee_univ", _brand.getAcademicYear() == null ? "" : _brand.getAcademicYear());
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

        // Legacy boxes preserved for backward compatibility with the bundled
        // template_pv.docx. They will only be populated if the filière name
        // matches the historical naming.
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

        // Generic placeholders so any establishment can define its own
        // template_pv.docx with placeholders like ${box_<FILIERE>} and
        // ${filiere_label} without us shipping legacy code.
        if (!normalized.isEmpty()) {
            String safeKey = "box_" + normalized.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "_");
            boxes.put(safeKey, "\u2612");
        }
        boxes.put("filiere_label", normalized);
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
            // Example sheet names; the user can rename / add as many sheets
            // as they have filières — the sheet name IS the filière code.
            String[] filieres = {"FILIERE_1", "FILIERE_2"};
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
        resp.sendRedirect("config.do");
    }

    private void doDeleteSalle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            resp.sendRedirect("config.do");
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
        resp.sendRedirect("config.do");
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
        resp.sendRedirect("config.do");
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

        // Cache the in-flight config in the session so navigating away and back
        // re-displays the user's edits, and persist it for restart safety.
        req.getSession().setAttribute("lastPlanningConfig", config);
        try { services.AppSettingsService.getInstance().savePlanningConfig(config); }
        catch (Exception ignored) {}

        // ── Compute auto-fill recommended values for the input fields ─────
        // Use null filieres to count ALL projects (session may not have lastFilieres yet).
        int totalProjects = service.getTotalProjetsAffectes(null);
        int totalProfs = service.getTotalProfesseurs();
        int slotsPerDay = config.getSlotsPerDay();

        // Recommended days: use a reference of 8 slots/day when the current
        // config produces 0 slots (time range not yet configured).
        int refSlots = slotsPerDay > 0 ? slotsPerDay : 8;
        int effectiveRooms = Math.max(1, rooms);
        int dailyRoomCapacity = effectiveRooms * refSlots;
        int recommendedDaysRoom = totalProjects > 0 && dailyRoomCapacity > 0
                ? (int) Math.ceil(totalProjects / (double) dailyRoomCapacity) : 1;
        int maxPerProfPerDay = config.getConstraints().getMaxSoutenancesPerProfPerDay();
        int profCapacityPerDay = Math.max(1, totalProfs) * Math.max(1, maxPerProfPerDay);
        int recommendedDaysProf = totalProjects > 0
                ? (int) Math.ceil((totalProjects * 3.0) / profCapacityPerDay) : 1;
        int recommendedDays = Math.max(1, Math.max(recommendedDaysRoom, recommendedDaysProf));

        // Recommended duration: 60 min default; drop to 45 when jury load is high.
        int recommendedDuration = 60;
        if (totalProjects > 0 && totalProfs >= 3) {
            double avgJuryPerProf = (totalProjects * 3.0) / totalProfs;
            if (avgJuryPerProf > 8) recommendedDuration = 45;
        }

        resp.setContentType("application/json; charset=UTF-8");
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"slotsPerDay\":").append(slotsPerDay).append(",");
        sb.append("\"totalProjects\":").append(totalProjects).append(",");
        sb.append("\"totalProfs\":").append(totalProfs).append(",");
        sb.append("\"recommendedDays\":").append(recommendedDays).append(",");
        sb.append("\"recommendedDuration\":").append(recommendedDuration).append(",");
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
        b.morningEnabled(!"false".equals(req.getParameter("morningEnabled")));
        b.startHourMorning(parseIntParam(req, "startHourMorning", 9));
        b.endHourMorning(parseIntParam(req, "endHourMorning", 12));
        b.afternoonEnabled(!"false".equals(req.getParameter("afternoonEnabled")));
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

    // --------------------------------------------------------------------------
    //  SETTINGS (branding, storage, NLP)
    // --------------------------------------------------------------------------

    private void doSettings(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        entities.AppSettings settings = services.AppSettingsService.getInstance().get();
        req.setAttribute("appSettings", settings);

        util.HistoryStorage storage = util.HistoryStorage.getInstance();
        boolean ok = false;
        String status;
        try {
            ok = storage.testConnection();
            status = storage.describeStatus() + (ok ? " — accessible" : " — non accessible");
        } catch (Exception e) {
            status = storage.describeStatus() + " — erreur: " + e.getMessage();
        }
        req.setAttribute("storageStatus", status);
        req.setAttribute("storageStatusOk", ok);

        passFlashFromSession(req, "settingsFlash", "settingsFlashIsError");
        req.getRequestDispatcher("settings.jsp").forward(req, resp);
    }

    private void doSaveBranding(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        entities.AppSettings settings = services.AppSettingsService.getInstance().get();
        settings.setInstitutionName(trimToNull(req.getParameter("institutionName")));
        settings.setInstitutionSubtitle(trimToNull(req.getParameter("institutionSubtitle")));
        settings.setAcademicYear(trimToNull(req.getParameter("academicYear")));
        settings.setDocumentTitleAffectation(trimToNull(req.getParameter("documentTitleAffectation")));
        settings.setDocumentTitlePlanning(trimToNull(req.getParameter("documentTitlePlanning")));

        if ("true".equals(req.getParameter("removeLogo"))) {
            settings.setLogoBytes(null);
            settings.setLogoMimeType(null);
        }

        try {
            Part logoPart = req.getPart("logo");
            if (logoPart != null && logoPart.getSize() > 0) {
                if (logoPart.getSize() > 1024 * 1024) { // 1 MB limit
                    flash(req, "settingsFlash", "Le logo est trop volumineux (max 1 Mo).", true);
                    resp.sendRedirect("settings.do");
                    return;
                }
                String mime = logoPart.getContentType();
                if (mime == null || (!mime.startsWith("image/"))) {
                    flash(req, "settingsFlash", "Le logo doit etre une image (PNG ou JPEG).", true);
                    resp.sendRedirect("settings.do");
                    return;
                }
                try (InputStream is = logoPart.getInputStream()) {
                    settings.setLogoBytes(is.readAllBytes());
                    settings.setLogoMimeType(mime);
                }
            }
        } catch (Exception e) {
            flash(req, "settingsFlash", "Erreur lors de la lecture du logo: " + e.getMessage(), true);
            resp.sendRedirect("settings.do");
            return;
        }

        try {
            services.AppSettingsService.getInstance().update(settings);
            flash(req, "settingsFlash", "Identite de l'etablissement enregistree.", false);
        } catch (Exception e) {
            flash(req, "settingsFlash", "Erreur de base de données (image potentiellement trop lourde pour MySQL) : " + e.getMessage(), true);
        }
        resp.sendRedirect("settings.do");
    }

    private void doSaveStorage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        entities.AppSettings settings = services.AppSettingsService.getInstance().get();
        String mode = req.getParameter("storageMode");
        if ("S3".equalsIgnoreCase(mode)) {
            settings.setStorageMode(entities.AppSettings.StorageMode.S3);
        } else {
            settings.setStorageMode(entities.AppSettings.StorageMode.LOCAL);
        }

        settings.setLocalStoragePath(trimToNull(req.getParameter("localStoragePath")));
        settings.setS3Endpoint(trimToNull(req.getParameter("s3Endpoint")));
        settings.setS3Region(trimToNull(req.getParameter("s3Region")));
        settings.setS3Bucket(trimToNull(req.getParameter("s3Bucket")));
        settings.setS3AccessKey(trimToNull(req.getParameter("s3AccessKey")));

        // Keep existing secret if user submits empty string
        String secretInput = req.getParameter("s3SecretKey");
        if (secretInput != null && !secretInput.isEmpty()) {
            settings.setS3SecretKey(secretInput);
        }

        settings.setS3Prefix(trimToNull(req.getParameter("s3Prefix")));
        settings.setS3PathStyleAccess("true".equals(req.getParameter("s3PathStyleAccess")));

        services.AppSettingsService.getInstance().update(settings);

        boolean ok = util.HistoryStorage.getInstance().testConnection();
        if (ok) {
            flash(req, "settingsFlash", "Stockage enregistre. Backend: "
                    + util.HistoryStorage.getInstance().describeStatus(), false);
        } else {
            flash(req, "settingsFlash", "Stockage enregistre, mais la connexion a echoue. Verifiez la configuration.", true);
        }
        resp.sendRedirect("settings.do");
    }

    private void doTestStorage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Save first then test, so the user can iterate quickly
        doSaveStorage(req, resp);
    }

    private void doSaveNlp(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        entities.AppSettings settings = services.AppSettingsService.getInstance().get();
        settings.setNlpBaseUrl(trimToNull(req.getParameter("nlpBaseUrl")));
        settings.setNlpModel(trimToNull(req.getParameter("nlpModel")));

        String keyInput = req.getParameter("nlpApiKey");
        if (keyInput != null) {
            settings.setNlpApiKey(keyInput.trim().isEmpty() ? null : keyInput.trim());
        }

        boolean wantEnabled = "true".equals(req.getParameter("nlpEnabled"));
        boolean canEnable = wantEnabled
                && settings.getNlpApiKey() != null
                && !settings.getNlpApiKey().isEmpty();
        settings.setNlpEnabled(canEnable);

        services.AppSettingsService.getInstance().update(settings);

        if (wantEnabled && !canEnable) {
            flash(req, "settingsFlash", "Le service NLP necessite une cle API. Il reste desactive.", true);
        } else {
            flash(req, "settingsFlash", "Configuration NLP enregistree (" + (canEnable ? "active" : "desactive") + ").", false);
        }
        resp.sendRedirect("settings.do");
    }

    private void doLogo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        entities.AppSettings settings = services.AppSettingsService.getInstance().get();
        if (settings == null || settings.getLogoBytes() == null || settings.getLogoBytes().length == 0) {
            // Fall back to bundled placeholder if any, otherwise 404
            byte[] fallback = readBundledLogoBytes();
            if (fallback != null) {
                resp.setContentType("image/png");
                resp.setHeader("Cache-Control", "public, max-age=300");
                resp.getOutputStream().write(fallback);
                return;
            }
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String mime = settings.getLogoMimeType();
        resp.setContentType(mime == null || mime.isEmpty() ? "image/png" : mime);
        resp.setHeader("Cache-Control", "public, max-age=300");
        resp.getOutputStream().write(settings.getLogoBytes());
    }

    private void flash(HttpServletRequest req, String key, String message, boolean isError) {
        req.getSession().setAttribute(key, message);
        if (key.endsWith("Flash")) {
            req.getSession().setAttribute(key + "IsError", isError);
        }
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private byte[] readBundledLogoBytes() {
        // Optional fallback used only when the user has not uploaded a logo yet.
        try {
            if (getServletContext() != null) {
                try (InputStream is = getServletContext().getResourceAsStream("/images/logo.png")) {
                    if (is != null) return is.readAllBytes();
                }
                try (InputStream is = getServletContext().getResourceAsStream("/assets/t1.png")) {
                    if (is != null) return is.readAllBytes();
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

}
