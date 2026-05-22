package dao;

import entities.Salle;
import java.util.List;

public interface SalleDAO {
    List<Salle> findAll();
    Salle findById(Long id);
    void save(Salle salle);
    void saveAll(List<Salle> salles);
    boolean deleteById(Long id);
    boolean isUsedInPlanning(Long id);
    void deleteAll();
    long count();
}
