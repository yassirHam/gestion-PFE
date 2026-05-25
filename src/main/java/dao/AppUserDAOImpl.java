package dao;

import entities.AppUser;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import util.HibernateUtil;

import java.util.List;

public class AppUserDAOImpl implements AppUserDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

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
}
