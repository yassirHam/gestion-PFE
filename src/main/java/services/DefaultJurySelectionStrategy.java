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
    public Professeur[] selectJury(Professeur encadrant, List<Professeur> available,
                                   Map<Long, Integer> profJuryCount, SujetAnalysis nlp, PlanningConfig config) {
        if (available == null || available.size() < 2) return null;

        ConstraintSet constraints = config.getConstraints();
        String dominantDiscipline = constraints.getDominantDiscipline();
        int minSpecialityMatch = constraints.getMinSpecialityMatch();
        int maxLoadGap = constraints.getMaxJuryLoadGap();
        boolean respectEnglish = constraints.isRespectEnglishProf();

        List<Professeur> candidates = new ArrayList<>(available);
        Collections.shuffle(candidates);
        candidates.sort(Comparator.comparingInt((Professeur p) -> profJuryCount.getOrDefault(p.getIdp(), 0)));
        int minLoad = candidates.isEmpty() ? 0 : profJuryCount.getOrDefault(candidates.get(0).getIdp(), 0);

        // ── Try NLP-aware selection first (English + spec-match aware) ──
        if (nlp != null) {
            Professeur[] nlpJury = selectNlpAwareJury(candidates, profJuryCount, minLoad, maxLoadGap, nlp,
                    respectEnglish);
            if (nlpJury != null) return nlpJury;
        }

        // ── Try to satisfy the dominant-discipline constraint ──
        boolean encadrantMatchesDominant = matchesDiscipline(encadrant, dominantDiscipline);
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                Professeur p1 = candidates.get(i);
                Professeur p2 = candidates.get(j);
                int matchCount = (encadrantMatchesDominant ? 1 : 0)
                        + (matchesDiscipline(p1, dominantDiscipline) ? 1 : 0)
                        + (matchesDiscipline(p2, dominantDiscipline) ? 1 : 0);
                if (matchCount >= minSpecialityMatch) {
                    return new Professeur[]{p1, p2};
                }
            }
        }

        // ── Fallback: pick the two least-loaded available professors ──
        return new Professeur[]{candidates.get(0), candidates.get(1)};
    }

    private Professeur[] selectNlpAwareJury(List<Professeur> candidates, Map<Long, Integer> profJuryCount,
                                            int minLoad, int maxLoadGap, SujetAnalysis nlp, boolean respectEnglish) {
        String targetSpec = nlp.getBestSpecialite();
        boolean needEnglish = respectEnglish && nlp.isEnglish();

        Professeur techProf = null;
        if (targetSpec != null && !targetSpec.isEmpty()) {
            for (Professeur p : candidates) {
                int load = profJuryCount.getOrDefault(p.getIdp(), 0);
                if (load <= minLoad + maxLoadGap && containsIgnoreCase(p.getSpecialite(), targetSpec)) {
                    techProf = p;
                    break;
                }
            }
        }

        Professeur englishProf = null;
        if (needEnglish) {
            for (Professeur p : candidates) {
                int load = profJuryCount.getOrDefault(p.getIdp(), 0);
                if (load > minLoad + maxLoadGap) continue;
                if (techProf != null && p.getIdp().equals(techProf.getIdp())) continue;
                if (isEnglish(p)) {
                    englishProf = p;
                    break;
                }
            }
        }

        if (techProf != null && englishProf != null) {
            return new Professeur[]{techProf, englishProf};
        }
        if (techProf != null && !needEnglish) {
            for (Professeur p : candidates) {
                if (!p.getIdp().equals(techProf.getIdp())) {
                    return new Professeur[]{techProf, p};
                }
            }
        }
        if (techProf == null && englishProf != null) {
            for (Professeur p : candidates) {
                if (!p.getIdp().equals(englishProf.getIdp())) {
                    return new Professeur[]{p, englishProf};
                }
            }
        }
        return null;
    }

    private boolean matchesDiscipline(Professeur p, String dominantDiscipline) {
        if (p == null || dominantDiscipline == null || dominantDiscipline.isEmpty()) return false;
        return containsIgnoreCase(p.getDiscipline(), dominantDiscipline)
                || containsIgnoreCase(p.getSpecialite(), dominantDiscipline);
    }

    private boolean isEnglish(Professeur p) {
        if (p == null) return false;
        return containsIgnoreCase(p.getDiscipline(), "anglais")
                || containsIgnoreCase(p.getDiscipline(), "english")
                || containsIgnoreCase(p.getSpecialite(), "anglais")
                || containsIgnoreCase(p.getSpecialite(), "english");
    }

    private boolean containsIgnoreCase(String value, String expected) {
        if (value == null || expected == null || expected.isEmpty()) return false;
        return value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }
}
