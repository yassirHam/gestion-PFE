# Document Generation & File Downloads - Technical Guide

## Overview

This project generates and serves 3 types of documents:

| Type | Library | Format | Purpose |
|------|---------|--------|---------|
| Affectation PDF | iText 7 | `.pdf` | Table of professor-student assignments |
| Affectation DOCX | Apache POI | `.docx` | Same data in Word format |
| Planning PDF | iText 7 | `.pdf` | Full soutenance schedule with colors |
| PV DOCX | Apache POI (template) | `.docx` | Official evaluation form per defense |
| PV ZIP | Java ZipOutputStream | `.zip` | Batch download of all PVs |

## Dependencies (pom.xml)

```xml
<!-- Apache POI - DOCX manipulation and Excel import -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- iText 7 - PDF generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>kernel</artifactId>
    <version>7.2.5</version>
</dependency>
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>layout</artifactId>
    <version>7.2.5</version>
</dependency>
```

---


## 1. PDF Generation with iText 7

### 1.1 Basic Setup

Every PDF starts with this boilerplate:

```java
// Create writer -> document pipeline
PdfWriter writer = new PdfWriter(outputStream);
PdfDocument pdf = new PdfDocument(writer);
pdf.setDefaultPageSize(PageSize.A4.rotate());  // Landscape A4
com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);
doc.setMargins(20, 20, 20, 20);  // top, right, bottom, left

// Load fonts
PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
```

### 1.2 Adding a Logo

```java
private void addPdfLogo(com.itextpdf.layout.Document doc) throws IOException {
    byte[] logoBytes = readLogoBytes();  // reads from assets/t1.png
    if (logoBytes == null) return;

    ImageData imageData = ImageDataFactory.create(logoBytes);
    Image logo = new Image(imageData)
            .scaleToFit(70, 70)
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
            .setMarginBottom(4);
    doc.add(logo);
}
```

### 1.3 Adding Text Paragraphs

```java
doc.add(new Paragraph("Ecole Nationale des Sciences Appliquees - Al Hoceima")
        .setFont(bold).setFontSize(12)
        .setTextAlignment(TextAlignment.CENTER));
doc.add(new Paragraph("Planning des soutenances")
        .setFont(normal).setFontSize(10)
        .setTextAlignment(TextAlignment.CENTER)
        .setMarginBottom(8));
```

### 1.4 Creating Tables

```java
// Define column widths as percentages
float[] cols = {3f, 12f, 12f, 12f, 8f, 6f, 6f, 9f, 9f, 5f};
Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();
```

### 1.5 Adding Header Cells

```java
String[] headers = {"ID", "Encadrant", "Jury 1", "Jury 2", "Date", "Heure", "Salle", "Nom", "Prenom", "Filiere"};
for (String h : headers) {
    table.addHeaderCell(new Cell()
            .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(COLOR_HEADER)  // DeviceRgb color
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(3));
}
```

### 1.6 Adding Data Cells with Colors

```java
private Cell planCell(String text, PdfFont font, int size, DeviceRgb bg, boolean white, boolean isBinome) {
    Cell c = new Cell()
            .add(new Paragraph(text == null ? "" : text).setFont(font).setFontSize(size))
            .setBackgroundColor(bg)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(2);
    if (white) c.setFontColor(ColorConstants.WHITE);
    if (isBinome) {
        c.setBorder(new SolidBorder(ColorConstants.BLACK, 1.5f));  // thick border for binomes
    }
    return c;
}

// Usage:
table.addCell(planCell(enc.getNom(), bold, 8, encColor, true, isBinome));
```

### 1.7 Color Management

```java
// Colors defined as constants
private static final DeviceRgb COLOR_HEADER = new DeviceRgb(26, 86, 219);  // blue
private static final DeviceRgb COLOR_GI = new DeviceRgb(46, 204, 113);     // green
private static final DeviceRgb COLOR_ID = new DeviceRgb(52, 152, 219);     // blue
private static final DeviceRgb COLOR_TDIA = new DeviceRgb(243, 156, 18);   // orange

// Professor colors come from a dynamic color map
DeviceRgb encColor = hexToRgb(colorMap.getOrDefault(enc.getIdp(), "1A56DB"));
```

