package dao;

import entities.FichierListe;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class FichierListeDAOImpl implements FichierListeDAO {

    private SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public void save(FichierListe f) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.persist(f);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public List<FichierListe> findAll() {
        try (Session session = sf.openSession()) {
            return session.createQuery("from FichierListe order by dateUpload desc", FichierListe.class).list();
        }
    }

    @Override
    public FichierListe findByFiliere(String filiere) {
        try (Session session = sf.openSession()) {
            return session.createQuery(
                "from FichierListe where filiere = :f", FichierListe.class)
                .setParameter("f", filiere).uniqueResult();
        }
    }

    @Override
    public void deleteByFiliere(String filiere) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("delete from FichierListe where filiere = :f")
                   .setParameter("f", filiere).executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void deleteById(Long id) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("delete from FichierListe where id = :id")
                   .setParameter("id", id).executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
    
    
}