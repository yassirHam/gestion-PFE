package dao;

import entities.AppSettings;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

public class AppSettingsDAOImpl implements AppSettingsDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public AppSettings findSingleton() {
        try (Session session = sf.openSession()) {
            return session.get(AppSettings.class, AppSettings.SINGLETON_ID);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void save(AppSettings settings) {
        if (settings == null) return;
        if (settings.getId() == null) {
            settings.setId(AppSettings.SINGLETON_ID);
        }
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.merge(settings);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception re) {
                    System.err.println("Rollback failed: " + re.getMessage());
                }
            }
            e.printStackTrace();
            throw new RuntimeException("Error saving AppSettings", e);
        }
    }
}
