package services;

import entities.Professeur;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DefaultJurySelectionStrategy implements JurySelectionStrategy {

    @Override
    public Professeur[] selectJury(Professeur encadrant, List<Professeur> available, Map<Long, Integer> profJuryCount,
                                   int globalMinLoad, SujetAnalysis nlp, PlanningConfig config) {
        if (available == null || available.size() < 2) return null;

        List<Professeur> candidates = new ArrayList<>(available);
        boolean encadrantIsInfo = isInfo(encadrant);
        Collections.shuffle(candidates);
        // Sort by current load ascending so least-loaded profs are tried first.
        candidates.sort(Comparator.comparingInt((Professeur p) -> profJuryCount.getOrDefault(p.getIdp(), 0)));

        int maxLoadGap = config.getMaxJuryLoadGap();
        int loadCeiling = globalMinLoad + maxLoadGap;

        // 1) NLP-aware selection, strictly respecting the global load ceiling.
        if (nlp != null) {
            Professeur[] nlpJury = selectNlpAwareJury(candidates, profJuryCount, loadCeiling, encadrantIsInfo, nlp);
            if (nlpJury != null) {
                return nlpJury;
            }
        }

        // 2) "2 informaticiens" rule + load ceiling. Strictest: respects both.
        Professeur[] pair = pickPair(candidates, profJuryCount, loadCeiling, encadrantIsInfo, true);
        if (pair != null) return pair;

        // 3) "2 informaticiens" rule WITHOUT the load ceiling. The discipline rule
        //    is treated as the higher-priority constraint: we prefer an unbalanced
        //    jury load over a jury that lacks 2 informaticiens. If the load ceiling
        //    is exceeded, the verification layer will surface it on the dashboard
        //    so the user can rebalance manually if needed.
        pair = pickPair(candidates, profJuryCount, Integer.MAX_VALUE, encadrantIsInfo, true);
        if (pair != null) return pair;

        // 4) Load ceiling only, dropping the informaticiens rule. Reached only when
        //    there genuinely aren't enough info profs available at this slot.
        pair = pickPair(candidates, profJuryCount, loadCeiling, encadrantIsInfo, false);
        if (pair != null) return pair;

        // 5) Absolute fallback: any 2 (least loaded first).
        return new Professeur[]{candidates.get(0), candidates.get(1)};
    }

    /**
     * Iterates pairs in load-ascending order and returns the first valid one.
     * Always picks the lowest-loaded pair that satisfies the constraints, so the
     * gap is minimized at every step rather than just bounded.
     */
    private Professeur[] pickPair(List<Professeur> candidates, Map<Long, Integer> profJuryCount,
                                  int loadCeiling, boolean encadrantIsInfo, boolean enforceInfoRule) {
        for (int i = 0; i < candidates.size(); i++) {
            Professeur p1 = candidates.get(i);
            if (load(profJuryCount, p1) > loadCeiling) continue;
            for (int j = i + 1; j < candidates.size(); j++) {
                Professeur p2 = candidates.get(j);
                if (load(profJuryCount, p2) > loadCeiling) continue;
                if (enforceInfoRule) {
                    int infoCount = (encadrantIsInfo ? 1 : 0) + (isInfo(p1) ? 1 : 0) + (isInfo(p2) ? 1 : 0);
                    if (infoCount < 2) continue;
                }
                return new Professeur[]{p1, p2};
            }
        }
        return null;
    }

    private Professeur[] selectNlpAwareJury(List<Professeur> candidates, Map<Long, Integer> profJuryCount,
                                            int loadCeiling, boolean encadrantIsInfo, SujetAnalysis nlp) {
        String targetSpec = nlp.getBestSpecialite();
        boolean needEnglish = nlp.isEnglish();

        // techProf: matches the subject specialty, respects the load ceiling, prefers least loaded.
        Professeur techProf = candidates.stream()
                .filter(p -> load(profJuryCount, p) <= loadCeiling)
                .filter(p -> containsIgnoreCase(p.getSpecialite(), targetSpec != null ? targetSpec : ""))
                .findFirst()
                .orElse(null);

        Professeur englishProf = null;
        if (needEnglish) {
            final Professeur selectedTechProf = techProf;
            englishProf = candidates.stream()
                    .filter(p -> load(profJuryCount, p) <= loadCeiling)
                    .filter(this::isEnglish)
                    .filter(p -> selectedTechProf == null || !p.getIdp().equals(selectedTechProf.getIdp()))
                    .findFirst()
                    .orElse(null);
        }

        if (techProf != null && englishProf != null) {
            return new Professeur[]{techProf, englishProf};
        }

        // Tech matched, no english needed -> need a second prof respecting the ceiling AND the info rule.
        if (techProf != null && !needEnglish) {
            final Professeur selectedTechProf = techProf;
            // Prefer info prof if encadrant is not info AND techProf is not info.
            int infoSoFar = (encadrantIsInfo ? 1 : 0) + (isInfo(techProf) ? 1 : 0);
            Professeur r2 = candidates.stream()
                    .filter(p -> !p.getIdp().equals(selectedTechProf.getIdp()))
                    .filter(p -> load(profJuryCount, p) <= loadCeiling)
                    .filter(p -> infoSoFar >= 2 || isInfo(p))
                    .findFirst()
                    .orElse(null);
            // If no info prof under the ceiling, prioritize the info rule over the
            // load ceiling: try to find an info prof anywhere before falling back
            // to any prof under the ceiling.
            if (r2 == null && infoSoFar < 2) {
                r2 = candidates.stream()
                        .filter(p -> !p.getIdp().equals(selectedTechProf.getIdp()))
                        .filter(this::isInfo)
                        .findFirst()
                        .orElse(null);
            }
            // Last fallback: any prof under the ceiling.
            if (r2 == null) {
                r2 = candidates.stream()
                        .filter(p -> !p.getIdp().equals(selectedTechProf.getIdp()))
                        .filter(p -> load(profJuryCount, p) <= loadCeiling)
                        .findFirst()
                        .orElse(null);
            }
            if (r2 != null) {
                return new Professeur[]{techProf, r2};
            }
        }

        // English matched, tech didn't -> need a tech prof respecting the ceiling.
        if (techProf == null && englishProf != null) {
            final Professeur selectedEnglishProf = englishProf;
            Professeur r1 = candidates.stream()
                    .filter(p -> !p.getIdp().equals(selectedEnglishProf.getIdp()))
                    .filter(p -> load(profJuryCount, p) <= loadCeiling)
                    .findFirst()
                    .orElse(null);
            if (r1 != null) {
                return new Professeur[]{r1, selectedEnglishProf};
            }
        }
        return null;
    }

    private int load(Map<Long, Integer> profJuryCount, Professeur p) {
        return profJuryCount.getOrDefault(p.getIdp(), 0);
    }

    private boolean isInfo(Professeur p) {
        return containsIgnoreCase(p.getDiscipline(), "info") || containsIgnoreCase(p.getSpecialite(), "info");
    }

    private boolean isEnglish(Professeur p) {
        return containsIgnoreCase(p.getDiscipline(), "anglais") || containsIgnoreCase(p.getDiscipline(), "english")
                || containsIgnoreCase(p.getSpecialite(), "anglais") || containsIgnoreCase(p.getSpecialite(), "english");
    }

    private boolean containsIgnoreCase(String value, String expected) {
        if (value == null || expected == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }
}
