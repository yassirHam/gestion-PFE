package dao;

import java.util.List;

import entities.Professeur;

public interface ProfesseurDAO {
	
	List<Professeur> findAll();
    void saveAll(List<Professeur> list);
    void deleteAll();

}
