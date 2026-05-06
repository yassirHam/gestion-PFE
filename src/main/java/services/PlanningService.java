package services;

import entities.Soutenance;
import java.util.List;
import java.util.Map;

public interface PlanningService {
    /**
     * Generate the full soutenance schedule from existing affectations.
     * @param debugLog list to append log messages
     * @return list of generated soutenances
     */
    List<Soutenance> genererPlanning(List<String> filieres, List<String> debugLog, List<Long> selectedSalles, String startDate);

    /**
     * Return all soutenances with full details (join-fetched).
     */
    List<Soutenance> getAllSoutenances();

    /**
     * Return the color map: professeur.idp -> hex color (e.g. "FF5733")
     */
    Map<Long, String> getProfessorColors();

    /**
     * Delete all soutenances and juries (reset planning).
     */
    void deletePlanning();
}
