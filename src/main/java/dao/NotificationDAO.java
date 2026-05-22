package dao;

import entities.Notification;
import java.util.List;

public interface NotificationDAO {
    Notification save(Notification notification);
    Notification findById(Long id);
    List<Notification> findRecent(int limit);
    List<Notification> findBySoutenance(Long soutenanceId);
    List<Notification> findQueued();
    void updateStatus(Long id, Notification.Status status, String errorMessage);
    int countByStatus(Notification.Status status);
}
