package dao;

import entities.Affectation;
import entities.LifecycleState;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.Date;
import java.util.List;

public class AffectationDAOImpl implements AffectationDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

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
    public Affectation findById(Long id) {
        if (id == null) return null;
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "select a from Affectation a " +
                    " join fetch a.etudiant " +
                    " join fetch a.encadrant " +
                    " left join fetch a.session " +
                    " where a.ida = :id",
                    Affectation.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    @Override
    public Affectation update(Affectation affectation) {
        if (affectation == null) return null;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Affectation merged = (Affectation) session.merge(affectation);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return affectation;
        }
    }

    @Override
    public void updateLifecycleState(Long id, LifecycleState state, Long actorId) {
        if (id == null || state == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Affectation a = session.get(Affectation.class, id);
            if (a != null) {
                a.setLifecycleState(state);
                a.setLastModifiedById(actorId);
                a.setLastModifiedAt(new Date());
                if (state == LifecycleState.VALIDATED) {
                    a.setValidatedById(actorId);
                    a.setValidatedAt(new Date());
                }
                session.merge(a);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void setLocked(Long id, boolean locked, Long actorId) {
        if (id == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Affectation a = session.get(Affectation.class, id);
            if (a != null) {
                a.setLocked(locked);
                a.setLastModifiedById(actorId);
                a.setLastModifiedAt(new Date());
                session.merge(a);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
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
    public void deleteById(Long id) {
        if (id == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("delete from Affectation where ida = :id")
                    .setParameter("id", id)
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
