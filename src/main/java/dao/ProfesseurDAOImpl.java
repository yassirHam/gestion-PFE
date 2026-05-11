package dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import entities.Professeur;
import util.HibernateUtil;

public class ProfesseurDAOImpl implements ProfesseurDAO{
	
	private SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

	@Override
	public List<Professeur> findAll() {
		// TODO Auto-generated method stub
		Session session = sessionFactory.openSession();
        List<Professeur> list = session
                .createQuery("from Professeur", Professeur.class)
                .list();
        session.close();
        return list;
	}

	@Override
	public void saveAll(List<Professeur> list) {
		// TODO Auto-generated method stub
		Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();

        for (Professeur p : list) {
            session.merge(p);
        }

        tx.commit();
        session.close();
		
	}

	@Override
	public void deleteAll() {
		// TODO Auto-generated method stub
		Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();

        session.createMutationQuery("delete from Professeur").executeUpdate();

        tx.commit();
        session.close();
		
	}

}
