package dao;

import entities.Salle;
import java.util.List;

public interface SalleDAO {
    List<Salle> findAll();
    List<Salle> findAvailable();
    Salle findById(Long id);
    void save(Salle salle);
    void saveAll(List<Salle> salles);
    boolean deleteById(Long id);
    boolean isUsedInPlanning(Long id);
    void deleteAll();
    long count();
    void setAvailable(Long id, boolean available);
}
