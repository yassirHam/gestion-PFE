package dao;

import entities.Notification;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.Date;
import java.util.List;

public class NotificationDAOImpl implements NotificationDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public Notification save(Notification notification) {
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            Notification merged = (Notification) s.merge(notification);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return notification;
        }
    }

    @Override
    public Notification findById(Long id) {
        if (id == null) return null;
        try (Session s = sf.openSession()) {
            return s.get(Notification.class, id);
        }
    }

    @Override
    public List<Notification> findRecent(int limit) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "from Notification order by queuedAt desc",
                    Notification.class)
                    .setMaxResults(Math.max(1, limit))
                    .list();
        }
    }

    @Override
    public List<Notification> findBySoutenance(Long soutenanceId) {
        if (soutenanceId == null) return java.util.Collections.emptyList();
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "from Notification where soutenanceId = :s order by queuedAt desc",
                    Notification.class)
                    .setParameter("s", soutenanceId)
                    .list();
        }
    }

    @Override
    public List<Notification> findQueued() {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "from Notification where status = 'QUEUED' order by queuedAt asc",
                    Notification.class).list();
        }
    }

    @Override
    public void updateStatus(Long id, Notification.Status status, String errorMessage) {
        if (id == null || status == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            Notification n = s.get(Notification.class, id);
            if (n != null) {
                n.setStatus(status);
                if (status == Notification.Status.SENT) {
                    n.setSentAt(new Date());
                    n.setErrorMessage(null);
                } else if (status == Notification.Status.FAILED) {
                    n.setErrorMessage(errorMessage);
                }
                s.merge(n);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public int countByStatus(Notification.Status status) {
        if (status == null) return 0;
        try (Session s = sf.openSession()) {
            Long n = s.createQuery(
                    "select count(*) from Notification where status = :st",
                    Long.class)
                    .setParameter("st", status)
                    .uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
