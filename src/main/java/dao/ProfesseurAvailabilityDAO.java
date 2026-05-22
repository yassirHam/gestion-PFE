package dao;

import entities.ProfesseurAvailability;
import java.util.List;

public interface ProfesseurAvailabilityDAO {
    ProfesseurAvailability save(ProfesseurAvailability av);
    ProfesseurAvailability findById(Long id);
    List<ProfesseurAvailability> findAll();
    List<ProfesseurAvailability> findByProfesseur(Long profId);
    List<ProfesseurAvailability> findBySession(Long sessionId);
    void deleteById(Long id);
}
