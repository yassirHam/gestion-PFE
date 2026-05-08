package services;

import entities.Affectation;
import entities.Etudiant;
import entities.FichierListe;
import entities.Professeur;
import entities.Soutenance;

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
    VerificationReport verifierFichiersGeneres(List<String> filieresFiltre);
    Map<String, Object> searchDashboard(String query);

    // Planning 
    List<Soutenance> genererPlanning(List<String> filieres, List<String> debugLog, List<Long> selectedSalles, String startDate);
    List<Soutenance> getAllSoutenances();
    Map<Long, String> getProfessorColors();
    void deletePlanning();

    // Salles
    List<entities.Salle> getAllSalles();
    boolean addSalle(String numSalle);
}
