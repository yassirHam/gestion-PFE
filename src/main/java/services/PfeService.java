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
    
    // Fichier operations
    List<FichierListe> getAllFichiers();
    void deleteFichierByFiliere(String filiere);

    // Dashboard operations
    Map<String, Integer> getEtudiantsParProf();
    Map<String, Integer> getEtudiantsParFiliere();
    int getTotalEtudiantsAffectes();
    int getTotalProfesseursEncadrants();

    // Planning operations
    List<Soutenance> genererPlanning(List<String> debugLog);
    List<Soutenance> getAllSoutenances();
    Map<Long, String> getProfessorColors();
    void deletePlanning();
}
