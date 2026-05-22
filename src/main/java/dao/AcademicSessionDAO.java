package dao;

import entities.AcademicSession;
import java.util.List;

public interface AcademicSessionDAO {
    AcademicSession save(AcademicSession session);
    AcademicSession findById(Long id);
    AcademicSession findByCode(String code);
    AcademicSession findActive();
    List<AcademicSession> findAll();
    void setActive(Long id);
    void close(Long id);
    void deleteById(Long id);
}
