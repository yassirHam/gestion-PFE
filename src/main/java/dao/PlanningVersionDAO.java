package dao;

import entities.LifecycleState;
import entities.PlanningVersion;
import java.util.List;

public interface PlanningVersionDAO {
    PlanningVersion save(PlanningVersion version);
    PlanningVersion findById(Long id);
    List<PlanningVersion> findBySession(Long sessionId);
    PlanningVersion findCurrent(Long sessionId);
    int nextVersionNumber(Long sessionId);
    void setCurrent(Long sessionId, Long versionId);
    void updateState(Long versionId, LifecycleState state);
    void deleteById(Long id);
}
