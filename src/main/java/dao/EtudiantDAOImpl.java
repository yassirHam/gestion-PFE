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
        Transaction tx = null;
        try {
            session = sf.openSession();
            tx = session.beginTransaction();
            
            // Delete soutenances for this filiere first
            session.createMutationQuery("delete from Soutenance s where s.etudiant.filiere = :f")
                .setParameter("f", filiere).executeUpdate();

            // Delete affectations linked
            session.createMutationQuery(
                "delete from Affectation a where a.etudiant.filiere = :f")
                .setParameter("f", filiere).executeUpdate();
            
            // Then delete students
            session.createMutationQuery(
                "delete from Etudiant where filiere = :f")
                .setParameter("f", filiere).executeUpdate();
            
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }

    @Override
    public void deleteAll() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sf.openSession();
            tx = session.beginTransaction();
            session.createMutationQuery("delete from Soutenance").executeUpdate();
            session.createMutationQuery("delete from Affectation").executeUpdate();
            session.createMutationQuery("delete from Etudiant").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            if (session != null && session.isOpen()) session.close();
        }
    }
}