package dao;

import entities.AuditLog;
import java.util.List;

public interface AuditLogDAO {
    void save(AuditLog log);
    List<AuditLog> findRecent(int limit);
    List<AuditLog> findByActor(Long actorId, int limit);
}
