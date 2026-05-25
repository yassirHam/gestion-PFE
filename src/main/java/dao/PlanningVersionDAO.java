package dao;

import entities.PlanningVersion;

public interface PlanningVersionDAO {
    PlanningVersion save(PlanningVersion version);
    PlanningVersion findCurrent(Long sessionId);
    int nextVersionNumber(Long sessionId);
    void setCurrent(Long sessionId, Long versionId);
}
