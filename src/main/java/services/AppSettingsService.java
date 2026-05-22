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

    private AppSettings createDefaults() {
        AppSettings s = new AppSettings();
        s.setId(AppSettings.SINGLETON_ID);
        s.setInstitutionName("Mon Établissement");
        s.setInstitutionSubtitle("");
        s.setAcademicYear("");
        s.setDocumentTitle("Affectation des encadrants de Projet de Fin d'Études");
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
