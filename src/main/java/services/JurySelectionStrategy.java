package services;

import entities.Professeur;

import java.util.List;
import java.util.Map;

public interface JurySelectionStrategy {
    /**
     * Selects two jury members (rapporteurs) for a defense.
     *
     * @param encadrant       the supervisor (will be the jury president, must be excluded from selection)
     * @param available       list of professors available at the slot being considered
     * @param profJuryCount   map of all professors -> their current jury participation count (global)
     * @param globalMinLoad   the minimum jury count across ALL professors eligible for jury duty
     *                        (not just those available in the current slot). Used to enforce the
     *                        load gap consistently across the planning, not locally per slot.
     * @param nlp             optional NLP analysis of the subject
     * @param config          planning configuration
     * @return two-element array of professors, or null if no valid pair found
     */
    Professeur[] selectJury(Professeur encadrant, List<Professeur> available, Map<Long, Integer> profJuryCount,
                            int globalMinLoad, SujetAnalysis nlp, PlanningConfig config);
}
