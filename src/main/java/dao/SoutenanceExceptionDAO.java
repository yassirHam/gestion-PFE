package dao;

import entities.SoutenanceException;
import java.util.List;

public interface SoutenanceExceptionDAO {
    SoutenanceException save(SoutenanceException exc);
    SoutenanceException findById(Long id);
    List<SoutenanceException> findOpen();
    List<SoutenanceException> findBySoutenance(Long soutenanceId);
    List<SoutenanceException> findRecent(int limit);
    void updateStatus(Long id, SoutenanceException.ResolutionStatus status,
                      String resolutionNotes, Long resolvedById);
}
