package dao;

import entities.Approval;
import java.util.List;

public interface ApprovalDAO {
    void save(Approval approval);
    List<Approval> findByVersion(Long versionId);
    List<Approval> findRecent(int limit);
}
