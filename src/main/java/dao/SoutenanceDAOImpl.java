package dao;

import entities.Etudiant;
import entities.Soutenance;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;
import java.util.Locale;

public class SoutenanceDAOImpl implements SoutenanceDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public void saveAll(List<Soutenance> soutenances) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            for (Soutenance s : soutenances) {
                normalizeBinomeSubject(session, s);
                // merge() handles detached associations (Jury, Salle, Etudiant)
                // that were loaded/saved in different sessions
                session.merge(s);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    private void normalizeBinomeSubject(Session session, Soutenance soutenance) {
        Etudiant etudiant = soutenance.getEtudiant();
        if (etudiant == null || !etudiant.hasBinome()) {
            return;
        }

        Etudiant managedEtudiant = etudiant.getIde() != null ? session.get(Etudiant.class, etudiant.getIde()) : null;
        Etudiant partner = session.createQuery("from Etudiant where cne = :cne", Etudiant.class)
                .setParameter("cne", etudiant.getBinome_cne())
                .uniqueResult();

        if (managedEtudiant == null || partner == null) {
            return;
        }

        String commonSubject = chooseProjectSubject(managedEtudiant, partner);
        if (!commonSubject.isEmpty()) {
            managedEtudiant.setSujet_stage(commonSubject);
            partner.setSujet_stage(commonSubject);
            etudiant.setSujet_stage(commonSubject);
        }
    }

    private String chooseProjectSubject(Etudiant first, Etudiant second) {
        String firstSubject = safe(first.getSujet_stage());
        String secondSubject = safe(second.getSujet_stage());
        if (!firstSubject.isEmpty() && !isDefaultSubject(firstSubject)) {
            return firstSubject;
        }
        if (!secondSubject.isEmpty() && !isDefaultSubject(secondSubject)) {
            return secondSubject;
        }
        if (!firstSubject.isEmpty()) {
            return firstSubject;
        }
        return secondSubject;
    }

    private boolean isDefaultSubject(String subject) {
        return subject.toLowerCase(Locale.ROOT).contains("projet de fin d");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public List<Soutenance> findAllWithDetails() {
        try (Session session = sf.openSession()) {
            return session.createQuery(
                "SELECT s FROM Soutenance s " +
                "JOIN FETCH s.etudiant " +
                "JOIN FETCH s.salle " +
                "JOIN FETCH s.jury j " +
                "JOIN FETCH j.president " +
                "LEFT JOIN FETCH j.rapporteur1 " +
                "LEFT JOIN FETCH j.rapporteur2 " +
                "ORDER BY s.date, s.heure, s.salle.num_salle",
                Soutenance.class
            ).list();
        }
    }

    @Override
    public void deleteByFilieres(List<String> filieres) {
        if (filieres == null || filieres.isEmpty()) {
            return;
        }

        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("delete from Soutenance s where s.etudiant.filiere in (:filieres)")
                    .setParameterList("filieres", filieres)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAll() {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            // Delete soutenances first (FK to jury)
            session.createMutationQuery("delete from Soutenance").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
