package dao;

import entities.Jury;
import entities.Salle;
import entities.Soutenance;
import entities.SoutenanceStatus;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.Date;
import java.util.List;

public class SoutenanceDAOImpl implements SoutenanceDAO {

    private final SessionFactory sf = HibernateUtil.getSessionFactory();

    @Override
    public void saveAll(List<Soutenance> soutenances) {
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            for (Soutenance s : soutenances) {
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

    @Override
    public Soutenance save(Soutenance soutenance) {
        if (soutenance == null) return null;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Soutenance merged = (Soutenance) session.merge(soutenance);
            tx.commit();
            return merged;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return soutenance;
        }
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
                "LEFT JOIN FETCH j.invite " +
                "LEFT JOIN FETCH s.version " +
                "LEFT JOIN FETCH s.session " +
                "ORDER BY s.date, s.heure, s.salle.num_salle",
                Soutenance.class
            ).list();
        }
    }

    @Override
    public Soutenance findById(Long id) {
        if (id == null) return null;
        try (Session session = sf.openSession()) {
            return session.get(Soutenance.class, id);
        }
    }

    @Override
    public Soutenance findByIdWithDetails(Long id) {
        if (id == null) return null;
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "SELECT s FROM Soutenance s " +
                    "JOIN FETCH s.etudiant " +
                    "JOIN FETCH s.salle " +
                    "JOIN FETCH s.jury j " +
                    "JOIN FETCH j.president " +
                    "LEFT JOIN FETCH j.rapporteur1 " +
                    "LEFT JOIN FETCH j.rapporteur2 " +
                    "LEFT JOIN FETCH j.invite " +
                    "WHERE s.ids = :id",
                    Soutenance.class)
                    .setParameter("id", id)
                    .uniqueResult();
        }
    }

    @Override
    public List<Soutenance> findByVersion(Long versionId) {
        if (versionId == null) return java.util.Collections.emptyList();
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "select s from Soutenance s " +
                    " join fetch s.etudiant " +
                    " join fetch s.salle " +
                    " join fetch s.jury " +
                    " where s.version.id = :v " +
                    " order by s.date, s.heure",
                    Soutenance.class)
                    .setParameter("v", versionId)
                    .list();
        }
    }

    @Override
    public List<Soutenance> findBySession(Long sessionId) {
        if (sessionId == null) return java.util.Collections.emptyList();
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "select s from Soutenance s " +
                    " join fetch s.etudiant " +
                    " join fetch s.salle " +
                    " join fetch s.jury " +
                    " where s.session.id = :s " +
                    " order by s.date, s.heure",
                    Soutenance.class)
                    .setParameter("s", sessionId)
                    .list();
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
    public void deleteById(Long id) {
        if (id == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("delete from Soutenance where ids = :id")
                    .setParameter("id", id)
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

    @Override
    public void setStatus(Long id, SoutenanceStatus status, Long actorId, String comment) {
        if (id == null || status == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Soutenance s = session.get(Soutenance.class, id);
            if (s != null) {
                s.setStatus(status);
                s.setManualOverride(true);
                s.setLastModifiedById(actorId);
                s.setLastModifiedAt(new Date());
                if (comment != null && !comment.isBlank()) {
                    s.setComment(comment);
                }
                session.merge(s);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void setLocked(Long id, boolean locked, Long actorId) {
        if (id == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Soutenance s = session.get(Soutenance.class, id);
            if (s != null) {
                s.setLocked(locked);
                s.setLockedById(locked ? actorId : null);
                s.setLockedAt(locked ? new Date() : null);
                if (locked && s.getStatus() == SoutenanceStatus.PLANNED) {
                    s.setStatus(SoutenanceStatus.LOCKED);
                } else if (!locked && s.getStatus() == SoutenanceStatus.LOCKED) {
                    s.setStatus(SoutenanceStatus.PLANNED);
                }
                s.setLastModifiedById(actorId);
                s.setLastModifiedAt(new Date());
                session.merge(s);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void replaceJury(Long soutenanceId, Long juryId, Long actorId) {
        if (soutenanceId == null || juryId == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Soutenance s = session.get(Soutenance.class, soutenanceId);
            Jury newJury = session.get(Jury.class, juryId);
            if (s != null && newJury != null) {
                s.setJury(newJury);
                s.setManualOverride(true);
                s.setStatus(SoutenanceStatus.JURY_REPLACED);
                s.setLastModifiedById(actorId);
                s.setLastModifiedAt(new Date());
                session.merge(s);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void replaceSalle(Long soutenanceId, Long salleId, Long actorId) {
        if (soutenanceId == null || salleId == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            Soutenance s = session.get(Soutenance.class, soutenanceId);
            Salle newSalle = session.get(Salle.class, salleId);
            if (s != null && newSalle != null) {
                s.setSalle(newSalle);
                s.setManualOverride(true);
                s.setLastModifiedById(actorId);
                s.setLastModifiedAt(new Date());
                session.merge(s);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void touchConvocation(Long id, Date when) {
        if (id == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "update Soutenance set convocationSentAt = :w where ids = :id")
                    .setParameter("w", when == null ? new Date() : when)
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public void touchReminder(Long id, Date when) {
        if (id == null) return;
        Transaction tx = null;
        try (Session session = sf.openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "update Soutenance set reminderSentAt = :w where ids = :id")
                    .setParameter("w", when == null ? new Date() : when)
                    .setParameter("id", id)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }

    @Override
    public long countByStatus(SoutenanceStatus status) {
        if (status == null) return 0L;
        try (Session session = sf.openSession()) {
            Long n = session.createQuery(
                    "select count(s) from Soutenance s where s.status = :st",
                    Long.class)
                    .setParameter("st", status)
                    .uniqueResult();
            return n == null ? 0L : n;
        }
    }
}