### 1.8 Closing the Document

```java
doc.add(table);
doc.close();  // flushes to the outputStream
```

---


## 2. DOCX Generation with Apache POI

### 2.1 Creating a DOCX from Scratch (Affectation Export)

```java
private void generateAffectationDocxToStream(OutputStream os, List<String> filieres) throws IOException {
    try (XWPFDocument doc = new XWPFDocument()) {
        // Set landscape orientation
        CTDocument1 ctDoc = doc.getDocument();
        CTBody body = ctDoc.getBody();
        if (!body.isSetSectPr()) body.addNewSectPr();
        CTSectPr sect = body.getSectPr();
        CTPageSz pgSz = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
        pgSz.setW(BigInteger.valueOf(16838));  // A4 landscape width in twips
        pgSz.setH(BigInteger.valueOf(11906));  // A4 landscape height in twips
        pgSz.setOrient(STPageOrientation.LANDSCAPE);

        // ... add content ...
        doc.write(os);
    }
}
```

### 2.2 Adding a Logo to DOCX

```java
private void addDocxLogo(XWPFDocument doc) throws IOException {
    byte[] logoBytes = readLogoBytes();
    if (logoBytes == null) return;

    XWPFParagraph paragraph = doc.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = paragraph.createRun();
    try (ByteArrayInputStream logoStream = new ByteArrayInputStream(logoBytes)) {
        run.addPicture(logoStream,
                Document.PICTURE_TYPE_PNG,
                "t1.png",
                Units.toEMU(70),   // width in EMU
                Units.toEMU(70));  // height in EMU
    }
}
```

### 2.3 Centered Text

```java
private void center(XWPFDocument doc, String text, int size, boolean bold) {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    run.setText(text);
    run.setFontSize(size);
    run.setBold(bold);
}
```

### 2.4 Creating Tables in DOCX

```java
// Create table with 3 columns
XWPFTable legend = doc.createTable(1, 3);
setWidth(legend, 4000);  // width in twips (1 inch = 1440 twips)

// Set table width programmatically
private void setWidth(XWPFTable table, int widthTwips) {
    CTTbl tbl = table.getCTTbl();
    CTTblPr tblPr = tbl.getTblPr() != null ? tbl.getTblPr() : tbl.addNewTblPr();
    CTTblWidth w = tblPr.getTblW() != null ? tblPr.getTblW() : tblPr.addNewTblW();
    w.setW(BigInteger.valueOf(widthTwips));
    w.setType(STTblWidth.DXA);
}
```

### 2.5 Setting Cell Content and Background Color

```java
private void setCellDocx(XWPFTableCell cell, String text, String bgColor,
                         boolean whiteText, boolean bold, int size) {
    setCellBg(cell, bgColor);
    cell.removeParagraph(0);  // remove the default empty paragraph
    XWPFParagraph p = cell.addParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    run.setText(text);
    run.setBold(bold);
    run.setFontSize(size);
    if (whiteText) run.setColor("FFFFFF");
}

private void setCellBg(XWPFTableCell cell, String color) {
    CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
    shd.setFill(color);        // hex string like "2ECC71"
    shd.setVal(STShd.CLEAR);
}
```

### 2.6 Horizontal Cell Merge

```java
private void setCellMergeH(XWPFTableRow row, int from, int to, String text,
                           String bgColor, boolean whiteText, boolean bold, int size) {
    // First cell: RESTART merge
    XWPFTableCell first = row.getCell(from);
    CTTcPr tcPr0 = getTcPr(first);
    CTHMerge hm0 = tcPr0.isSetHMerge() ? tcPr0.getHMerge() : tcPr0.addNewHMerge();
    hm0.setVal(STMerge.RESTART);
    setCellDocx(first, text, bgColor, whiteText, bold, size);

    // Remaining cells: CONTINUE merge
    for (int i = from + 1; i <= to; i++) {
        XWPFTableCell c = row.getCell(i);
        CTTcPr tcPr = getTcPr(c);
        CTHMerge hm = tcPr.isSetHMerge() ? tcPr.getHMerge() : tcPr.addNewHMerge();
        hm.setVal(STMerge.CONTINUE);
        setCellBg(c, bgColor);
    }
}
```

