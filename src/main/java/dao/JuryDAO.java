package dao;

import entities.Jury;
import java.util.List;

public interface JuryDAO {
    Jury save(Jury jury);
    List<Jury> findAll();
    void deleteAll();
}
