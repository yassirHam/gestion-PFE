package dao;

import entities.AppUser;
import entities.UserRole;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.Date;
import java.util.List;

public class AppUserDAOImpl implements AppUserDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public AppUser save(AppUser user) {
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            AppUser merged = (AppUser) s.merge(user);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return user;
        }
    }

    @Override
    public AppUser findById(Long id) {
        if (id == null) return null;
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select u from AppUser u " +
                    " left join fetch u.professeur " +
                    " left join fetch u.department " +
                    " where u.id = :id",
                    AppUser.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    @Override
    public AppUser findByUsername(String username) {
        if (username == null) return null;
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select u from AppUser u " +
                    " left join fetch u.professeur " +
                    " left join fetch u.department " +
                    " where lower(u.username) = lower(:n)",
                    AppUser.class)
                    .setParameter("n", username.trim())
                    .uniqueResult();
        }
    }

    @Override
    public List<AppUser> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "select u from AppUser u " +
                    " left join fetch u.department " +
                    " order by u.username asc",
                    AppUser.class).list();
        }
    }

    @Override
    public List<AppUser> findByRole(UserRole role) {
        if (role == null) return java.util.Collections.emptyList();
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "from AppUser where role = :r order by username asc",
                    AppUser.class)
                    .setParameter("r", role)
                    .list();
        }
    }

    @Override
    public long count() {
        try (Session s = sf.openSession()) {
            Long n = s.createQuery("select count(u) from AppUser u", Long.class).uniqueResult();
            return n == null ? 0 : n;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery("delete from AppUser where id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void touchLastLogin(Long id) {
        if (id == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery("update AppUser set lastLoginAt = :t where id = :id")
                    .setParameter("t", new Date())
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
