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
                                   SujetAnalysis nlp, PlanningConfig config) {
        List<Professeur> candidates = new ArrayList<>(available);
        boolean encadrantIsInfo = isInfo(encadrant);
        Collections.shuffle(candidates);
        candidates.sort(Comparator.comparingInt((Professeur p) -> profJuryCount.getOrDefault(p.getIdp(), 0)));
        int minLoad = candidates.isEmpty() ? 0 : profJuryCount.getOrDefault(candidates.get(0).getIdp(), 0);
        int maxLoadGap = config.getMaxJuryLoadGap();
        if (nlp != null) {
            Professeur[] nlpJury = selectNlpAwareJury(candidates, profJuryCount, minLoad, maxLoadGap, nlp);
            if (nlpJury != null) {
                return nlpJury;
            }
        }

        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                Professeur p1 = candidates.get(i);
                Professeur p2 = candidates.get(j);
                int infoCount = (encadrantIsInfo ? 1 : 0) + (isInfo(p1) ? 1 : 0) + (isInfo(p2) ? 1 : 0);
                if (infoCount >= 2) {
                    return new Professeur[]{p1, p2};
                }
            }
        }

        if (candidates.size() >= 2) {
            return new Professeur[]{candidates.get(0), candidates.get(1)};
        }
        return null;
    }

    private Professeur[] selectNlpAwareJury(List<Professeur> candidates, Map<Long, Integer> profJuryCount, int minLoad,
                                            int maxLoadGap, SujetAnalysis nlp) {
        String targetSpec = nlp.getBestSpecialite();
        boolean needEnglish = nlp.isEnglish();

        Professeur techProf = candidates.stream()
                .filter(p -> profJuryCount.getOrDefault(p.getIdp(), 0) <= minLoad + maxLoadGap)
                .filter(p -> containsIgnoreCase(p.getSpecialite(), targetSpec != null ? targetSpec : ""))
                .findFirst()
                .orElse(null);

        Professeur englishProf = null;
        if (needEnglish) {
            final Professeur selectedTechProf = techProf;
            englishProf = candidates.stream()
                    .filter(p -> profJuryCount.getOrDefault(p.getIdp(), 0) <= minLoad + maxLoadGap)
                    .filter(this::isEnglish)
                    .filter(p -> selectedTechProf == null || !p.getIdp().equals(selectedTechProf.getIdp()))
                    .findFirst()
                    .orElse(null);
        }
        if (techProf != null && englishProf != null) {
            return new Professeur[]{techProf, englishProf};
        }
        if (techProf != null && !needEnglish) {
            Professeur r2 = candidates.stream()
                    .filter(p -> !p.getIdp().equals(techProf.getIdp())).findFirst().orElse(null);
            if (r2 != null) {
                return new Professeur[]{techProf, r2};
            }
        }
        if (techProf == null && englishProf != null) {
            final Professeur selectedEnglishProf = englishProf;
            Professeur r1 = candidates.stream().filter(p -> !p.getIdp().equals(selectedEnglishProf.getIdp()))
                    .findFirst()
                    .orElse(null);
            if (r1 != null) {
                return new Professeur[]{r1, selectedEnglishProf};
            }
        }
        return null;
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
