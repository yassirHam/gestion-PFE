package dao;

import entities.Affectation;
import entities.LifecycleState;
import java.util.List;

public interface AffectationDAO {
    void saveAll(List<Affectation> affectations);
    List<Affectation> findAll();
    List<Affectation> findAllWithDetails();
    Affectation findById(Long id);
    Affectation update(Affectation affectation);
    void updateLifecycleState(Long id, LifecycleState state, Long actorId);
    void setLocked(Long id, boolean locked, Long actorId);
    void deleteByFilieres(List<String> filieres);
    void deleteById(Long id);
    void deleteAll();
}
