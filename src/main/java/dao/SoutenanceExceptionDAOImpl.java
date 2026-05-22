package dao;

import entities.SoutenanceException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.Date;
import java.util.List;

public class SoutenanceExceptionDAOImpl implements SoutenanceExceptionDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public SoutenanceException save(SoutenanceException exc) {
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            SoutenanceException merged = (SoutenanceException) s.merge(exc);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return exc;
        }
    }

    @Override
    public SoutenanceException findById(Long id) {
        if (id == null) return null;
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select e from SoutenanceException e " +
                    " join fetch e.soutenance s " +
                    " join fetch s.etudiant " +
                    " where e.id = :id",
                    SoutenanceException.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    @Override
    public List<SoutenanceException> findOpen() {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select e from SoutenanceException e " +
                    " join fetch e.soutenance s " +
                    " join fetch s.etudiant " +
                    " where e.status in ('OPEN','IN_PROGRESS') " +
                    " order by e.reportedAt desc",
                    SoutenanceException.class).list();
        }
    }

    @Override
    public List<SoutenanceException> findBySoutenance(Long soutenanceId) {
        if (soutenanceId == null) return java.util.Collections.emptyList();
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select e from SoutenanceException e " +
                    " join fetch e.soutenance s " +
                    " where s.ids = :sid order by e.reportedAt desc",
                    SoutenanceException.class)
                    .setParameter("sid", soutenanceId)
                    .list();
        }
    }

    @Override
    public List<SoutenanceException> findRecent(int limit) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select e from SoutenanceException e " +
                    " join fetch e.soutenance s " +
                    " join fetch s.etudiant " +
                    " order by e.reportedAt desc",
                    SoutenanceException.class)
                    .setMaxResults(Math.max(1, limit))
                    .list();
        }
    }

    @Override
    public void updateStatus(Long id, SoutenanceException.ResolutionStatus status,
                             String resolutionNotes, Long resolvedById) {
        if (id == null || status == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            SoutenanceException ex = s.get(SoutenanceException.class, id);
            if (ex != null) {
                ex.setStatus(status);
                if (resolutionNotes != null) ex.setResolutionNotes(resolutionNotes);
                if (status == SoutenanceException.ResolutionStatus.RESOLVED
                        || status == SoutenanceException.ResolutionStatus.IGNORED) {
                    ex.setResolvedAt(new Date());
                    ex.setResolvedById(resolvedById);
                }
                s.merge(ex);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
