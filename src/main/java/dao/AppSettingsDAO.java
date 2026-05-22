package dao;

import entities.AppSettings;

public interface AppSettingsDAO {

    /**
     * Returns the singleton settings row (id = 1), or {@code null} if it has
     * not been initialised yet.
     */
    AppSettings findSingleton();

    /**
     * Persists the singleton settings row. Inserts if absent, updates otherwise.
     */
    void save(AppSettings settings);
}