---


## 3. PV Generation (Template-Based DOCX)

### 3.1 Architecture

The PV system uses a **Word template** with `${placeholder}` markers that are filled with real data at generation time. This is different from the affectation DOCX which is built from scratch.

```
templates/template_pv.docx   →   Replace ${placeholders}   →   Final PV document
       (in classpath)                                              (served to browser)
```

### 3.2 Loading the Template

```java
private void generatePvDocx(OutputStream os, List<Soutenance> group) throws IOException {
    try (InputStream template = FrontController.class.getClassLoader()
                                .getResourceAsStream("templates/template_pv.docx")) {
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
```

### 3.3 Template Values Map

```java
private Map<String, String> pvTemplateValues(List<Soutenance> group) {
    Soutenance s = group.get(0);
    Etudiant e = s.getEtudiant();
    Professeur encadrant = s.getJury().getPresident();
    Professeur jury1 = s.getJury().getRapporteur1();
    Professeur jury2 = s.getJury().getRapporteur2();

    Map<String, String> values = new HashMap<>();
    values.put("annee_univ", "2025/2026");
    values.put("nom_etudiant", studentNames(group));
    values.put("intitule_rapport", e.getSujet_stage());
    values.put("nom_encadrant", profName(encadrant));
    values.put("nom_jury", profName(jury1));
    values.put("date_soutenance", sdf.format(s.getDate()));
    values.put("signature1", profName(encadrant));
    values.put("signature2", profName(jury1));
    values.put("signature3", profName(jury2));

    // Checkbox characters for filiere selection
    values.putAll(pvFiliereBoxes(filiere));
    return values;
}
```

### 3.4 Filiere Checkboxes (Unicode)

```java
private Map<String, String> pvFiliereBoxes(String filiere) {
    Map<String, String> boxes = new HashMap<>();
    boxes.put("box_id", "\u2610");    // ☐ empty checkbox
    boxes.put("box_gi", "\u2610");
    boxes.put("box_tdia", "\u2610");

    String normalized = filiere.toUpperCase(Locale.ROOT);
    if (normalized.contains("TDIA")) {
        boxes.put("box_tdia", "\u2612");  // ☒ checked checkbox
    } else if (normalized.contains("GI")) {
        boxes.put("box_gi", "\u2612");
    } else if (normalized.contains("ID")) {
        boxes.put("box_id", "\u2612");
    }
    return boxes;
}
```

### 3.5 The Placeholder Replacement Engine

The engine handles the fact that Word splits text across multiple "runs" (formatting units). The placeholder `${nom_etudiant}` might be stored internally as `${` in one run and `nom_etudiant}` in another.

```java
private void replacePlaceholders(XWPFDocument doc, Map<String, String> values) {
    // Replace in body paragraphs
    for (XWPFParagraph paragraph : doc.getParagraphs()) {
        replaceInParagraph(paragraph, values);
    }
    // Replace in tables (cell by cell)
    for (XWPFTable table : doc.getTables()) {
        replaceInTable(table, values);
    }
    // Replace in headers
    for (XWPFHeader header : doc.getHeaderList()) {
        for (XWPFParagraph paragraph : header.getParagraphs()) {
            replaceInParagraph(paragraph, values);
        }
        for (XWPFTable table : header.getTables()) {
            replaceInTable(table, values);
        }
    }
}
```

### 3.6 Replacing in Tables (Recursive for Nested Tables)

```java
private void replaceInTable(XWPFTable table, Map<String, String> values) {
    for (XWPFTableRow row : table.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                replaceInParagraph(paragraph, values);
            }
            // Handle nested tables inside cells
            for (XWPFTable nested : cell.getTables()) {
                replaceInTable(nested, values);
            }
        }
    }
}
```

### 3.7 Replacing in a Single Paragraph

