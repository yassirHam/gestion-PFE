package services;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Implémentation NLP via NVIDIA NIM (Llama 3.3 70B Instruct).
 *
 * NVIDIA NIM est 100% compatible avec l'API OpenAI → on utilise
 * langchain4j-open-ai en écrasant le baseUrl.
 *
 * Le modèle reçoit le sujet, la liste des spécialités dispo,
 * et retourne un JSON simple : {"specialite":"...","language":"fr|ag"}
 */
public class NlpServiceImpl implements NlpService {

    private static final Logger LOG = Logger.getLogger(NlpServiceImpl.class.getName());

    // ── Cache statique pour éviter de réinterroger l'API pour les mêmes sujets ──
    private static final Map<String, SujetAnalysis> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> VERIFICATION_CACHE = new ConcurrentHashMap<>();

    private final ChatLanguageModel model;
    private final boolean configured;

    public NlpServiceImpl() {
        java.util.Properties cfg = loadConfig();
        String apiKey  = cfg.getProperty("nvidia.api.key",  "MISSING_API_KEY");
        String baseUrl = cfg.getProperty("nvidia.base.url", "https://integrate.api.nvidia.com/v1");
        String model_  = cfg.getProperty("nvidia.model",   "meta/llama-3.3-70b-instruct");
        this.configured = apiKey != null
                && !apiKey.trim().isEmpty()
                && !"MISSING_API_KEY".equals(apiKey)
                && !"VOTRE_CLE_API_NVIDIA_ICI".equals(apiKey);

        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model_)
                .temperature(0.2)
                .maxTokens(2048)
                .maxRetries(0)
                .timeout(java.time.Duration.ofSeconds(30)) // Timeout étendu à 30s pour laisser le temps au modèle de générer le batch entier
                .build();
    }



    /** Charge config.properties depuis le classpath */
    private static java.util.Properties loadConfig() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = NlpServiceImpl.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                LOG.severe("config.properties introuvable dans le classpath ! Copiez config.properties.example.");
            }
        } catch (java.io.IOException e) {
            LOG.log(java.util.logging.Level.SEVERE, "Erreur lecture config.properties", e);
        }
        return props;
    }

    @Override
    public SujetAnalysis analyzeSujet(String sujet, List<String> specialitesDispos) {
        if (sujet == null || sujet.trim().isEmpty()) {
            return new SujetAnalysis(specialitesDispos.isEmpty() ? "Informatique" : specialitesDispos.get(0), "fr");
        }

        // Vérification dans le cache
        String cacheKey = sujet.trim().toLowerCase();
        if (CACHE.containsKey(cacheKey)) {
            LOG.info("🚀 Sujet trouvé dans le cache NLP : " + sujet);
            return CACHE.get(cacheKey);
        }

        String specialitesStr = String.join(", ", specialitesDispos);

        String prompt = "Tu es un assistant qui classe des sujets de PFE (Projet de Fin d'Études).\n" +
                "Voici le sujet d'un étudiant : \"" + sujet + "\"\n" +
                "Les spécialités disponibles des professeurs sont : [" + specialitesStr + "]\n\n" +
                "Réponds UNIQUEMENT avec un objet JSON valide sur une seule ligne, sans explications, sans markdown :\n" +
                "{\"specialite\":\"<la_spécialité_la_plus_proche>\",\"language\":\"<fr_ou_ag>\"}\n\n" +
                "Règles :\n" +
                "- Pour 'specialite' : choisis EXACTEMENT un des éléments de la liste fournie, celui qui correspond le mieux au sujet.\n" +
                "- Pour 'language' : mets 'ag' si le sujet est rédigé en anglais ou s'il contient majoritairement des termes anglais, sinon 'fr'.\n" +
                "Réponds uniquement avec le JSON.";

        try {
            // Pause plus longue (1.5 sec) pour respecter strictement les limites gratuites de NVIDIA NIM (Rate Limit 429)
            Thread.sleep(1500);
            
            String response = model.generate(prompt);
            SujetAnalysis result = parseResponse(response, specialitesDispos);
            
            // On sauvegarde dans le cache
            CACHE.put(cacheKey, result);
            return result;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "NLP API call failed for sujet: " + sujet + " - Reason: " + e.getMessage());
            // Graceful fallback: return first available specialite, default language fr
            return new SujetAnalysis(specialitesDispos.isEmpty() ? "Informatique" : specialitesDispos.get(0), "fr");
        }
    }

    /**
     * Parse la réponse JSON du LLM.
     * Format attendu: {"specialite":"...","language":"fr|ag"}
     * On fait un parsing manuel simple pour éviter une dépendance JSON.
     */
    private SujetAnalysis parseResponse(String response, List<String> specialitesDispos) {
        String specialite = null;
        String language   = "fr";

        try {
            // Extraire la valeur de "specialite"
            int sIdx = response.indexOf("\"specialite\"");
            if (sIdx >= 0) {
                int colon = response.indexOf(':', sIdx);
                int q1    = response.indexOf('"', colon + 1);
                int q2    = response.indexOf('"', q1 + 1);
                if (q1 >= 0 && q2 > q1) {
                    specialite = response.substring(q1 + 1, q2).trim();
                }
            }

            // Extraire la valeur de "language"
            int lIdx = response.indexOf("\"language\"");
            if (lIdx >= 0) {
                int colon = response.indexOf(':', lIdx);
                int q1    = response.indexOf('"', colon + 1);
                int q2    = response.indexOf('"', q1 + 1);
                if (q1 >= 0 && q2 > q1) {
                    String lang = response.substring(q1 + 1, q2).trim().toLowerCase();
                    if (lang.contains("ag") || lang.contains("eng") || lang.contains("ang")) {
                        language = "ag";
                    } else {
                        language = "fr";
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse NLP response: " + response, e);
        }

        // Validate specialite is in the list (fuzzy match)
        if (specialite != null && !specialitesDispos.isEmpty()) {
            final String finalSpe = specialite.toLowerCase();
            String matched = specialitesDispos.stream()
                    .filter(s -> s != null && s.toLowerCase().contains(finalSpe) || finalSpe.contains(s != null ? s.toLowerCase() : ""))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                specialite = matched;
            } else {
                // Exact match failed — keep the LLM's value as-is (it may still be useful)
                LOG.warning("NLP returned specialite '" + specialite + "' not in list " + specialitesDispos + ", keeping as-is.");
            }
        }

        if (specialite == null) {
            specialite = specialitesDispos.isEmpty() ? "Informatique" : specialitesDispos.get(0);
        }

        return new SujetAnalysis(specialite, language);
    }

    @Override
    public java.util.Map<String, SujetAnalysis> analyzeSujetsBatch(List<String> sujets, List<String> specialitesDispos) {
        java.util.Map<String, SujetAnalysis> results = new java.util.HashMap<>();
        
        // Filtrer les sujets valides et non en cache
        List<String> sujetsToAnalyze = new java.util.ArrayList<>();
        for (String s : sujets) {
            if (s == null || s.trim().isEmpty()) continue;
            String key = s.trim().toLowerCase();
            if (CACHE.containsKey(key)) {
                results.put(s, CACHE.get(key));
            } else if (!sujetsToAnalyze.contains(s)) {
                sujetsToAnalyze.add(s);
            }
        }
        
        if (sujetsToAnalyze.isEmpty()) {
            return results;
        }
        
        LOG.info("🚀 Batch NLP analysis pour " + sujetsToAnalyze.size() + " sujets inédits en UNE SEULE requête...");
        
        String specialitesStr = String.join(", ", specialitesDispos);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Tu es un expert qui classe des sujets de PFE.\n");
        promptBuilder.append("Pour CHAQUE sujet ci-dessous, donne la Spécialité et la Langue.\n");
        promptBuilder.append("- Spécialité: choisis EXCLUSIVEMENT l'une de ces valeurs exactes : [").append(specialitesStr).append("]\n");
        promptBuilder.append("- Langue: 'fr' (Français) ou 'ag' (Anglais).\n");
        promptBuilder.append("RÉPONDS UNIQUEMENT avec le format suivant, une ligne par sujet (séparateur = pipe |) :\n");
        promptBuilder.append("ID|Spécialité|Langue\n\n");
        promptBuilder.append("SUJETS:\n");
        
        for (int i = 0; i < sujetsToAnalyze.size(); i++) {
            promptBuilder.append(i).append("|").append(sujetsToAnalyze.get(i)).append("\n");
        }
        
        try {
            // Un seul appel pour tout le batch ! Pas de timeout ou sleep restrictif requis ici
            String response = model.generate(promptBuilder.toString());
            
            // Parsing de la réponse
            String[] lines = response.split("\n");
            for (String line : lines) {
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        if (id >= 0 && id < sujetsToAnalyze.size()) {
                            String originalSujet = sujetsToAnalyze.get(id);
                            String spec = parts[1].trim();
                            String lang = parts[2].trim().toLowerCase();
                            
                            // Nettoyage
                            if (!lang.equals("ag")) lang = "fr";
                            // Vérifier si la spé existe vraiment, sinon fallback
                            final String finalSpec = spec.toLowerCase();
                            String matchedSpec = specialitesDispos.stream()
                                    .filter(s -> s.toLowerCase().contains(finalSpec) || finalSpec.contains(s.toLowerCase()))
                                    .findFirst()
                                    .orElse(specialitesDispos.isEmpty() ? "Informatique" : specialitesDispos.get(0));
                                    
                            SujetAnalysis analysis = new SujetAnalysis(matchedSpec, lang);
                            
                            // Ajout au cache et au résultat
                            CACHE.put(originalSujet.trim().toLowerCase(), analysis);
                            results.put(originalSujet, analysis);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore ligne invalide
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Batch NLP API failed - Reason: " + e.getMessage());
        }
        
        // Fallback pour ceux qui ont échoué ou n'ont pas été bien parsés
        for (String s : sujetsToAnalyze) {
            if (!results.containsKey(s)) {
                SujetAnalysis fallback = new SujetAnalysis(specialitesDispos.isEmpty() ? "Informatique" : specialitesDispos.get(0), "fr");
                CACHE.put(s.trim().toLowerCase(), fallback);
                results.put(s, fallback);
            }
        }
        
        return results;
    }

    @Override
    public String summarizeVerificationFindings(List<String> findings) {
        if (findings == null || findings.isEmpty()) {
            return "Aucune anomalie detectee. Les fichiers generes semblent conformes aux contraintes principales.";
        }

        String source = String.join("\n", findings);
        String cacheKey = source.trim().toLowerCase();
        if (VERIFICATION_CACHE.containsKey(cacheKey)) {
            return VERIFICATION_CACHE.get(cacheKey);
        }

        if (!configured) {
            String fallback = localVerificationSummary(findings);
            VERIFICATION_CACHE.put(cacheKey, fallback);
            return fallback;
        }

        String prompt = "Tu es un assistant de controle qualite pour des fichiers de soutenances PFE.\n"
                + "A partir des constats suivants, redige une synthese courte en francais, en 2 phrases maximum.\n"
                + "Mentionne les priorites de correction sans inventer de nouvelles anomalies.\n\n"
                + source;

        try {
            String response = model.generate(prompt);
            String summary = response == null ? "" : response.trim();
            if (summary.isEmpty()) {
                summary = localVerificationSummary(findings);
            }
            VERIFICATION_CACHE.put(cacheKey, summary);
            return summary;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Verification NLP summary failed - Reason: " + e.getMessage());
            String fallback = localVerificationSummary(findings);
            VERIFICATION_CACHE.put(cacheKey, fallback);
            return fallback;
        }
    }

    private String localVerificationSummary(List<String> findings) {
        boolean hasCritical = false;
        boolean hasWarning = false;
        for (String finding : findings) {
            if (finding != null && finding.contains("CRITIQUE")) hasCritical = true;
            if (finding != null && finding.contains("ALERTE")) hasWarning = true;
        }
        if (hasCritical) {
            return "Des anomalies critiques ont ete detectees. Corrigez d'abord les conflits de planning ou donnees manquantes, puis relancez la verification.";
        }
        if (hasWarning) {
            return "Le planning est exploitable mais certaines contraintes meritent une correction. Verifiez surtout l'equite d'encadrement et les temps de repos.";
        }
        return "Aucune anomalie bloquante detectee. Les fichiers generes semblent conformes aux contraintes principales.";
    }
}
