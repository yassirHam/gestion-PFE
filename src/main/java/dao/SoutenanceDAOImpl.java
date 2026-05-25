package dao;

import entities.Soutenance;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class SoutenanceDAOImpl implements SoutenanceDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public void saveAll(List<Soutenance> soutenances) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            for (Soutenance s : soutenances) {
                // merge() handles detached associations (Jury, Salle, Etudiant)
                // that were loaded/saved in different sessions
                session.merge(s);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public Soutenance save(Soutenance soutenance) {
        if (soutenance == null) return null;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Soutenance merged = (Soutenance) session.merge(soutenance);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return soutenance;
        }
    }

    @Override
    public List<Soutenance> findAllWithDetails() {
        try (Session session = sf.openSession()) {
            return session.createQuery(
                "SELECT DISTINCT s FROM Soutenance s " +
                "JOIN FETCH s.etudiant " +
                "JOIN FETCH s.salle " +
                "JOIN FETCH s.jury j " +
                "JOIN FETCH j.president " +
                "LEFT JOIN FETCH j.rapporteur1 " +
                "LEFT JOIN FETCH j.rapporteur2 " +
                "LEFT JOIN FETCH j.invite " +
                "LEFT JOIN FETCH j.extraMembers " +
                "LEFT JOIN FETCH s.version " +
                "LEFT JOIN FETCH s.session " +
                "ORDER BY s.date, s.heure, s.salle.num_salle",
                Soutenance.class
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
            session.createMutationQuery("delete from Soutenance s where s.etudiant.filiere in (:filieres)")
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
            session.createMutationQuery("delete from Soutenance where ids = :id")
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
            // Delete soutenances first (FK to jury)
            session.createMutationQuery("delete from Soutenance").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
