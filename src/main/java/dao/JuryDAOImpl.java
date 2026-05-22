package dao;

import entities.Jury;
import entities.Professeur;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.Date;
import java.util.List;

public class JuryDAOImpl implements JuryDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public Jury save(Jury jury) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            // Use merge() so existing juries (e.g. swap-member) get updated
            // and new ones get inserted, in a single API.
            Jury merged = (Jury) session.merge(jury);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Jury findById(Long id) {
        if (id == null) return null;
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "SELECT j FROM Jury j " +
                    "JOIN FETCH j.president " +
                    "LEFT JOIN FETCH j.rapporteur1 " +
                    "LEFT JOIN FETCH j.rapporteur2 " +
                    "LEFT JOIN FETCH j.invite " +
                    "WHERE j.idJury = :id",
                    Jury.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    @Override
    public List<Jury> findAll() {
        try (Session session = sf.openSession()) {
            return session.createQuery(
                "SELECT j FROM Jury j " +
                "JOIN FETCH j.president " +
                "LEFT JOIN FETCH j.rapporteur1 " +
                "LEFT JOIN FETCH j.rapporteur2 " +
                "LEFT JOIN FETCH j.invite",
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

    @Override
    public void swapMember(Long juryId, String role, Professeur replacement, Long actorId, String reason) {
        if (juryId == null || role == null || replacement == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Jury j = session.get(Jury.class, juryId);
            if (j != null) {
                if (j.contains(replacement)) {
                    // Reject duplicate-member swap; calling code is responsible for surfacing the message
                    tx.rollback();
                    return;
                }
                j.replaceMember(role, replacement);
                j.setManualOverride(true);
                j.setSwapReason(reason);
                j.setLastModifiedById(actorId);
                j.setLastModifiedAt(new Date());
                session.merge(j);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void setLocked(Long juryId, boolean locked, Long actorId) {
        if (juryId == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Jury j = session.get(Jury.class, juryId);
            if (j != null) {
                j.setLocked(locked);
                j.setLastModifiedById(actorId);
                j.setLastModifiedAt(new Date());
                session.merge(j);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