```java
private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> values) {
    String text = paragraph.getText();
    if (text == null || !text.contains("${")) return;

    String replaced = replaceTemplateText(text, values);
    if (replaced.equals(text)) return;

    // Special formatting for certain lines
    boolean juryRoleLine = replaced.contains("Examinateur") || replaced.contains("President");
    if (juryRoleLine) {
        replaced = alignJuryRoleText(replaced);
        setJuryRoleTabStop(paragraph);
    }

    // Clear all existing runs and recreate with new text
    int runs = paragraph.getRuns().size();
    for (int i = runs - 1; i >= 0; i--) {
        paragraph.removeRun(i);
    }
    XWPFRun run = paragraph.createRun();

    // Handle multi-line values (newline characters)
    String[] lines = replaced.split("\\n", -1);
    for (int i = 0; i < lines.length; i++) {
        if (i > 0) run.addBreak();
        addRunTextWithTabs(run, lines[i]);
    }
}
```

### 3.8 The Regex Pattern for Flexible Matching

Word often inserts invisible formatting between characters. This regex handles whitespace inserted by Word inside placeholders:

```java
private String placeholderRegex(String key) {
    // Builds: \$\{\s*n\s*o\s*m\s*_\s*e\s*t\s*u\s*d\s*i\s*a\s*n\s*t\s*\}
    StringBuilder pattern = new StringBuilder("\\$\\{\\s*");
    for (int i = 0; i < key.length(); i++) {
        pattern.append(Pattern.quote(String.valueOf(key.charAt(i)))).append("\\s*");
    }
    pattern.append("\\}");
    return pattern.toString();
}
```

### 3.9 Tab Stops for Jury Role Alignment

```java
private void setJuryRoleTabStop(XWPFParagraph paragraph) {
    CTPPr pPr = paragraph.getCTP().isSetPPr()
            ? paragraph.getCTP().getPPr()
            : paragraph.getCTP().addNewPPr();
    if (pPr.isSetTabs()) pPr.unsetTabs();
    CTTabStop tab = pPr.addNewTabs().addNewTab();
    tab.setVal(STTabJc.LEFT);
    tab.setPos(BigInteger.valueOf(6800));  // position in twips
}

private void addRunTextWithTabs(XWPFRun run, String line) {
    String[] parts = line.split("\\t", -1);
    for (int i = 0; i < parts.length; i++) {
        if (i > 0) run.addTab();
        if (!parts[i].isEmpty()) run.setText(parts[i]);
    }
}
```

---


## 4. File Downloads

### 4.1 Single File Download (PDF or DOCX)

The servlet sets HTTP headers to trigger a browser download:

```java
private void doExportPdf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("application/pdf");
    resp.setHeader("Content-Disposition", "attachment; filename=affectations.pdf");
    generateAffectationPdfToStream(resp.getOutputStream());
}

private void doExportDocx(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    resp.setHeader("Content-Disposition", "attachment; filename=affectations.docx");
    generateAffectationDocxToStream(resp.getOutputStream());
}
```

**Key headers:**
- `Content-Type`: tells the browser what type of file it is
- `Content-Disposition: attachment; filename="..."`: forces download instead of inline display

### 4.2 Single PV Download

```java
private void doDownloadPvDocx(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pvId = req.getParameter("pvId");
    List<Soutenance> group = findPvGroupById(pvId);
    if (group == null || group.isEmpty()) {
        resp.sendRedirect("pv.do");
        return;
    }

    Pv item = buildPvItem(group);
    resp.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    resp.setHeader("Content-Disposition",
        "attachment; filename=\"" + encodeDownloadFileName(item.getFileName()) + "\"");
    generatePvDocx(resp.getOutputStream(), group);
}
```

### 4.3 Filename Encoding for Special Characters

French names contain accents which must be URL-encoded for the `Content-Disposition` header:

```java
private String encodeDownloadFileName(String fileName) {
    try {
        return URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
        return fileName;
    }
}
```

### 4.4 Sanitizing File Names

