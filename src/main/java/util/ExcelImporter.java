package util;

import entities.Etudiant;
import entities.Professeur;
import entities.Salle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excel parsing utilities for the PFE management application.
 *
 * <p>The application uses a single multi-sheet workbook:</p>
 * <ul>
 *   <li>One sheet per filière (sheet name = filière code) with student rows</li>
 *   <li>One sheet "Professeurs" with the professor list</li>
 *   <li>An optional sheet "Salles" with predefined rooms</li>
 * </ul>
 *
 * <p>Legacy single-sheet imports are still supported via the
 * {@link #importEtudiants(InputStream, String)} and
 * {@link #importProfs(InputStream)} methods.</p>
 */
public class ExcelImporter {

    private static final DataFormatter FORMATTER = new DataFormatter();

    // ─── Sheet name predicates ──────────────────────────────────────────────

    public static boolean isProfesseursSheet(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase(Locale.ROOT);
        // Exact matches
        if (n.equals("professeurs") || n.equals("professeur") || n.equals("profs") || n.equals("prof")) return true;
        // Pattern matches: sheets that clearly list professors
        if (n.contains("liste") && (n.contains("prof") || n.contains("enseignant"))) return true;
        if (n.contains("enseignants") || n.contains("enseignant")) return true;
        if (n.startsWith("prof") || n.startsWith("enseignant")) return true;
        return false;
    }

    public static boolean isSallesSheet(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase(Locale.ROOT);
        return n.equals("salles") || n.equals("salle") || n.equals("rooms") || n.equals("room");
    }

    // ─── Cell helpers ────────────────────────────────────────────────────────

    /** Empty-string-safe, trimmed value for any cell type. */
    public static String getCellValue(Row row, int index) {
        if (row == null) return "";
        Cell cell = row.getCell(index);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.BLANK) return "";
        return FORMATTER.formatCellValue(cell).trim();
    }

    private static boolean isRowBlank(Row row, int columnsToCheck) {
        if (row == null) return true;
        for (int i = 0; i < columnsToCheck; i++) {
            if (!getCellValue(row, i).isEmpty()) return false;
        }
        return true;
    }

    // ─── Public unified import ──────────────────────────────────────────────

    /**
     * Result returned by {@link #importWorkbook(InputStream)}.
     */
    public static class ImportResult {
        private final Map<String, List<Etudiant>> studentsByFiliere = new LinkedHashMap<>();
        private final List<Professeur> professeurs = new ArrayList<>();
        private final List<Salle> salles = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public Map<String, List<Etudiant>> getStudentsByFiliere() { return studentsByFiliere; }
        public List<Professeur> getProfesseurs() { return professeurs; }
        public List<Salle> getSalles() { return salles; }
        public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }

        public int getStudentCount() {
            int n = 0;
            for (List<Etudiant> v : studentsByFiliere.values()) n += v.size();
            return n;
        }

        public Set<String> getFilieres() { return studentsByFiliere.keySet(); }

        void addWarning(String w) { if (w != null) warnings.add(w); }
    }

    /**
     * Reads every sheet of the supplied workbook and dispatches each row to the
     * appropriate parser based on the sheet name.
     */
    public static ImportResult importWorkbook(InputStream is) {
        ImportResult result = new ImportResult();
        try (Workbook wb = new XSSFWorkbook(is)) {
            int sheetCount = wb.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = wb.getSheetAt(i);
                String name = sheet.getSheetName();
                if (name == null || name.trim().isEmpty()) {
                    result.addWarning("Feuille " + (i + 1) + " sans nom : ignoree.");
                    continue;
                }

                if (isProfesseursSheet(name)) {
                    List<Professeur> profs = parseProfesseursSheet(sheet);
                    result.getProfesseurs().addAll(profs);
                    result.addWarning("Feuille '" + name + "' : " + profs.size() + " professeur(s) lus.");
                } else if (isSallesSheet(name)) {
                    List<Salle> salles = parseSallesSheet(sheet);
                    result.getSalles().addAll(salles);
                    result.addWarning("Feuille '" + name + "' : " + salles.size() + " salle(s) lues.");
                } else {
                    String filiere = normalizeFiliere(name);
                    List<Etudiant> students = parseEtudiantsSheet(sheet, filiere);
                    if (!students.isEmpty()) {
                        result.getStudentsByFiliere()
                                .computeIfAbsent(filiere, k -> new ArrayList<>())
                                .addAll(students);
                        result.addWarning("Feuille '" + name + "' (filiere " + filiere + ") : "
                                + students.size() + " etudiant(s) lus.");
                    } else {
                        result.addWarning("Feuille '" + name + "' : aucun etudiant detecte.");
                    }
                }
            }
        } catch (Exception e) {
            result.addWarning("Erreur lors de la lecture du fichier Excel : " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    // ─── Per-sheet parsers ──────────────────────────────────────────────────

    private static List<Etudiant> parseEtudiantsSheet(Sheet sheet, String filiere) {
        List<Etudiant> list = new ArrayList<>();
        if (sheet == null) return list;
        int firstDataRow = detectStudentDataStartRow(sheet);

        Set<String> seenCne = new HashSet<>();
        for (Row row : sheet) {
            if (row.getRowNum() < firstDataRow) continue;
            if (isRowBlank(row, 6)) continue;

            Etudiant e = new Etudiant();
            e.setCne(getCellValue(row, 0));
            e.setNomE(getCellValue(row, 1));
            e.setPrenomE(getCellValue(row, 2));
            e.setEmail(getCellValue(row, 3));
            e.setBinome_cne(getCellValue(row, 4)); // raw value — resolved below
            e.setFiliere(filiere);

            String sujet = getCellValue(row, 5);
            if (sujet == null || sujet.trim().isEmpty()) {
                sujet = "Projet de fin d'etudes";
            }
            e.setSujet_stage(sujet);

            if (e.getNomE() == null || e.getNomE().isEmpty()) continue;
            String cne = e.getCne();
            if (cne != null && !cne.isEmpty() && !seenCne.add(cne)) continue;

            list.add(e);
        }

        // Second pass: resolve binome_cne values that look like "NOM PRÉNOM"
        // (i.e. not a CNE code) by looking up the matching student in the same sheet.
        // Build a lookup map: normalised "NOM PRÉNOM" -> CNE
        Map<String, String> nameToCne = new java.util.HashMap<>();
        for (Etudiant e : list) {
            if (e.getCne() != null && !e.getCne().isEmpty()
                    && e.getNomE() != null && e.getPrenomE() != null) {
                // Index by "NOM PRÉNOM" and "PRÉNOM NOM" (both orders)
                String key1 = normalizeNameKey(e.getNomE() + " " + e.getPrenomE());
                String key2 = normalizeNameKey(e.getPrenomE() + " " + e.getNomE());
                nameToCne.put(key1, e.getCne());
                nameToCne.put(key2, e.getCne());
            }
        }
        for (Etudiant e : list) {
            String raw = e.getBinome_cne();
            if (raw == null || raw.trim().isEmpty()) continue;
            // If it already looks like a CNE (contains digits and letters, no spaces,
            // or matches the typical Rxxxxxxx pattern), keep it as-is.
            if (looksLikeCne(raw)) continue;
            // Otherwise treat it as a name and resolve to CNE.
            String resolved = nameToCne.get(normalizeNameKey(raw));
            if (resolved != null) {
                e.setBinome_cne(resolved);
            }
            // If not found, keep the raw value — the service layer will handle it gracefully.
        }

        return list;
    }

    /**
     * Returns true if the value looks like a CNE code rather than a person's name.
     * A CNE typically has no spaces and contains a mix of letters and digits
     * (e.g. "R140025687", "G123456", "20190001").
     */
    private static boolean looksLikeCne(String value) {
        if (value == null) return false;
        String v = value.trim();
        // Contains a space → almost certainly a name
        if (v.contains(" ")) return false;
        // All digits → student ID number
        if (v.matches("\\d+")) return true;
        // Starts with a letter followed by digits (common Moroccan CNE pattern)
        if (v.matches("[A-Za-z]{1,3}\\d+")) return true;
        // Purely alphabetic with no digits → likely a single-word name, treat as name
        if (v.matches("[A-Za-zÀ-ÿ]+")) return false;
        // Mixed alphanumeric without spaces → treat as CNE
        return v.matches("[A-Za-z0-9]+");
    }

    /**
     * Normalise a full name for lookup: uppercase, remove accents, collapse spaces.
     */
    private static String normalizeNameKey(String name) {
        if (name == null) return "";
        String n = java.text.Normalizer.normalize(name.trim().toUpperCase(Locale.ROOT),
                java.text.Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", ""); // strip combining diacritics
        n = n.replaceAll("\\s+", " ").trim();
        return n;
    }

    private static List<Professeur> parseProfesseursSheet(Sheet sheet) {
        List<Professeur> list = new ArrayList<>();
        if (sheet == null) return list;
        int firstDataRow = detectProfesseursDataStartRow(sheet);

        Set<String> seenKeys = new HashSet<>();
        for (Row row : sheet) {
            if (row.getRowNum() < firstDataRow) continue;
            if (isRowBlank(row, 4)) continue;

            Professeur p = new Professeur();
            p.setNom(getCellValue(row, 0));
            p.setPrenom(getCellValue(row, 1));
            p.setDiscipline(getCellValue(row, 2));
            p.setSpecialite(getCellValue(row, 3));

            if ((p.getNom() == null || p.getNom().isEmpty())
                    && (p.getPrenom() == null || p.getPrenom().isEmpty())) {
                continue;
            }
            String key = (p.getNom() + "|" + p.getPrenom()).toLowerCase(Locale.ROOT);
            if (!seenKeys.add(key)) continue;

            list.add(p);
        }
        return list;
    }

    private static List<Salle> parseSallesSheet(Sheet sheet) {
        List<Salle> list = new ArrayList<>();
        if (sheet == null) return list;
        int firstDataRow = detectSimpleHeaderStartRow(sheet);

        Set<String> seenNames = new HashSet<>();
        for (Row row : sheet) {
            if (row.getRowNum() < firstDataRow) continue;
            if (isRowBlank(row, 3)) continue;

            String num = getCellValue(row, 0);
            if (num == null || num.isEmpty()) continue;
            String key = num.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
            if (!seenNames.add(key)) continue;

            Salle s = new Salle();
            s.setNum_salle(num.trim());
            String block = getCellValue(row, 1);
            s.setBlock(block == null ? "" : block);
            String status = getCellValue(row, 2);
            s.setStatus(status == null || status.isEmpty() ? "Libre" : status);
            list.add(s);
        }
        return list;
    }

    // ─── Header detection (skip decorative rows) ────────────────────────────

    private static int detectStudentDataStartRow(Sheet sheet) {
        // Try to find a row that contains "CNE" or "NOM" or "PRENOM" (header).
        // Otherwise skip the first row.
        int last = Math.min(3, sheet.getLastRowNum());
        for (int i = 0; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String v0 = getCellValue(row, 0).toUpperCase(Locale.ROOT);
            String v1 = getCellValue(row, 1).toUpperCase(Locale.ROOT);
            if (v0.contains("CNE") || v1.contains("NOM") || v0.contains("CODE")) {
                return i + 1;
            }
        }
        return 1;
    }

    private static int detectProfesseursDataStartRow(Sheet sheet) {
        // Original template had 2 header rows (decorative + real). Detect them.
        int last = Math.min(4, sheet.getLastRowNum());
        int headerRow = -1;
        for (int i = 0; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String v0 = getCellValue(row, 0).toUpperCase(Locale.ROOT);
            String v1 = getCellValue(row, 1).toUpperCase(Locale.ROOT);
            if (v0.contains("NOM") && v1.contains("PR")) {
                headerRow = i;
                break;
            }
        }
        if (headerRow >= 0) return headerRow + 1;
        // Fallback: treat the first 2 rows as headers (matches legacy behaviour).
        return 2;
    }

    private static int detectSimpleHeaderStartRow(Sheet sheet) {
        Row row0 = sheet.getRow(0);
        if (row0 == null) return 1;
        String v0 = getCellValue(row0, 0).toUpperCase(Locale.ROOT);
        if (v0.contains("SALLE") || v0.contains("NUM") || v0.contains("ROOM")) {
            return 1;
        }
        return 0;
    }

    // ─── Filiere normalization ──────────────────────────────────────────────

    /**
     * Normalize a sheet name to a filière code. The sheet name is preserved
     * as-is (uppercased and stripped of special characters) so the application
     * works with any establishment's filière naming.
     */
    public static String normalizeFiliere(String rawName) {
        if (rawName == null) return "INCONNUE";
        String name = rawName.trim();
        if (name.isEmpty()) return "INCONNUE";
        String upper = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        // Trim leading / trailing underscores left over by the substitution.
        upper = upper.replaceAll("^_+", "").replaceAll("_+$", "");
        return upper.isEmpty() ? "INCONNUE" : upper;
    }

    // ─── Legacy single-sheet APIs (kept for backward compatibility) ─────────

    /**
     * @deprecated use {@link #importWorkbook(InputStream)} instead. The new UI
     *             uses a single multi-sheet workbook.
     */
    @Deprecated
    public static List<Etudiant> importEtudiants(InputStream is, String filiere) {
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            return parseEtudiantsSheet(sheet, filiere);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * @deprecated use {@link #importWorkbook(InputStream)} instead.
     */
    @Deprecated
    public static List<Professeur> importProfs(InputStream is) {
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            return parseProfesseursSheet(sheet);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
