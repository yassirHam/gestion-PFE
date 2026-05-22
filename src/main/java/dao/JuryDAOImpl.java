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
            session.persist(jury);
            tx.commit();
            return jury;
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
                "SELECT j FROM Jury j " +
                "JOIN FETCH j.president " +
                "LEFT JOIN FETCH j.rapporteur1 " +
                "LEFT JOIN FETCH j.rapporteur2",
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
