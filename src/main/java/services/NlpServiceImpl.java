package services;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

import entities.AppSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * NLP service backed by an OpenAI-compatible chat model (NVIDIA NIM, OpenAI,
 * OpenRouter, ...). The service is fully <strong>optional</strong>:
 *
 * <ul>
 *   <li>When the user has not enabled NLP (or no API key is configured), no
 *       network call is made and a deterministic fallback is returned.</li>
 *   <li>When NLP is enabled but the remote call fails (network down, invalid
 *       key, rate-limit), the service degrades gracefully to the same
 *       fallback so the planning algorithm always has an answer.</li>
 * </ul>
 *
 * <p>Configuration lives in {@link AppSettings} and can be edited via the
 * settings page at <code>/settings.do</code>. The legacy
 * <code>config.properties</code> file is still read as a backup, but it is
 * no longer required.</p>
 */
public class NlpServiceImpl implements NlpService {

    private static final Logger LOG = Logger.getLogger(NlpServiceImpl.class.getName());
    private static final Map<String, SujetAnalysis> CACHE = new ConcurrentHashMap<>();

    public NlpServiceImpl() {
        // No eager client creation: the model is built lazily so that disabling
        // NLP at runtime via the settings page takes effect immediately.
    }

    /**
     * Resolves the NLP configuration from {@link AppSettings} first, then
     * falls back to the legacy <code>config.properties</code> file. Returns
     * {@code null} when no usable configuration is available.
     */
    private NlpConfig resolveConfig() {
        try {
            AppSettings settings = AppSettingsService.getInstance().get();
            if (settings != null && settings.isNlpEnabled()
                    && settings.getNlpApiKey() != null && !settings.getNlpApiKey().isEmpty()) {
                String baseUrl = settings.getNlpBaseUrl() == null || settings.getNlpBaseUrl().isEmpty()
                        ? "https://integrate.api.nvidia.com/v1"
                        : settings.getNlpBaseUrl();
                String model = settings.getNlpModel() == null || settings.getNlpModel().isEmpty()
                        ? "meta/llama-3.3-70b-instruct"
                        : settings.getNlpModel();
                return new NlpConfig(settings.getNlpApiKey(), baseUrl, model);
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "AppSettings unavailable, will try legacy config.properties", e);
        }

        // Legacy fallback: only used when AppSettings does not have NLP enabled.
        java.util.Properties cfg = loadLegacyConfig();
        String apiKey = cfg.getProperty("nvidia.api.key");
        if (apiKey == null || apiKey.isEmpty() || "MISSING_API_KEY".equals(apiKey)
                || "VOTRE_CLE_API_NVIDIA_ICI".equals(apiKey)) {
            return null;
        }
        String baseUrl = cfg.getProperty("nvidia.base.url", "https://integrate.api.nvidia.com/v1");
        String model = cfg.getProperty("nvidia.model", "meta/llama-3.3-70b-instruct");
        return new NlpConfig(apiKey, baseUrl, model);
    }

    private ChatLanguageModel buildModel(NlpConfig cfg) {
        return OpenAiChatModel.builder()
                .apiKey(cfg.apiKey)
                .baseUrl(cfg.baseUrl)
                .modelName(cfg.model)
                .temperature(0.2)
                .maxTokens(2048)
                .maxRetries(0)
                .timeout(java.time.Duration.ofSeconds(30))
                .build();
    }

