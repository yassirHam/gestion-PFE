package dao;

import entities.FichierListe;
import java.util.List;

public interface FichierListeDAO {
    void save(FichierListe f);
    List<FichierListe> findAll();
    FichierListe findByFiliere(String filiere);
    void deleteByFiliere(String filiere);
    void deleteById(Long id);
}