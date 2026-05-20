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
     * Generate the full soutenance schedule with custom hour slots.
     * @param filieres list of filieres to include
     * @param debugLog list to append log messages
     * @param selectedSalles list of salle IDs to use
     * @param startDate start date string (yyyy-MM-dd)
     * @param customSlots custom hour slots array (e.g. {10, 11, 14, 15, 16})
     * @return list of generated soutenances
     */
    List<Soutenance> genererPlanning(List<String> filieres, List<String> debugLog, List<Long> selectedSalles, String startDate, int[] customSlots);

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
