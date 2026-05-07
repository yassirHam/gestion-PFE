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

    /**
     * Analyse une liste de sujets en un seul appel API (Batching) pour éviter le rate limit.
     * @param sujets             La liste des sujets à analyser
     * @param specialitesDispos  Liste des spécialités
     * @return Map associant chaque sujet à son analyse
     */
    java.util.Map<String, SujetAnalysis> analyzeSujetsBatch(List<String> sujets, List<String> specialitesDispos);

    /**
     * Resume les anomalies detectees par le verificateur des fichiers generes.
     * Le retour reste optionnel: en cas d'indisponibilite API, l'implementation
     * doit fournir une synthese locale courte.
     */
    String summarizeVerificationFindings(List<String> findings);
}
