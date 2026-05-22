package dao;

import entities.Affectation;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class AffectationDAOImpl implements AffectationDAO {

    private SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public void saveAll(List<Affectation> affectations) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            for (Affectation a : affectations) {
                session.persist(a);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public List<Affectation> findAll() {
        try (Session session = sf.openSession()) {
            return session.createQuery("from Affectation", Affectation.class).list();
        }
    }

    @Override
    public List<Affectation> findAllWithDetails() {
        try (Session session = sf.openSession()) {
            return session.createQuery(
                "SELECT a FROM Affectation a " +
                "JOIN FETCH a.etudiant " +
                "JOIN FETCH a.encadrant " +
                "ORDER BY a.encadrant.nom, a.encadrant.prenom, a.etudiant.filiere",
                Affectation.class
            ).list();
        }
    }

    @Override
    public void deleteByFilieres(List<String> filieres) {
        if (filieres == null || filieres.isEmpty()) {
            return;
        }

        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("delete from Affectation a where a.etudiant.filiere in (:filieres)")
                    .setParameterList("filieres", filieres)
                    .executeUpdate();
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
            session.createMutationQuery("delete from Affectation").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
