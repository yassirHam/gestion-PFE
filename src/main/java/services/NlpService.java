package services;

import java.util.List;

/**
 * Service NLP : analyse le sujet_stage d'un étudiant
 * et détermine la spécialité technique la plus proche
 * ainsi que la langue du projet ("fr" ou "ag").
 */
public interface NlpService {

    /**
     * 
     * @param sujets             
     * @param specialitesDispos 
     * @return Map 
     */
    java.util.Map<String, SujetAnalysis> analyzeSujetsBatch(List<String> sujets, List<String> specialitesDispos);

}
