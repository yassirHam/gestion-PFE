package dao;

import entities.ProfesseurAvailability;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class ProfesseurAvailabilityDAOImpl implements ProfesseurAvailabilityDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public ProfesseurAvailability save(ProfesseurAvailability av) {
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            ProfesseurAvailability merged = (ProfesseurAvailability) s.merge(av);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return av;
        }
    }

    @Override
    public ProfesseurAvailability findById(Long id) {
        if (id == null) return null;
        try (Session s = sf.openSession()) {
            return s.get(ProfesseurAvailability.class, id);
        }
    }

    @Override
    public List<ProfesseurAvailability> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select a from ProfesseurAvailability a " +
                    " join fetch a.professeur " +
                    " left join fetch a.session " +
                    " order by a.theDate asc",
                    ProfesseurAvailability.class).list();
        }
    }

    @Override
    public List<ProfesseurAvailability> findByProfesseur(Long profId) {
        if (profId == null) return java.util.Collections.emptyList();
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select a from ProfesseurAvailability a " +
                    " join fetch a.professeur p " +
                    " left join fetch a.session " +
                    " where p.idp = :p order by a.theDate asc",
                    ProfesseurAvailability.class)
                    .setParameter("p", profId)
                    .list();
        }
    }

    @Override
    public List<ProfesseurAvailability> findBySession(Long sessionId) {
        try (Session s = sf.openSession()) {
            String hql = "select a from ProfesseurAvailability a " +
                    " join fetch a.professeur " +
                    " left join fetch a.session sess " +
                    (sessionId == null
                        ? " where sess is null "
                        : " where sess.id = :s or sess is null ") +
                    " order by a.theDate asc";
            var q = s.createQuery(hql, ProfesseurAvailability.class);
            if (sessionId != null) q.setParameter("s", sessionId);
            return q.list();
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery("delete from ProfesseurAvailability where id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
