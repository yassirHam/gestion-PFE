package dao;

import entities.Department;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class DepartmentDAOImpl implements DepartmentDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public Department save(Department department) {
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            Department merged = (Department) s.merge(department);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return department;
        }
    }

    @Override
    public Department findById(Long id) {
        if (id == null) return null;
        try (Session s = sf.openSession()) {
            return s.get(Department.class, id);
        }
    }

    @Override
    public Department findByCode(String code) {
        if (code == null) return null;
        try (Session s = sf.openSession()) {
            return s.createQuery("from Department where lower(code) = lower(:c)", Department.class)
                    .setParameter("c", code.trim())
                    .uniqueResult();
        }
    }

    @Override
    public List<Department> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery("from Department order by name asc", Department.class).list();
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        Transaction tx = null;
        try (Session s = sf.openSession()) {
            tx = s.beginTransaction();
            s.createMutationQuery("delete from Department where id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
