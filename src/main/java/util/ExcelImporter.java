package util;

import entities.Etudiant;
import entities.Professeur;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelImporter {

    public static String getCellValue(Row row, int index) {
        if (row.getCell(index) == null) return "";
        return row.getCell(index).toString().trim();
    }

    
    public static List<Etudiant> importEtudiants(InputStream is, String filiere) {

        List<Etudiant> list = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);

            for (Row row : sheet) {

                if (row.getRowNum() == 0) continue;

                Etudiant e = new Etudiant();

                e.setCne(getCellValue(row, 0));
                e.setNomE(row.getCell(1).getStringCellValue());
                e.setPrenomE(row.getCell(2).getStringCellValue());
                e.setEmail(getCellValue(row, 3)); // ✅ FIX

                e.setFiliere(filiere);
                e.setSujet_stage("PFE");

                if (e.getNomE() == null || e.getNomE().isEmpty()) continue;

                list.add(e);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static List<Professeur> importProfs(InputStream is) {
        List<Professeur> list = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || row.getRowNum() == 1 ) continue;

                Professeur p = new Professeur();

                p.setNom(getCellValue(row, 0));
                p.setPrenom(getCellValue(row, 1));
                p.setSpecialite(getCellValue(row, 2));

                list.add(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}