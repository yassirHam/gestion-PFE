package test;

import util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.Arrays;
import java.util.List;

public class TestDelete {
    public static void main(String[] args) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            List<String> filieres = Arrays.asList("GI");
            int deleted = session.createMutationQuery("delete from Affectation a where a.etudiant.filiere in (:filieres)")
                   .setParameterList("filieres", filieres)
                   .executeUpdate();
            System.out.println("Deleted: " + deleted);
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
