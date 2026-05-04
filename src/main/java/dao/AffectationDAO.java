package dao;

import entities.Affectation;
import java.util.List;

public interface AffectationDAO {
    void saveAll(List<Affectation> affectations);
    List<Affectation> findAll();
    List<Affectation> findAllWithDetails();
    void deleteAll();
}