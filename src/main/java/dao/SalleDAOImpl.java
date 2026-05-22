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
    public Salle findById(Long id) {
        if (id == null) return null;
        try (Session session = sf.openSession()) {
            return session.get(Salle.class, id);
        }
    }

    @Override
    public void save(Salle salle) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.persist(salle);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
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
    public boolean deleteById(Long id) {
        if (id == null) return false;
        if (isUsedInPlanning(id)) return false;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            int affected = session.createMutationQuery("delete from Salle s where s.id_salle = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
            return affected > 0;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isUsedInPlanning(Long id) {
        if (id == null) return false;
        try (Session session = sf.openSession()) {
            Long count = session.createQuery(
                    "select count(s) from Soutenance s where s.salle.id_salle = :id", Long.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
