package util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

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
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("🛑 Application arrêtée — Fermeture Hibernate");
        HibernateUtil.shutdown();
    }
}