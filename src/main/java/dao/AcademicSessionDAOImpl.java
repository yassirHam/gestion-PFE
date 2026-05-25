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
}
