package dao;

import entities.Soutenance;
import java.util.List;

public interface SoutenanceDAO {
    void saveAll(List<Soutenance> soutenances);
    Soutenance save(Soutenance soutenance);
    List<Soutenance> findAllWithDetails();
    void deleteByFilieres(List<String> filieres);
    void deleteById(Long id);
    void deleteAll();
}
