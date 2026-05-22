package util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Listener qui initialise Hibernate au démarrage de l'application
 * et le ferme proprement à l'arrêt.
 *
 * ✅ Cela évite que la SessionFactory soit créée plusieurs fois
 *    (une fois par DAO instancié), ce qui causait des DROP/CREATE répétés.
 */
@WebListener
public class AppListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("✅ Application démarrée — Hibernate SessionFactory initialisée");
        // Force l'initialisation du singleton au démarrage
        HibernateUtil.getSessionFactory();
        try {
            services.AppSettingsService.getInstance().get();
            System.out.println("✅ AppSettings chargés (ou initialisés avec valeurs par défaut)");
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de charger les AppSettings au démarrage: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("🛑 Application arrêtée — Fermeture Hibernate");
        HibernateUtil.shutdown();
    }
}