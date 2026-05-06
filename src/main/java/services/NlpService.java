package services;

import java.util.List;

/**
 * Service NLP : analyse le sujet_stage d'un étudiant
 * et détermine la spécialité technique la plus proche
 * ainsi que la langue du projet ("fr" ou "ag").
 */
public interface NlpService {

    /**
     * @param sujet              Le titre/sujet de stage de l'étudiant
     * @param specialitesDispos  Liste des spécialités des profs disponibles
     * @return SujetAnalysis(bestSpecialite, language)
     */
    SujetAnalysis analyzeSujet(String sujet, List<String> specialitesDispos);
}
