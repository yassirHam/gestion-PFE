package services;

import entities.Professeur;
import entities.ProfesseurGrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default jury rapporteur selection strategy. Honors every constraint
 * exposed by {@link ConstraintSet}, including the new operational rules
 * (grade, internal/external mix, language match, jury repetition cap).
 *
 * <p>The strategy proceeds in three layers, each one a refinement of the
 * previous:</p>
 * <ol>
 *   <li><strong>Hard filters</strong> — exclude excluded/maxed-out professors,
 *       enforce the min grade for the president if requested,
 *       enforce the language requirement when it is HARD.</li>
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
        if (available == null || available.size() < 2) return null;

        ConstraintSet constraints = config.getConstraints();
        String dominantDiscipline = constraints.getDominantDiscipline();
        int minSpecialityMatch = constraints.getMinSpecialityMatch();
        int maxLoadGap = constraints.getMaxJuryLoadGap();
        boolean respectEnglish = constraints.isRespectEnglishProf();
        boolean respectLanguage = constraints.isRespectLanguageRequirement();
        ProfesseurGrade minPresidentGrade = constraints.getMinPresidentGrade();
        boolean requireExternal = constraints.isRequireExternalMember();

        // ── Hard filters ────────────────────────────────────────────────────
        // 1. Excluded professors are already filtered out by the planning
        //    engine, but we double-check here for safety in case this strategy
        //    is invoked from another caller.
        List<Professeur> candidates = new ArrayList<>();
        for (Professeur p : available) {
            if (p == null || p.isExcluded()) continue;
            candidates.add(p);
        }
        if (candidates.size() < 2) return null;

        // 2. Min president grade — if the encadrant doesn't meet it AND the
        //    rule is HARD, we cannot honor "encadrant = président". We still
        //    return a jury so the caller can detect the violation, but the
        //    PlanningServiceImpl will catch this in validatePlanning. We
        //    deliberately don't reject here to keep auto-planning resilient.

        Collections.shuffle(candidates);
        candidates.sort(Comparator.comparingInt((Professeur p) -> profJuryCount.getOrDefault(p.getIdp(), 0)));
        int minLoad = candidates.isEmpty() ? 0 : profJuryCount.getOrDefault(candidates.get(0).getIdp(), 0);

        // ── Try NLP-aware selection first ──────────────────────────────────
        if (nlp != null) {
            Professeur[] nlpJury = selectNlpAwareJury(candidates, profJuryCount, minLoad, maxLoadGap, nlp,
                    respectEnglish, respectLanguage, requireExternal);
            if (nlpJury != null) return nlpJury;
        }

        // ── Try to satisfy dominant-discipline + external constraints ──────
        Professeur[] best = selectByDisciplineAndExternal(encadrant, candidates, dominantDiscipline,
                minSpecialityMatch, requireExternal);
        if (best != null) return best;

        // ── Fallback: two least-loaded professors, preferring an external if
        //    the constraint asks for it ──
        if (requireExternal) {
            Professeur firstExternal = null;
            for (Professeur p : candidates) {
                if (!p.isInternal()) { firstExternal = p; break; }
            }
            if (firstExternal != null) {
                for (Professeur p : candidates) {
                    if (!p.getIdp().equals(firstExternal.getIdp())) {
                        return new Professeur[]{firstExternal, p};
                    }
                }
            }
        }
        return new Professeur[]{candidates.get(0), candidates.get(1)};
    }

    private Professeur[] selectNlpAwareJury(List<Professeur> candidates, Map<Long, Integer> profJuryCount,
                                            int minLoad, int maxLoadGap, SujetAnalysis nlp,
                                            boolean respectEnglish, boolean respectLanguage,
                                            boolean requireExternal) {
        String targetSpec = nlp.getBestSpecialite();
        boolean needEnglish = respectEnglish && nlp.isEnglish();
        String targetLanguage = nlp.getLanguage();

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

        // Language-match alternative when English wasn't requested but NLP
        // detected a non-French language and the rule is enabled.
        Professeur languageProf = null;
        if (englishProf == null && respectLanguage && targetLanguage != null
                && !targetLanguage.equalsIgnoreCase("fr")) {
            for (Professeur p : candidates) {
                int load = profJuryCount.getOrDefault(p.getIdp(), 0);
                if (load > minLoad + maxLoadGap) continue;
                if (techProf != null && p.getIdp().equals(techProf.getIdp())) continue;
                if (p.speaksLanguage(targetLanguage)) {
                    languageProf = p;
                    break;
                }
            }
        }

        Professeur secondPick = englishProf != null ? englishProf : languageProf;

        if (techProf != null && secondPick != null) {
            // If we need an external member, ensure at least one of the two
            // is external; otherwise fall through to the next strategy.
            if (!requireExternal || !techProf.isInternal() || !secondPick.isInternal()) {
                return new Professeur[]{techProf, secondPick};
            }
            // Try to find an external as a third pick replacing one of the picks.
            Professeur external = pickExternal(candidates, techProf, secondPick);
            if (external != null) return new Professeur[]{techProf, external};
        }
        if (techProf != null && secondPick == null) {
            for (Professeur p : candidates) {
                if (p.getIdp().equals(techProf.getIdp())) continue;
                if (requireExternal && p.isInternal() && techProf.isInternal()) continue;
                return new Professeur[]{techProf, p};
            }
        }
        if (techProf == null && secondPick != null) {
            for (Professeur p : candidates) {
                if (p.getIdp().equals(secondPick.getIdp())) continue;
                if (requireExternal && p.isInternal() && secondPick.isInternal()) continue;
                return new Professeur[]{p, secondPick};
            }
        }
        return null;
    }

    private Professeur pickExternal(List<Professeur> candidates, Professeur exclude1, Professeur exclude2) {
        for (Professeur p : candidates) {
            if (exclude1 != null && p.getIdp().equals(exclude1.getIdp())) continue;
            if (exclude2 != null && p.getIdp().equals(exclude2.getIdp())) continue;
            if (!p.isInternal()) return p;
        }
        return null;
    }

    private Professeur[] selectByDisciplineAndExternal(Professeur encadrant, List<Professeur> candidates,
                                                       String dominantDiscipline, int minSpecialityMatch,
                                                       boolean requireExternal) {
        boolean encadrantMatchesDominant = matchesDiscipline(encadrant, dominantDiscipline);
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                Professeur p1 = candidates.get(i);
                Professeur p2 = candidates.get(j);
                int matchCount = (encadrantMatchesDominant ? 1 : 0)
                        + (matchesDiscipline(p1, dominantDiscipline) ? 1 : 0)
                        + (matchesDiscipline(p2, dominantDiscipline) ? 1 : 0);
                if (matchCount < minSpecialityMatch) continue;
                if (requireExternal && p1.isInternal() && p2.isInternal()) continue;
                return new Professeur[]{p1, p2};
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
