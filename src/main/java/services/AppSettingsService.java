package services;

import dao.AppSettingsDAO;
import dao.AppSettingsDAOImpl;
import entities.AppSettings;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Centralised access to {@link AppSettings}. Caches the singleton in memory
 * to avoid hitting the database on every request.
 *
 * <p>Use {@link #get()} for read access and {@link #update} when persisting
 * changes from the settings UI. The cache is automatically refreshed after
 * a successful save.</p>
 */
public final class AppSettingsService {

    private static final Logger LOG = Logger.getLogger(AppSettingsService.class.getName());

    private static final AppSettingsService INSTANCE = new AppSettingsService(new AppSettingsDAOImpl());

    private final AppSettingsDAO dao;
    private final AtomicReference<AppSettings> cache = new AtomicReference<>();

    AppSettingsService(AppSettingsDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public static AppSettingsService getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the current settings, loading them from the database (and
     * creating defaults if needed) on first access.
     */
    public AppSettings get() {
        AppSettings cached = cache.get();
        if (cached != null) return cached;
        synchronized (cache) {
            cached = cache.get();
            if (cached != null) return cached;
            try {
                AppSettings loaded = dao.findSingleton();
                if (loaded == null) {
                    loaded = createDefaults();
                    dao.save(loaded);
                }
                cache.set(loaded);
                return loaded;
            } catch (Exception e) {
                LOG.warning("Could not load AppSettings, falling back to defaults: " + e.getMessage());
                AppSettings fallback = createDefaults();
                cache.set(fallback);
                return fallback;
            }
        }
    }

    /**
     * Persists the supplied settings, overwriting whatever was stored before,
     * and refreshes the cache.
     */
    public AppSettings update(AppSettings updated) {
        if (updated == null) return get();
        if (updated.getId() == null) updated.setId(AppSettings.SINGLETON_ID);
        dao.save(updated);
        cache.set(updated);
        return updated;
    }

    public void invalidate() {
        cache.set(null);
    }

    // ─── Planning configuration persistence ────────────────────────────────

    /**
     * Build a {@link PlanningConfig} from the persisted {@link AppSettings}
     * fields. Returns the application defaults when nothing was saved yet.
     */
    public PlanningConfig loadPlanningConfig() {
        AppSettings s = get();
        PlanningConfig.Builder b = PlanningConfig.defaults().toBuilder();
        if (s.getPlanningNumberOfDays() != null)    b.numberOfDays(s.getPlanningNumberOfDays());
        if (s.getPlanningStartDate() != null)       b.startDate(s.getPlanningStartDate());
        if (s.getPlanningStartHour() != null)       b.startHourMorning(s.getPlanningStartHour());
        if (s.getPlanningEndHour() != null)         b.endHourMorning(s.getPlanningEndHour());
        if (s.getPlanningDurationMinutes() != null) b.soutenanceDurationMinutes(s.getPlanningDurationMinutes());
        if (s.getPlanningBreakMinutes() != null)    b.breakBetweenMinutes(s.getPlanningBreakMinutes());

        ConstraintSet cs = ConstraintSet.defaults();
        applyConstraintsJson(cs, s.getPlanningConstraintsJson());
        b.constraints(cs);
        // Afternoon / enable flags are stored as pseudo-constraints in the JSON.
        applyTimeFlagsFromJson(b, s.getPlanningConstraintsJson());
        return b.build();
    }

    public void savePlanningConfig(PlanningConfig cfg) {
        if (cfg == null) return;
        AppSettings s = get();
        s.setPlanningNumberOfDays(cfg.getNumberOfDays());
        s.setPlanningStartDate(cfg.getStartDate() == null ? null : cfg.getStartDate().toString());
        s.setPlanningStartHour(cfg.getStartHourMorning());
        s.setPlanningEndHour(cfg.getEndHourMorning());
        s.setPlanningDurationMinutes(cfg.getSoutenanceDurationMinutes());
        s.setPlanningBreakMinutes(cfg.getBreakBetweenMinutes());
        s.setPlanningConstraintsJson(serializeFullConfig(cfg));
        update(s);
    }

    /** Serialise constraints + AM/PM time flags into one JSON object. */
    private static String serializeFullConfig(PlanningConfig cfg) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"morningEnabled\":").append(cfg.isMorningEnabled()).append(",");
        sb.append("\"afternoonEnabled\":").append(cfg.isAfternoonEnabled()).append(",");
        sb.append("\"startHourAfternoon\":").append(cfg.getStartHourAfternoon()).append(",");
        sb.append("\"endHourAfternoon\":").append(cfg.getEndHourAfternoon()).append(",");
        sb.append("\"constraints\":");
        sb.append(serializeConstraints(cfg.getConstraints()));
        sb.append("}");
        return sb.toString();
    }

    private static void applyTimeFlagsFromJson(PlanningConfig.Builder b, String json) {
        if (json == null || json.isBlank()) return;
        try {
            b.morningEnabled(!json.contains("\"morningEnabled\":false"));
            b.afternoonEnabled(!json.contains("\"afternoonEnabled\":false"));
            int pmStart = extractInt(json, "startHourAfternoon", 14);
            int pmEnd   = extractInt(json, "endHourAfternoon",   18);
            b.startHourAfternoon(pmStart);
            b.endHourAfternoon(pmEnd);
        } catch (Exception ignored) {}
    }

    private static int extractInt(String json, String key, int def) {
        String marker = "\"" + key + "\":";
        int idx = json.indexOf(marker);
        if (idx < 0) return def;
        int start = idx + marker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return def; }
    }

    /**
     * Minimal JSON serialiser for the constraint set: a top-level JSON object
     * with one entry per constraint id mapping to {value, priority}. Avoids
     * pulling a JSON dependency for one small payload.
     */
    private static String serializeConstraints(ConstraintSet cs) {
        if (cs == null) return null;
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Constraint c : cs.asList()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(c.getId())).append('"').append(':');
            sb.append("{\"value\":\"").append(escape(c.getValue() == null ? "" : c.getValue())).append('"');
            sb.append(",\"priority\":\"").append(c.getPriority().name()).append("\"}");
        }
        sb.append('}');
        return sb.toString();
    }

    private static void applyConstraintsJson(ConstraintSet cs, String json) {
        if (cs == null || json == null || json.isBlank()) return;
        // The JSON may be a flat constraints object or the new wrapper format
        // {"morningEnabled":…,"constraints":{…}}. Extract the inner object.
        String inner = json;
        int cIdx = json.indexOf("\"constraints\":");
        if (cIdx >= 0) {
            int braceOpen = json.indexOf('{', cIdx + 14);
            if (braceOpen >= 0) {
                int depth = 0, end = braceOpen;
                for (; end < json.length(); end++) {
                    if (json.charAt(end) == '{') depth++;
                    else if (json.charAt(end) == '}') { depth--; if (depth == 0) break; }
                }
                inner = json.substring(braceOpen, end + 1);
            }
        }
        // Very small bespoke parser for {"id":{"value":"…","priority":"HARD"},…}
        try {
            int i = 0;
            while (i < inner.length()) {
                int idStart = inner.indexOf('"', i);
                if (idStart < 0) break;
                int idEnd = inner.indexOf('"', idStart + 1);
                if (idEnd < 0) break;
                String id = inner.substring(idStart + 1, idEnd);

                int valKey = inner.indexOf("\"value\"", idEnd);
                int priKey = inner.indexOf("\"priority\"", idEnd);
                if (valKey < 0 || priKey < 0) break;

                int valQuote = inner.indexOf('"', inner.indexOf(':', valKey) + 1);
                int valEnd   = inner.indexOf('"', valQuote + 1);
                String value = inner.substring(valQuote + 1, valEnd);

                int priQuote = inner.indexOf('"', inner.indexOf(':', priKey) + 1);
                int priEnd   = inner.indexOf('"', priQuote + 1);
                String priority = inner.substring(priQuote + 1, priEnd);

                ConstraintPriority p;
                try { p = ConstraintPriority.valueOf(priority); }
                catch (Exception e) { p = ConstraintPriority.SOFT; }
                cs.update(id, value, p);

                int closeBrace = inner.indexOf('}', priEnd);
                if (closeBrace < 0) break;
                i = closeBrace + 1;
            }
        } catch (Exception e) {
            LOG.warning("Could not parse persisted planning constraints: " + e.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.toString();
    }

    private AppSettings createDefaults() {
        AppSettings s = new AppSettings();
        s.setId(AppSettings.SINGLETON_ID);
        s.setInstitutionName("Mon Établissement");
        s.setInstitutionSubtitle("");
        s.setAcademicYear("");
        s.setDocumentTitleAffectation("Affectation des encadrants de Projet de Fin d'Études");
        s.setDocumentTitlePlanning("Planning des soutenances des Projets de Fin d'Études");
        s.setStorageMode(AppSettings.StorageMode.LOCAL);
        s.setLocalStoragePath(System.getProperty("user.home")
                + java.io.File.separator + "plannings_history");
        s.setNlpEnabled(false);
        s.setNlpBaseUrl("https://integrate.api.nvidia.com/v1");
        s.setNlpModel("meta/llama-3.3-70b-instruct");
        s.setS3PathStyleAccess(true);
        return s;
    }
}
