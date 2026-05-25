package dao;

import entities.AuditLog;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class AuditLogDAOImpl implements AuditLogDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public void save(AuditLog log) {
        if (log == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.persist(log);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            // Audit failures must never break the calling operation.
            e.printStackTrace();
        }
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        try (Session s = sf.openSession()) {
            return s.createQuery("from AuditLog order by occurredAt desc", AuditLog.class)
                    .setMaxResults(Math.max(1, limit))
                    .list();
        }
    }

    @Override
    public List<AuditLog> findByActor(Long actorId, int limit) {
        if (actorId == null) return java.util.Collections.emptyList();
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "from AuditLog where actorId = :a order by occurredAt desc",
                    AuditLog.class)
                    .setParameter("a", actorId)
                    .setMaxResults(Math.max(1, limit))
                    .list();
        }
    }
}
