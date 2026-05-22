package dao;

import entities.AuditLog;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.Date;
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
    public AuditLog findById(Long id) {
        if (id == null) return null;
        try (Session s = sf.openSession()) {
            return s.get(AuditLog.class, id);
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

    @Override
    public List<AuditLog> findByTarget(String targetType, Long targetId) {
        if (targetType == null) return java.util.Collections.emptyList();
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "from AuditLog where targetType = :t and targetId = :i order by occurredAt desc",
                    AuditLog.class)
                    .setParameter("t", targetType)
                    .setParameter("i", targetId)
                    .list();
        }
    }

    @Override
    public List<AuditLog> findBetween(Date from, Date to, int limit) {
        try (Session s = sf.openSession()) {
            StringBuilder hql = new StringBuilder("from AuditLog where 1=1 ");
            if (from != null) hql.append(" and occurredAt >= :f ");
            if (to != null)   hql.append(" and occurredAt <= :t ");
            hql.append(" order by occurredAt desc");
            var q = s.createQuery(hql.toString(), AuditLog.class);
            if (from != null) q.setParameter("f", from);
            if (to != null)   q.setParameter("t", to);
            q.setMaxResults(Math.max(1, limit));
            return q.list();
        }
    }
}
