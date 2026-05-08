package services;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Implementation NLP via NVIDIA NIM (Llama 3.3 70B Instruct).
 */
public class NlpServiceImpl implements NlpService {

    private static final Logger LOG = Logger.getLogger(NlpServiceImpl.class.getName());
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
                .maxTokens(2048)
                .maxRetries(0)
                .timeout(java.time.Duration.ofSeconds(30))
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
    public java.util.Map<String, SujetAnalysis> analyzeSujetsBatch(List<String> sujets, List<String> specialitesDispos) {
        java.util.Map<String, SujetAnalysis> results = new java.util.HashMap<>();
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
        
        LOG.info("Batch NLP analysis pour " + sujetsToAnalyze.size() + " sujets inÃ©dits en UNE SEULE requete...");
        
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
            String response = model.generate(promptBuilder.toString());
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
                            if (!lang.equals("ag")) lang = "fr";
                            final String finalSpec = spec.toLowerCase();
                            String matchedSpec = specialitesDispos.stream()
                                    .filter(s -> s.toLowerCase().contains(finalSpec) || finalSpec.contains(s.toLowerCase()))
                                    .findFirst()
                                    .orElse(specialitesDispos.isEmpty() ? "Informatique" : specialitesDispos.get(0));
                                    
                            SujetAnalysis analysis = new SujetAnalysis(matchedSpec, lang);
                            CACHE.put(originalSujet.trim().toLowerCase(), analysis);
                            results.put(originalSujet, analysis);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Batch NLP API failed - Reason: " + e.getMessage());
        }
        for (String s : sujetsToAnalyze) {
            if (!results.containsKey(s)) {
                SujetAnalysis fallback = new SujetAnalysis(specialitesDispos.isEmpty() ? "Informatique" : specialitesDispos.get(0), "fr");
                CACHE.put(s.trim().toLowerCase(), fallback);
                results.put(s, fallback);
            }
        }
        
        return results;
    }
}

