package services;

import entities.Professeur;

import java.util.List;
import java.util.Map;

public interface JurySelectionStrategy {
    Professeur[] selectJury(Professeur encadrant,
                            List<Professeur> available,
                            Map<Long, Integer> profJuryCount,
                            SujetAnalysis nlp,
                            PlanningConfig config);
}
