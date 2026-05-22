package dao;

import entities.AuditLog;
import java.util.Date;
import java.util.List;

public interface AuditLogDAO {
    void save(AuditLog log);
    AuditLog findById(Long id);
    List<AuditLog> findRecent(int limit);
    List<AuditLog> findByActor(Long actorId, int limit);
    List<AuditLog> findByTarget(String targetType, Long targetId);
    List<AuditLog> findBetween(Date from, Date to, int limit);
}
