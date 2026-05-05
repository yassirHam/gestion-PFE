package dao;

import entities.Salle;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class SalleDAOImpl implements SalleDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public List<Salle> findAll() {
        try (Session session = sf.openSession()) {
            return session.createQuery("from Salle order by num_salle", Salle.class).list();
        }
    }

    @Override
    public void saveAll(List<Salle> salles) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            for (Salle s : salles) {
                session.persist(s);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAll() {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("delete from Salle").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public long count() {
        try (Session session = sf.openSession()) {
            return session.createQuery("select count(s) from Salle s", Long.class).uniqueResult();
        }
    }
}
