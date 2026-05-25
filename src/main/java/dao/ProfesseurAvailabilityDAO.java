package dao;

import entities.ProfesseurAvailability;
import java.util.List;

public interface ProfesseurAvailabilityDAO {
    ProfesseurAvailability save(ProfesseurAvailability av);
    List<ProfesseurAvailability> findBySession(Long sessionId);
    void deleteById(Long id);
}