```java
private String sanitizeFilePart(String value) {
    // Remove accents: e -> e, e -> e
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
    String cleaned = normalized.replaceAll("\\p{M}", "");
    // Keep only safe characters
    cleaned = cleaned.replaceAll("[^A-Za-z0-9_-]+", "_");
    cleaned = cleaned.replaceAll("_+", "_").replaceAll("^_|_$", "");
    return cleaned.isEmpty() ? "PV" : cleaned;
}

// Produces: "Fiche_Evaluation_PFE_EL_YAZID_Dari.docx"
private String pvFileName(List<Soutenance> group) {
    Soutenance first = group.get(0);
    Etudiant etudiant = first.getEtudiant();
    String student = sanitizeFilePart(etudiant.getNomE()) + "_" + sanitizeFilePart(etudiant.getPrenomE());
    return "Fiche_Evaluation_PFE_" + student + ".docx";
}
```

---

## 5. ZIP Download (Multiple Files)

### 5.1 ZIP with Folder Structure

Each professor gets their own folder inside the ZIP:

```
PVs_Soutenances.zip
├── CHERRADI_Mohamed/
│   ├── Fiche_Evaluation_PFE_Student1.docx
│   └── Fiche_Evaluation_PFE_Student2.docx
├── ABAKOUY_Redouan/
│   ├── Fiche_Evaluation_PFE_Student3.docx
│   └── ...
```

### 5.2 ZIP Generation Code

```java
private void doDownloadPvZip(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String professorId = req.getParameter("profId");
    List<List<Soutenance>> groups = groupSoutenancesForPv(service.getAllSoutenances());

    // Filter by professor if specified
    if (professorId != null && !professorId.trim().isEmpty()) {
        List<List<Soutenance>> filtered = new ArrayList<>();
        for (List<Soutenance> group : groups) {
            Soutenance first = group.get(0);
            if (first.getJury() != null && first.getJury().getPresident() != null
                    && professorId.equals(String.valueOf(first.getJury().getPresident().getIdp()))) {
                filtered.add(group);
            }
        }
        groups = filtered;
    }

    // Set response headers for ZIP
    String zipName = professorId == null ? "PVs_Soutenances.zip"
                                        : "PVs_Professeur_" + professorId + ".zip";
    resp.setContentType("application/zip");
    resp.setHeader("Content-Disposition", "attachment; filename=\"" + encodeDownloadFileName(zipName) + "\"");

    // Write ZIP directly to the response stream
    Set<String> usedNames = new HashSet<>();
    Set<String> createdFolders = new HashSet<>();
    try (ZipOutputStream zip = new ZipOutputStream(resp.getOutputStream())) {
        for (List<Soutenance> group : groups) {
            Pv item = buildPvItem(group);

            // Create professor folder entry
            String folderName = pvProfessorFolderName(group) + "/";
            if (createdFolders.add(folderName)) {
                zip.putNextEntry(new ZipEntry(folderName));
                zip.closeEntry();
            }

            // Generate the DOCX into memory, then write to ZIP
            String entryName = uniqueZipName(folderName + item.getFileName(), usedNames);
            zip.putNextEntry(new ZipEntry(entryName));
            ByteArrayOutputStream docx = new ByteArrayOutputStream();
            generatePvDocx(docx, group);
            zip.write(docx.toByteArray());
            zip.closeEntry();
        }
    }
}
```

### 5.3 Handling Duplicate File Names in ZIP

```java
private String uniqueZipName(String fileName, Set<String> usedNames) {
    if (usedNames.add(fileName)) return fileName;  // unique, use as-is

    // Append _2, _3, etc. until unique
    int dot = fileName.lastIndexOf('.');
    String base = dot > 0 ? fileName.substring(0, dot) : fileName;
    String ext = dot > 0 ? fileName.substring(dot) : "";
    int i = 2;
    while (!usedNames.add(base + "_" + i + ext)) {
        i++;
    }
    return base + "_" + i + ext;
}
```

---

## 6. Excel Import (Apache POI)

### 6.1 Reading Student Data from Excel

```java
public static List<Etudiant> importEtudiants(InputStream is, String filiere) {
    List<Etudiant> list = new ArrayList<>();
    try (Workbook wb = new XSSFWorkbook(is)) {
        Sheet sheet = wb.getSheetAt(0);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;  // skip header row

            Etudiant e = new Etudiant();
            e.setCne(getCellValue(row, 0));       // Column A
            e.setNomE(row.getCell(1).getStringCellValue());   // Column B
            e.setPrenomE(row.getCell(2).getStringCellValue()); // Column C
            e.setEmail(getCellValue(row, 3));     // Column D
            e.setBinome_cne(getCellValue(row, 4)); // Column E (optional)
            e.setSujet_stage(getCellValue(row, 5)); // Column F

            e.setFiliere(filiere);
            list.add(e);
        }
    }
    return list;
}
```

