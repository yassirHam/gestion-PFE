package dao;

import entities.Etudiant;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class EtudiantDAOImpl implements EtudiantDAO {

    private SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public List<Etudiant> findAll() {
        try (Session session = sf.openSession()) {
            return session.createQuery("from Etudiant", Etudiant.class).list();
        }
    }

    @Override
    public List<Etudiant> findByFilieres(List<String> filieres) {
        try (Session session = sf.openSession()) {
            return session.createQuery(
                "from Etudiant where filiere in (:filieres)", Etudiant.class)
                .setParameterList("filieres", filieres)
                .list();
        }
    }

    @Override
    public void saveAll(List<Etudiant> list) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            for (Etudiant e : list) {
                session.persist(e);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void deleteByFiliere(String filiere) {
        Session session = null;
        org.hibernate.Transaction tx = null;
        try {
            session = sf.openSession();
            tx = session.beginTransaction();

            // Disable FK checks so we don't get constraint violations
            session.createNativeQuery("SET foreign_key_checks = 0", Void.class).executeUpdate();

            // Delete in order: Soutenance → Affectation → Etudiant
            session.createNativeQuery(
                "DELETE s FROM soutenance s " +
                "JOIN etudiant e ON s.id_etudiant = e.id_etudiant " +
                "WHERE e.filiere = :f", Void.class)
                .setParameter("f", filiere).executeUpdate();

            session.createNativeQuery(
                "DELETE a FROM affectation a " +
                "JOIN etudiant e ON a.id_etudiant = e.id_etudiant " +
                "WHERE e.filiere = :f", Void.class)
                .setParameter("f", filiere).executeUpdate();

            session.createNativeQuery(
                "DELETE FROM etudiant WHERE filiere = :f", Void.class)
                .setParameter("f", filiere).executeUpdate();

            session.createNativeQuery("SET foreign_key_checks = 1", Void.class).executeUpdate();

            tx.commit();
        } catch (Exception e) {
            try { if (tx != null) tx.rollback(); } catch (Exception ignored) {}
            e.printStackTrace();
        } finally {
            try { if (session != null) session.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void deleteAll() {
        Session session = null;
        org.hibernate.Transaction tx = null;
        try {
            session = sf.openSession();
            tx = session.beginTransaction();

            session.createNativeQuery("SET foreign_key_checks = 0", Void.class).executeUpdate();
            session.createNativeQuery("DELETE FROM soutenance", Void.class).executeUpdate();
            session.createNativeQuery("DELETE FROM affectation", Void.class).executeUpdate();
            session.createNativeQuery("DELETE FROM etudiant", Void.class).executeUpdate();
            session.createNativeQuery("SET foreign_key_checks = 1", Void.class).executeUpdate();

            tx.commit();
        } catch (Exception e) {
            try { if (tx != null) tx.rollback(); } catch (Exception ignored) {}
            e.printStackTrace();
        } finally {
            try { if (session != null) session.close(); } catch (Exception ignored) {}
        }
    }
}