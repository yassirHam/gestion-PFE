package services;

/**
 * Résultat de l'analyse NLP d'un sujet de PFE.
 */
public class SujetAnalysis {

    /** Spécialité technique la plus proche parmi celles dispo (ex: "Réseaux", "IA", "Web") */
    private final String bestSpecialite;

    /** Langue du sujet : "ag" (anglais) ou "fr" (français) */
    private final String language;

    public SujetAnalysis(String bestSpecialite, String language) {
        this.bestSpecialite = bestSpecialite;
        this.language = language;
    }

    public String getBestSpecialite() { return bestSpecialite; }
    public String getLanguage()       { return language; }
    public boolean isEnglish()        { return "ag".equalsIgnoreCase(language); }

    @Override
    public String toString() {
        return "SujetAnalysis{specialite='" + bestSpecialite + "', language='" + language + "'}";
    }
}
