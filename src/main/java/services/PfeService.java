package services;

import entities.Affectation;
import entities.Etudiant;
import entities.FichierListe;
import entities.Professeur;
import entities.Salle;
import entities.Soutenance;
import util.ExcelImporter;

import java.util.List;
import java.util.Map;

public interface PfeService {

    // Etudiant
    void saveEtudiants(List<Etudiant> etudiants, String filiere, String fileName);
    void deleteEtudiantsByFiliere(String filiere);

    // Professeur
    void saveProfesseurs(List<Professeur> profs);
    void deleteAffectationsAndProfesseurs();

    // Affectation
    List<Affectation> getAllAffectationsWithDetails();
    void lancerAffectationGlobale(List<String> filieres, List<String> debugLog);
    void restoreAffectation(java.io.File backupFile) throws java.io.IOException;

    // Fichier
    List<FichierListe> getAllFichiers();

    // Dashboard
    Map<String, Integer> getEtudiantsParProf(List<String> filieresFiltre);
    Map<String, Integer> getEtudiantsParFiliere(List<String> filieresFiltre);
    int getTotalEtudiantsAffectes(List<String> filieresFiltre);
    int getTotalProfesseursEncadrants(List<String> filieresFiltre);
    Map<String, Integer> getSoutenancesParProf(List<String> filieresFiltre);
    int getTotalSoutenances(List<String> filieresFiltre);
    int getTotalProfesseurs();
    int getTotalProjetsAffectes(List<String> filieresFiltre);
    VerificationReport verifierFichiersGeneres(List<String> filieresFiltre);
    VerificationReport verifierFichiersGeneres(List<String> filieresFiltre, ConstraintSet constraints);
    Map<String, Object> searchDashboard(String query);

    // Planning
    /**
     * Generate the planning. Returns a {@link PlanningResult} with the
     * generated soutenances when successful, or a list of violations and
     * suggestions otherwise. The planning is persisted only when
     * {@link PlanningResult#isSuccess()} is {@code true}.
     */
    PlanningResult genererPlanning(List<String> filieres, List<Long> selectedSalles, PlanningConfig config);

    List<Soutenance> getAllSoutenances();
    Map<Long, String> getProfessorColors();
    void deletePlanning();

    // Salles
    List<Salle> getAllSalles();
    boolean addSalle(String numSalle);
    int addSalleBulk(String multilineNames);
    int saveSallesIfMissing(List<Salle> imported);
    boolean deleteSalle(Long id);
    boolean isSalleUsedInPlanning(Long id);
    int deleteAllSalles();

    // Unified Excel import (single workbook, multiple sheets)
    ExcelImporter.ImportResult importWorkbook(java.io.InputStream is, String fileName);

    // Recommendations
    List<Recommendation> generateRecommendations(List<String> filieres, int numberOfRooms, PlanningConfig config);
}
