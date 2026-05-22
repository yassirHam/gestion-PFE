package dao;

import entities.Approval;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class ApprovalDAOImpl implements ApprovalDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public void save(Approval approval) {
        if (approval == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.persist(approval);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public List<Approval> findByVersion(Long versionId) {
        if (versionId == null) return java.util.Collections.emptyList();
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "from Approval where version.id = :v order by occurredAt asc",
                    Approval.class)
                    .setParameter("v", versionId)
                    .list();
        }
    }

    @Override
    public List<Approval> findRecent(int limit) {
        try (Session s = sf.openSession()) {
            return s.createQuery("from Approval order by occurredAt desc", Approval.class)
                    .setMaxResults(Math.max(1, limit))
                    .list();
        }
    }
}
