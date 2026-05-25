package services;

import entities.Professeur;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default jury rapporteur selection strategy. Honors the constraints exposed
 * by {@link ConstraintSet} (dominant discipline, specialty match, English
 * profession requirement, jury load gap).
 *
 * <p>The strategy proceeds in three layers, each one a refinement of the
 * previous:</p>
 * <ol>
 *   <li><strong>Hard filters</strong> — exclude excluded/maxed-out professors.</li>
 *   <li><strong>Specialty / NLP-aware matching</strong> — pick a tech profile
 *       matching the subject when NLP data is available, and (optionally) an
 *       English-speaking second member for English subjects.</li>
 *   <li><strong>Fallback</strong> — when no preferred candidate is available,
 *       return the two least-loaded professors.</li>
 * </ol>
 */
public class DefaultJurySelectionStrategy implements JurySelectionStrategy {

    @Override
    public Professeur[] selectJury(Professeur encadrant, List<Professeur> available,
                                   Map<Long, Integer> profJuryCount, SujetAnalysis nlp, PlanningConfig config) {
        ConstraintSet constraints = config.getConstraints();
        int jurySize = constraints.getJurySize(); // total including encadrant
        int numRapporteurs = jurySize - 1;         // how many additional members to pick
        numRapporteurs = Math.max(1, numRapporteurs); // at least 1

        if (available == null || available.size() < numRapporteurs) return null;

        String dominantDiscipline = constraints.getDominantDiscipline();
        int minSpecialityMatch = constraints.getMinSpecialityMatch();
        int maxLoadGap = constraints.getMaxJuryLoadGap();
        boolean respectEnglish = constraints.isRespectEnglishProf();

        // ── Hard filters ────────────────────────────────────────────────────
        List<Professeur> candidates = new ArrayList<>();
        for (Professeur p : available) {
            if (p == null || p.isExcluded()) continue;
            candidates.add(p);
        }
        if (candidates.size() < numRapporteurs) return null;

        Collections.shuffle(candidates);
        candidates.sort(Comparator.comparingInt((Professeur p) -> profJuryCount.getOrDefault(p.getIdp(), 0)));
        int minLoad = candidates.isEmpty() ? 0 : profJuryCount.getOrDefault(candidates.get(0).getIdp(), 0);

        // For a single rapporteur we just pick the least-loaded available prof.
        if (numRapporteurs == 1) {
            return new Professeur[]{candidates.get(0)};
        }

        // ── Try to satisfy dominant-discipline FIRST (2+ rapporteurs) ───────
        if (minSpecialityMatch > 0) {
            Professeur[] best = selectByDiscipline(encadrant, candidates, dominantDiscipline,
                    minSpecialityMatch, numRapporteurs);
            if (best != null) return best;
        }

        // ── Try NLP-aware selection ──────────────────────────────────────────
        if (nlp != null && numRapporteurs == 2) {
            Professeur[] nlpJury = selectNlpAwareJury(candidates, profJuryCount, minLoad, maxLoadGap, nlp,
                    respectEnglish);
            if (nlpJury != null) return nlpJury;
        }

        // ── Fallback: N least-loaded professors ───────────────────────────────
        int take = Math.min(numRapporteurs, candidates.size());
        Professeur[] fallback = new Professeur[take];
        for (int i = 0; i < take; i++) fallback[i] = candidates.get(i);
        return fallback;
    }

    private Professeur[] selectNlpAwareJury(List<Professeur> candidates, Map<Long, Integer> profJuryCount,
                                            int minLoad, int maxLoadGap, SujetAnalysis nlp,
                                            boolean respectEnglish) {
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
        if (techProf != null) {
            for (Professeur p : candidates) {
                if (p.getIdp().equals(techProf.getIdp())) continue;
                return new Professeur[]{techProf, p};
            }
        }
        if (englishProf != null) {
            for (Professeur p : candidates) {
                if (p.getIdp().equals(englishProf.getIdp())) continue;
                return new Professeur[]{p, englishProf};
            }
        }
        return null;
    }

    private Professeur[] selectByDiscipline(Professeur encadrant, List<Professeur> candidates,
                                            String dominantDiscipline, int minSpecialityMatch,
                                            int numRapporteurs) {
        boolean encadrantMatchesDominant = matchesDiscipline(encadrant, dominantDiscipline);
        if (numRapporteurs == 1) {
            // 1 rapporteur
            for (Professeur p : candidates) {
                int matchCount = (encadrantMatchesDominant ? 1 : 0)
                        + (matchesDiscipline(p, dominantDiscipline) ? 1 : 0);
                if (matchCount >= minSpecialityMatch) return new Professeur[]{p};
            }
            return null;
        }
        // 2+ rapporteurs: find the first valid pair then fill the rest with least-loaded
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                Professeur p1 = candidates.get(i);
                Professeur p2 = candidates.get(j);
                int matchCount = (encadrantMatchesDominant ? 1 : 0)
                        + (matchesDiscipline(p1, dominantDiscipline) ? 1 : 0)
                        + (matchesDiscipline(p2, dominantDiscipline) ? 1 : 0);
                if (matchCount < minSpecialityMatch) continue;
                if (numRapporteurs == 2) return new Professeur[]{p1, p2};
                // For 3+ rapporteurs: fill remaining slots with next least-loaded
                List<Professeur> result = new java.util.ArrayList<>();
                result.add(p1); result.add(p2);
                java.util.Set<Integer> used = new java.util.HashSet<>(java.util.Arrays.asList(i, j));
                for (int k = 0; k < candidates.size() && result.size() < numRapporteurs; k++) {
                    if (!used.contains(k)) { result.add(candidates.get(k)); used.add(k); }
                }
                if (result.size() == numRapporteurs) return result.toArray(new Professeur[0]);
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
        if (p.speaksLanguage("en") || p.speaksLanguage("ag") || p.speaksLanguage("english")) return true;
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
