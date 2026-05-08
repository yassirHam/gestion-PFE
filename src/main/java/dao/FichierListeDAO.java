package dao;

import entities.FichierListe;
import java.util.List;

public interface FichierListeDAO {
    void save(FichierListe f);
    List<FichierListe> findAll();
    void deleteByFiliere(String filiere);
}
