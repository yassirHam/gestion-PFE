package dao;

import entities.Salle;
import java.util.List;

public interface SalleDAO {
    List<Salle> findAll();
    void save(Salle salle);
    void saveAll(List<Salle> salles);
    void deleteAll();
    long count();
}
