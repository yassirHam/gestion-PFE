package dao;

import entities.Soutenance;
import entities.SoutenanceStatus;
import java.util.Date;
import java.util.List;

public interface SoutenanceDAO {
    void saveAll(List<Soutenance> soutenances);
    Soutenance save(Soutenance soutenance);
    List<Soutenance> findAllWithDetails();
    Soutenance findById(Long id);
    Soutenance findByIdWithDetails(Long id);
    List<Soutenance> findByVersion(Long versionId);
    List<Soutenance> findBySession(Long sessionId);
    void deleteByFilieres(List<String> filieres);
    void deleteById(Long id);
    void deleteAll();

    // Manual override / lifecycle ops
    void setStatus(Long id, SoutenanceStatus status, Long actorId, String comment);
    void setLocked(Long id, boolean locked, Long actorId);
    void replaceJury(Long soutenanceId, Long juryId, Long actorId);
    void replaceSalle(Long soutenanceId, Long salleId, Long actorId);
    void touchConvocation(Long id, Date when);
    void touchReminder(Long id, Date when);
    long countByStatus(SoutenanceStatus status);
}
