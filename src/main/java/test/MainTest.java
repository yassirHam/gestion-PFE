
package test;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import util.HibernateUtil;

public class MainTest {

    public static void main(String[] args) {
        System.out.println(">>> DÉMARRAGE DU TEST HIBERNATE...");

        try {
            // 1. Récupération de la SessionFactory (déclenche la lecture du fichier cfg.xml)
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            
            // 2. Ouverture d'une session (déclenche la création des tables si hbm2ddl.auto=update)
            Session session = sessionFactory.openSession();
            
            System.out.println(">>> CONNEXION RÉUSSIE !");
            System.out.println(">>> VÉRIFIE TA BASE DE DONNÉES MYSQL MAINTENANT.");

            // 3. Fermeture
            session.close();
            // sessionFactory.close(); // Optionnel ici

        } catch (Exception e) {
            System.err.println(">>> ÉCHEC DU TEST !");
            e.printStackTrace();
        }
    }
}