package dao;

import entities.Jury;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class JuryDAOImpl implements JuryDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public Jury save(Jury jury) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            // Use merge() so existing juries get updated and new ones get
            // inserted, in a single API.
            Jury merged = (Jury) session.merge(jury);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Jury> findAll() {
        try (Session session = sf.openSession()) {
            return session.createQuery(
                "SELECT DISTINCT j FROM Jury j " +
                "JOIN FETCH j.president " +
                "LEFT JOIN FETCH j.rapporteur1 " +
                "LEFT JOIN FETCH j.rapporteur2 " +
                "LEFT JOIN FETCH j.invite " +
                "LEFT JOIN FETCH j.extraMembers",
                Jury.class
            ).list();
        }
    }

    @Override
    public void deleteAll() {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("delete from Jury").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
