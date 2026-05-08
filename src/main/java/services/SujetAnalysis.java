package services;

/**
 * Résultat de l'analyse NLP d'un sujet de PFE.
 */
public class SujetAnalysis {

    private final String bestSpecialite;

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
