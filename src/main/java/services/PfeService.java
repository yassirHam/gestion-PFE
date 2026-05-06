package services;

import entities.Affectation;
import entities.Etudiant;
import entities.FichierListe;
import entities.Professeur;
import entities.Soutenance;

import java.util.List;
import java.util.Map;

public interface PfeService {
    
    // Etudiant operations
    void saveEtudiants(List<Etudiant> etudiants, String filiere, String fileName);
    void deleteEtudiantsByFiliere(String filiere);
    List<Etudiant> getEtudiantsByFilieres(List<String> filieres);

    // Professeur operations
    void saveProfesseurs(List<Professeur> profs);
    List<Professeur> getAllProfesseurs();
    void deleteAffectationsAndProfesseurs();

    // Affectation operations
    List<Affectation> getAllAffectationsWithDetails();
    void deleteAllAffectations();
    void lancerAffectationGlobale(List<String> filieres, List<String> debugLog);
    void restoreAffectation(java.io.File backupFile) throws java.io.IOException;
    
    // Fichier operations
    List<FichierListe> getAllFichiers();
    void deleteFichierByFiliere(String filiere);

    // Dashboard operations
    Map<String, Integer> getEtudiantsParProf(List<String> filieresFiltre);
    Map<String, Integer> getEtudiantsParFiliere(List<String> filieresFiltre);
    int getTotalEtudiantsAffectes(List<String> filieresFiltre);
    int getTotalProfesseursEncadrants(List<String> filieresFiltre);
    
    // New Dashboard stats for Soutenances
    Map<String, Integer> getSoutenancesParProf(List<String> filieresFiltre);
    int getTotalSoutenances(List<String> filieresFiltre);

    // Planning operations
    List<Soutenance> genererPlanning(List<String> filieres, List<String> debugLog, List<Long> selectedSalles, String startDate);
    List<Soutenance> getAllSoutenances();
    Map<Long, String> getProfessorColors();
    void deletePlanning();

    // Salles operations
    List<entities.Salle> getAllSalles();
    void addSalle(String numSalle);
}