    /** Reads the legacy config.properties, never throws. */
    private static java.util.Properties loadLegacyConfig() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = NlpServiceImpl.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (java.io.IOException e) {
            LOG.log(Level.FINE, "config.properties unreadable (ignored): " + e.getMessage());
        }
        return props;
    }

    @Override
    public Map<String, SujetAnalysis> analyzeSujetsBatch(List<String> sujets, List<String> specialitesDispos) {
        Map<String, SujetAnalysis> results = new HashMap<>();
        if (sujets == null || sujets.isEmpty()) return results;

        List<String> safeSpecs = new ArrayList<>();
        if (specialitesDispos != null) {
            for (String s : specialitesDispos) if (s != null && !s.trim().isEmpty()) safeSpecs.add(s.trim());
        }
        if (safeSpecs.isEmpty()) safeSpecs.add("General");

        // First pass: serve from cache + identify unknown sujets.
        List<String> sujetsToAnalyze = new ArrayList<>();
        for (String s : sujets) {
            if (s == null || s.trim().isEmpty()) continue;
            String key = s.trim().toLowerCase(Locale.ROOT);
            if (CACHE.containsKey(key)) {
                results.put(s, CACHE.get(key));
            } else if (!sujetsToAnalyze.contains(s)) {
                sujetsToAnalyze.add(s);
            }
        }
        if (sujetsToAnalyze.isEmpty()) return results;

        NlpConfig cfg = resolveConfig();
        if (cfg == null) {
            // NLP disabled or unconfigured: fill every sujet with the heuristic.
            LOG.fine("NLP disabled or unconfigured; using heuristic fallback for "
                    + sujetsToAnalyze.size() + " sujet(s).");
            applyFallback(sujetsToAnalyze, safeSpecs, results);
            return results;
        }

        // NLP enabled: try one batch call, gracefully fall back on any error.
        LOG.info("Running NLP batch analysis for " + sujetsToAnalyze.size() + " sujet(s)...");
        try {
            ChatLanguageModel model = buildModel(cfg);
            String response = model.generate(buildPrompt(sujetsToAnalyze, safeSpecs));
            parseResponse(response, sujetsToAnalyze, safeSpecs, results);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Batch NLP API failed, using heuristic fallback: " + e.getMessage());
        }

        // Anything the model didn't return → heuristic fallback.
        List<String> missing = new ArrayList<>();
        for (String s : sujetsToAnalyze) {
            if (!results.containsKey(s)) missing.add(s);
        }
        if (!missing.isEmpty()) {
            applyFallback(missing, safeSpecs, results);
        }
        return results;
    }

    private String buildPrompt(List<String> sujetsToAnalyze, List<String> specialitesDispos) {
        String specialitesStr = String.join(", ", specialitesDispos);
        StringBuilder p = new StringBuilder();
        p.append("Tu es un expert qui classe des sujets de PFE.\n");
        p.append("Pour CHAQUE sujet ci-dessous, donne le Module Enseigne et la Langue.\n");
        p.append("- Module Enseigne: choisis EXCLUSIVEMENT l'une de ces valeurs exactes : [")
         .append(specialitesStr).append("]\n");
        p.append("- Langue: 'fr' (Francais) ou 'ag' (Anglais).\n");
        p.append("REPONDS UNIQUEMENT avec le format suivant, une ligne par sujet (separateur = pipe |) :\n");
        p.append("ID|Module Enseigne|Langue\n\n");
        p.append("SUJETS:\n");
        for (int i = 0; i < sujetsToAnalyze.size(); i++) {
            p.append(i).append("|").append(sujetsToAnalyze.get(i)).append("\n");
        }
        return p.toString();
    }

    private void parseResponse(String response, List<String> sujets, List<String> specialitesDispos,
                               Map<String, SujetAnalysis> results) {
        if (response == null) return;
        for (String line : response.split("\n")) {
            String[] parts = line.split("\\|");
            if (parts.length < 3) continue;
            try {
                int id = Integer.parseInt(parts[0].trim());
                if (id < 0 || id >= sujets.size()) continue;
                String original = sujets.get(id);
                String spec = parts[1].trim().toLowerCase(Locale.ROOT);
                String lang = parts[2].trim().toLowerCase(Locale.ROOT);
                if (!"ag".equals(lang)) lang = "fr";

                String matched = specialitesDispos.stream()
                        .filter(s -> s.toLowerCase(Locale.ROOT).contains(spec)
                                || spec.contains(s.toLowerCase(Locale.ROOT)))
                        .findFirst()
                        .orElse(specialitesDispos.get(0));

                SujetAnalysis analysis = new SujetAnalysis(matched, lang);
                CACHE.put(original.trim().toLowerCase(Locale.ROOT), analysis);
                results.put(original, analysis);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /**
     * Heuristic fallback that picks the most likely speciality based on simple
     * keyword overlap, and detects English by counting common stop-words. This
     * keeps the planning algorithm working even without any AI service.
     */
    private void applyFallback(List<String> sujets, List<String> specialitesDispos,
                               Map<String, SujetAnalysis> results) {
        String defaultSpec = specialitesDispos.get(0);
        for (String sujet : sujets) {
            String lower = sujet == null ? "" : sujet.toLowerCase(Locale.ROOT);

            // Pick the speciality whose name has the highest token overlap.
            String bestSpec = defaultSpec;
            int bestScore = 0;
            for (String spec : specialitesDispos) {
                int score = tokenOverlap(lower, spec.toLowerCase(Locale.ROOT));
                if (score > bestScore) {
                    bestScore = score;
                    bestSpec = spec;
                }
            }

            String lang = looksEnglish(lower) ? "ag" : "fr";
            SujetAnalysis analysis = new SujetAnalysis(bestSpec, lang);
            CACHE.put(sujet.trim().toLowerCase(Locale.ROOT), analysis);
            results.put(sujet, analysis);
        }
    }

    private int tokenOverlap(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0;
        int count = 0;
        for (String token : b.split("[\\s,;/]+")) {
            if (token.length() < 3) continue;
            if (a.contains(token)) count++;
        }
        return count;
    }

    private boolean looksEnglish(String text) {
        if (text == null || text.isEmpty()) return false;
        String[] englishMarkers = {" the ", " and ", " with ", " using ", " based ",
                " for ", " of ", " a ", " an ", " on ", " in ", " to "};
        int hits = 0;
        String padded = " " + text + " ";
        for (String m : englishMarkers) {
            if (padded.contains(m)) hits++;
        }
        return hits >= 3;
    }

    private static final class NlpConfig {
        final String apiKey;
        final String baseUrl;
        final String model;
        NlpConfig(String apiKey, String baseUrl, String model) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
        }
    }
}
