package dao;

import java.util.List;

import entities.Professeur;

public interface ProfesseurDAO {

    List<Professeur> findAll();
    Professeur findById(Long id);
    Professeur save(Professeur prof);
    void saveAll(List<Professeur> list);
    void deleteAll();

    /** Temporarily exclude a professor from new juries, with a reason. */
    void setExcluded(Long profId, boolean excluded, String reason, Long actorId);
}
