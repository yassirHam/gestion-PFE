package controller;

import dao.AffectationDAO;
import dao.AffectationDAOImpl;
import entities.Affectation;
import entities.Etudiant;
import entities.Professeur;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.util.List;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

@WebServlet("/exportDocx")
public class ExportDocxServlet extends HttpServlet {

    private AffectationDAO affDao = new AffectationDAOImpl();

    // ── Couleurs hex ─────────────────────────────────────────────────────────
    private static final String C_HEADER = "1A56DB";  // bleu foncé (encadrant & en-tête)
    private static final String C_GI     = "CFE2FF";  // bleu clair
    private static final String C_ID     = "FFF3CD";  // jaune pâle
    private static final String C_TDIA   = "D9EAD3";  // vert pâle
    private static final String C_EMPTY  = "F8F9FA";  // gris très clair
    private static final String C_WHITE  = "FFFFFF";

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<Affectation> affectations = affDao.findAllWithDetails();

        resp.setContentType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        resp.setHeader("Content-Disposition", "attachment; filename=affectations.docx");

        try (XWPFDocument doc = new XWPFDocument()) {

            // ── Orientation paysage ──────────────────────────────────────────
            CTDocument1 ctDoc = doc.getDocument();
            CTBody body = ctDoc.getBody();
            if (!body.isSetSectPr()) body.addNewSectPr();
            CTSectPr sect = body.getSectPr();
            CTPageSz pgSz = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
            pgSz.setW(BigInteger.valueOf(16838)); // A4 landscape width (twips)
            pgSz.setH(BigInteger.valueOf(11906)); // A4 landscape height
            pgSz.setOrient(STPageOrientation.LANDSCAPE);

            // ── En-tête document ────────────────────────────────────────────
            center(doc, "École Nationale des Sciences Appliquées – Al Hoceima", 14, true);
            center(doc, "Département Mathématiques et Informatique", 12, false);
            center(doc, "Affectation des encadrants de Projet de Fin d'Etude", 11, false);
            center(doc, "Année Universitaire 2025/2026", 10, false);
            doc.createParagraph(); // espace

            // ── Légende ─────────────────────────────────────────────────────
            XWPFTable legend = doc.createTable(1, 3);
            setWidth(legend, 4000);
            setLegendCell(legend.getRow(0).getCell(0), "Filière ID",   C_ID);
            setLegendCell(legend.getRow(0).getCell(1), "Filière GI",   C_GI);
            setLegendCell(legend.getRow(0).getCell(2), "Filière TDIA", C_TDIA);
            doc.createParagraph();

            // ── Grouper par encadrant ────────────────────────────────────────
            Map<Professeur, List<Etudiant>> map = new LinkedHashMap<>();
            affectations.sort(Comparator.comparing(a -> a.getEncadrant().getNom()));
            for (Affectation a : affectations) {
                map.computeIfAbsent(a.getEncadrant(), k -> new ArrayList<>())
                   .add(a.getEtudiant());
            }

            // ── Tableau principal ─────────────────────────────────────────────
            // 10 colonnes : NomProf | PrenomProf | Nom1|Prenom1 | Nom2|Prenom2 | Nom3|Prenom3 | Nom4|Prenom4
            XWPFTable table = doc.createTable();
            setWidth(table, 9500); // ~100% de la page paysage

            // ── Ligne 1 en-tête : "Encadrant" (2 cols) | "Etudiants encadrés" (8 cols) ──
            XWPFTableRow row0 = table.getRow(0);
            // Forcer 10 cellules dès la création
            while (row0.getTableCells().size() < 10) row0.addNewTableCell();

            setCellMergeH(row0, 0, 1, "Encadrant",          C_HEADER, true, true, 10);
            setCellMergeH(row0, 2, 9, "Etudiants encadrés", C_HEADER, true, true, 10);

            // ── Ligne 2 : sous-en-têtes ──
            XWPFTableRow row1 = table.createRow();
            while (row1.getTableCells().size() < 10) row1.addNewTableCell();

            setCell(row1.getCell(0), "Nom",    C_HEADER, true, true, 9);
            setCell(row1.getCell(1), "Prénom", C_HEADER, true, true, 9);
            for (int i = 1; i <= 4; i++) {
                setCell(row1.getCell((i - 1) * 2 + 2), "Etudiant " + i, C_HEADER, true, true, 9);
                setCell(row1.getCell((i - 1) * 2 + 3), "",              C_HEADER, true, true, 9);
            }

            // ── Données ──────────────────────────────────────────────────────
            for (Map.Entry<Professeur, List<Etudiant>> entry : map.entrySet()) {
                Professeur prof = entry.getKey();
                List<Etudiant> list = entry.getValue();

                XWPFTableRow row = table.createRow();
                while (row.getTableCells().size() < 10) row.addNewTableCell();

                setCell(row.getCell(0), prof.getNom(),    C_HEADER, true, true,  9);
                setCell(row.getCell(1), prof.getPrenom(), C_HEADER, true, true,  9);

                for (int i = 0; i < 4; i++) {
                    String nom    = "";
                    String prenom = "";
                    String color  = C_EMPTY;

                    if (i < list.size()) {
                        Etudiant e = list.get(i);
                        nom    = e.getNomE();
                        prenom = e.getPrenomE();
                        color  = filiereColor(e.getFiliere());
                    }

                    setCell(row.getCell(i * 2 + 2), nom,    color, false, false, 8);
                    setCell(row.getCell(i * 2 + 3), prenom, color, false, false, 8);
                }
            }

            doc.write(resp.getOutputStream());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void center(XWPFDocument doc, String text, int size, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setBold(bold);
    }

    // Cellule colorée simple 
    private void setCell(XWPFTableCell cell, String text,
                         String bgColor, boolean whiteText, boolean bold, int size) {
        setCellBg(cell, bgColor);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(size);
        if (whiteText) run.setColor(C_WHITE);
    }

    //Fusion horizontale de from à to dans la même ligne 
    private void setCellMergeH(XWPFTableRow row, int from, int to,
                                String text, String bgColor,
                                boolean whiteText, boolean bold, int size) {
        // Marquer la première cellule comme début de fusion
        XWPFTableCell first = row.getCell(from);
        CTTcPr tcPr0 = getTcPr(first);
        CTHMerge hm0 = tcPr0.isSetHMerge() ? tcPr0.getHMerge() : tcPr0.addNewHMerge();
        hm0.setVal(STMerge.RESTART);
        setCell(first, text, bgColor, whiteText, bold, size);

        // Les cellules intermédiaires : CONTINUE
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

    private void setLegendCell(XWPFTableCell cell, String label, String color) {
        setCellBg(cell, color);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(label);
        run.setFontSize(9);
    }

    private String filiereColor(String filiere) {
        if ("GI".equals(filiere))   return C_GI;
        if ("ID".equals(filiere))   return C_ID;
        if ("TDIA".equals(filiere)) return C_TDIA;
        return C_EMPTY;
    }
} 

 