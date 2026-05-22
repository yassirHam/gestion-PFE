package dao;

import entities.Soutenance;
import java.util.List;

public interface SoutenanceDAO {
    void saveAll(List<Soutenance> soutenances);
    List<Soutenance> findAllWithDetails();
    void deleteByFilieres(List<String> filieres);
    void deleteAll();
}
