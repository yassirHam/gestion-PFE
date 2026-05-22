package dao;

import java.util.List;

import entities.Etudiant;

public interface EtudiantDAO {
	
	List<Etudiant> findAll();
    List<Etudiant> findByFilieres(List<String> filieres);
    void saveAll(List<Etudiant> list);
    void deleteByFiliere(String filiere);
    void deleteAll();
     
}
