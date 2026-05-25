package dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import entities.Professeur;
import util.HibernateUtil;

public class ProfesseurDAOImpl implements ProfesseurDAO {

    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    @Override
    public List<Professeur> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session
                    .createQuery("from Professeur order by nom, prenom", Professeur.class)
                    .list();
        }
    }

    @Override
    public Professeur findById(Long id) {
        if (id == null) return null;
        try (Session session = sessionFactory.openSession()) {
            return session.get(Professeur.class, id);
        }
    }

    @Override
    public Professeur save(Professeur prof) {
        if (prof == null) return null;
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            Professeur merged = (Professeur) session.merge(prof);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return prof;
        }
    }

    @Override
    public void saveAll(List<Professeur> list) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                for (Professeur p : list) {
                    session.merge(p);
                }
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deleteAll() {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.createMutationQuery("delete from Professeur").executeUpdate();
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setExcluded(Long profId, boolean excluded, String reason, Long actorId) {
        if (profId == null) return;
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            Professeur p = session.get(Professeur.class, profId);
            if (p != null) {
                p.setExcluded(excluded);
                p.setExclusionReason(excluded ? reason : null);
                session.merge(p);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
