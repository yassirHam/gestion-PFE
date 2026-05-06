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

    private final ChatLanguageModel model;

    public NlpServiceImpl() {
        java.util.Properties cfg = loadConfig();
        String apiKey  = cfg.getProperty("nvidia.api.key",  "MISSING_API_KEY");
        String baseUrl = cfg.getProperty("nvidia.base.url", "https://integrate.api.nvidia.com/v1");
        String model_  = cfg.getProperty("nvidia.model",   "meta/llama-3.3-70b-instruct");

        this.model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model_)
                .temperature(0.2)
                .maxTokens(256)
                .maxRetries(0)
                .timeout(java.time.Duration.ofSeconds(3)) // Timeout très court pour ne pas bloquer si l'API rame
                .build();
    }

    // Pré-remplissage du cache pour la démo afin d'éviter l'API au maximum
    static {
        // GI
        CACHE.put("development of a web application for project management", new SujetAnalysis("Développement Web", "ag"));
        CACHE.put("conception d'une application mobile de gestion de stock", new SujetAnalysis("Développement Web", "fr"));
        CACHE.put("mise en place d'une architecture microservices", new SujetAnalysis("Développement Web", "fr"));
        CACHE.put("building an e-commerce platform with spring boot and react", new SujetAnalysis("Développement Web", "ag"));
        CACHE.put("optimisation des performances d'une base de données", new SujetAnalysis("Bases de données", "fr"));
        CACHE.put("création d'un erp pour la gestion rh", new SujetAnalysis("Génie Logiciel", "fr"));
        // ID
        CACHE.put("predictive modeling for customer churn", new SujetAnalysis("Data Science", "ag"));
        CACHE.put("analyse de données et création de dashboard bi", new SujetAnalysis("Data Science", "fr"));
        CACHE.put("implémentation d'un data lake avec hadoop", new SujetAnalysis("Bases de données", "fr"));
        CACHE.put("real-time data streaming pipeline using kafka", new SujetAnalysis("Data Science", "ag"));
        CACHE.put("machine learning pour la détection de fraudes", new SujetAnalysis("Intelligence Artificielle", "fr"));
        CACHE.put("web scraping et analyse de sentiments", new SujetAnalysis("Data Science", "fr"));
        // TDIA
        CACHE.put("deep learning for medical image segmentation", new SujetAnalysis("Intelligence Artificielle", "ag"));
        CACHE.put("création d'un chatbot intelligent avec nlp", new SujetAnalysis("Intelligence Artificielle", "fr"));
        CACHE.put("reconnaissance faciale et sécurité", new SujetAnalysis("Intelligence Artificielle", "fr"));
        CACHE.put("implementation of a recommendation system", new SujetAnalysis("Intelligence Artificielle", "ag"));
        CACHE.put("transformation digitale des processus d'une entreprise", new SujetAnalysis("Génie Logiciel", "fr"));
        CACHE.put("génération de texte avec des modèles llm", new SujetAnalysis("Intelligence Artificielle", "fr"));
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
}
