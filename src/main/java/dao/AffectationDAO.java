package dao;

import entities.Affectation;
import java.util.List;

public interface AffectationDAO {
    void saveAll(List<Affectation> affectations);
    List<Affectation> findAllWithDetails();
    void deleteByFilieres(List<String> filieres);
    void deleteAll();
}
