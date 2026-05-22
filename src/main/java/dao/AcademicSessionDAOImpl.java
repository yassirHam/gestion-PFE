package dao;

import entities.AcademicSession;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class AcademicSessionDAOImpl implements AcademicSessionDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public AcademicSession save(AcademicSession session) {
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            AcademicSession merged = (AcademicSession) s.merge(session);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return session;
        }
    }

    @Override
    public AcademicSession findById(Long id) {
        if (id == null) return null;
        try (Session s = sf.openSession()) {
            return s.get(AcademicSession.class, id);
        }
    }

    @Override
    public AcademicSession findByCode(String code) {
        if (code == null) return null;
        try (Session s = sf.openSession()) {
            return s.createQuery("from AcademicSession where code = :c", AcademicSession.class)
                    .setParameter("c", code)
                    .uniqueResult();
        }
    }

    @Override
    public AcademicSession findActive() {
        try (Session s = sf.openSession()) {
            List<AcademicSession> list = s.createQuery(
                    "from AcademicSession where active = true and closed = false order by id desc",
                    AcademicSession.class)
                    .setMaxResults(1)
                    .list();
            return list.isEmpty() ? null : list.get(0);
        }
    }

    @Override
    public List<AcademicSession> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "from AcademicSession order by closed asc, active desc, id desc",
                    AcademicSession.class).list();
        }
    }

    @Override
    public void setActive(Long id) {
        if (id == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery("update AcademicSession set active = false where active = true and id <> :id")
                    .setParameter("id", id)
                    .executeUpdate();
            s.createMutationQuery("update AcademicSession set active = true, closed = false where id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void close(Long id) {
        if (id == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery("update AcademicSession set active = false, closed = true where id = :id")
                    .setParameter("id", id)
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
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery("delete from AcademicSession where id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
