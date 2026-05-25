package dao;

import entities.PlanningVersion;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class PlanningVersionDAOImpl implements PlanningVersionDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public PlanningVersion save(PlanningVersion version) {
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            PlanningVersion merged = (PlanningVersion) s.merge(version);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return version;
        }
    }

    @Override
    public PlanningVersion findCurrent(Long sessionId) {
        if (sessionId == null) return null;
        try (Session s = sf.openSession()) {
            List<PlanningVersion> list = s.createQuery(
                    "select v from PlanningVersion v " +
                    " join fetch v.session sess " +
                    " where sess.id = :sid and v.current = true " +
                    " order by v.versionNumber desc",
                    PlanningVersion.class)
                    .setParameter("sid", sessionId)
                    .setMaxResults(1)
                    .list();
            return list.isEmpty() ? null : list.get(0);
        }
    }

    @Override
    public int nextVersionNumber(Long sessionId) {
        if (sessionId == null) return 1;
        try (Session s = sf.openSession()) {
            Integer max = s.createQuery(
                    "select max(v.versionNumber) from PlanningVersion v where v.session.id = :sid",
                    Integer.class)
                    .setParameter("sid", sessionId)
                    .uniqueResult();
            return (max == null ? 0 : max) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public void setCurrent(Long sessionId, Long versionId) {
        if (sessionId == null || versionId == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery(
                    "update PlanningVersion set current = false where session.id = :sid and id <> :vid")
                    .setParameter("sid", sessionId)
                    .setParameter("vid", versionId)
                    .executeUpdate();
            s.createMutationQuery("update PlanningVersion set current = true where id = :vid")
                    .setParameter("vid", versionId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