### 6.2 Reading Professor Data from Excel

```java
public static List<Professeur> importProfs(InputStream is) {
    List<Professeur> list = new ArrayList<>();
    try (Workbook wb = new XSSFWorkbook(is)) {
        Sheet sheet = wb.getSheetAt(0);
        for (Row row : sheet) {
            if (row.getRowNum() <= 1) continue;  // skip 2 header rows

            Professeur p = new Professeur();
            p.setNom(getCellValue(row, 0));        // Column A
            p.setPrenom(getCellValue(row, 1));      // Column B
            p.setDiscipline(getCellValue(row, 2));  // Column C
            p.setSpecialite(getCellValue(row, 3));  // Column D
            list.add(p);
        }
    }
    return list;
}
```

---

## 7. JSP Frontend (Download Buttons)

### 7.1 Single PV Download Button

```html
<a href="downloadPvDocx.do?pvId=${pv.id}" class="btn btn-sm btn-primary">
    <i class="fa-solid fa-download me-1"></i> DOCX
</a>
```

### 7.2 ZIP Download (All PVs)

```html
<a href="downloadPvZip.do" class="btn btn-success"
   onclick="return confirm('Voulez-vous telecharger tous les PVs au format ZIP ?')">
    <i class="fa-solid fa-file-zipper me-1"></i> Telecharger tous les PVs (ZIP)
</a>
```

### 7.3 ZIP Download (One Professor's PVs)

```html
<a href="downloadPvZip.do?profId=${selectedProfessorGroup.professorId}"
   class="btn btn-sm btn-outline-danger"
   onclick="return confirm('Voulez-vous telecharger les PVs de ce professeur ?')">
    <i class="fa-solid fa-file-zipper me-1"></i> ZIP de ce professeur
</a>
```

---

## 8. URL Routing Summary

| URL | Method | Output |
|-----|--------|--------|
| `pv.do` | `doPV()` | JSP page listing all PVs |
| `downloadPvDocx.do?pvId=X` | `doDownloadPvDocx()` | Single .docx download |
| `downloadPvZip.do` | `doDownloadPvZip()` | All PVs in .zip with folders |
| `downloadPvZip.do?profId=X` | `doDownloadPvZip()` | One professor's PVs in .zip |
| `exportPdf.do` | `doExportPdf()` | Affectation table as PDF |
| `exportDocx.do` | `doExportDocx()` | Affectation table as DOCX |
| `planningPdf.do` | `doPlanningPdf()` | Planning schedule as PDF |
| `planningJurySujetPdf.do` | `doPlanningJurySujetPdf()` | Jury+Subject view as PDF |

---

## 9. Data Flow Diagram

```
Browser Request
     │
     ▼
FrontController (*.do)
     │
     ├── pv.do ──────────► groupSoutenancesForPv() → buildPvItems() → pv.jsp
     │
     ├── downloadPvDocx.do → findPvGroupById() → generatePvDocx()
     │                              │
     │                              ├── Load template_pv.docx from classpath
     │                              ├── Build values map (pvTemplateValues)
     │                              ├── replacePlaceholders() on doc
     │                              └── doc.write(response.outputStream)
     │
     ├── downloadPvZip.do → for each group:
     │                         ├── generatePvDocx() → ByteArrayOutputStream
     │                         └── zip.putNextEntry() + zip.write(bytes)
     │
     ├── exportPdf.do ──────► generateAffectationPdfToStream()
     │                              ├── new PdfDocument + PdfWriter
     │                              ├── Add logo, headers, legend
     │                              ├── Build Table with professor rows
     │                              └── doc.close()
     │
     └── exportDocx.do ─────► generateAffectationDocxToStream()
                                    ├── new XWPFDocument()
                                    ├── Set landscape, add logo
                                    ├── Build XWPFTable with rows
                                    └── doc.write(outputStream)
```
