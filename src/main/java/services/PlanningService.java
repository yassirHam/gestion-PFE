package services;

import entities.Soutenance;
import java.util.List;
import java.util.Map;

public interface PlanningService {

    /**
     * Generate the full soutenance schedule from the existing affectations.
     *
     * <p>The method returns a {@link PlanningResult} that contains either a
     * successful planning (already persisted) or a list of constraint
     * violations and recommendations explaining why the planning could not be
     * generated. Implementations <strong>must</strong> roll back / not persist
     * the result when {@link PlanningResult#isSuccess()} is {@code false}.</p>
     *
     * @param filieres        filière filter (null/empty = all)
     * @param selectedSalles  salle ids selected by the user (null/empty = all)
     * @param config          full planning configuration including constraints
     */
    PlanningResult genererPlanning(List<String> filieres, List<Long> selectedSalles, PlanningConfig config);

    /** Return all soutenances with full details (join-fetched). */
    List<Soutenance> getAllSoutenances();

    /** Return the color map: professeur.idp -> hex color (e.g. "FF5733"). */
    Map<Long, String> getProfessorColors();

    /** Delete all soutenances and juries (reset planning). */
    void deletePlanning();
}
