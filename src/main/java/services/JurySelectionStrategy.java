package services;

import entities.Professeur;

import java.util.List;
import java.util.Map;

/**
 * Strategy for selecting the two rapporteurs of a jury.
 *
 * <p>Implementations honor the dynamic constraints exposed by
 * {@link PlanningConfig#getConstraints()} (dominant discipline, max load gap,
 * etc.).</p>
 */
public interface JurySelectionStrategy {

    Professeur[] selectJury(Professeur encadrant,
                            List<Professeur> available,
                            Map<Long, Integer> profJuryCount,
                            SujetAnalysis nlp,
                            PlanningConfig config);
}
