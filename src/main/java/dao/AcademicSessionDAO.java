package dao;

import entities.AcademicSession;

public interface AcademicSessionDAO {
    AcademicSession save(AcademicSession session);
    AcademicSession findById(Long id);
    AcademicSession findActive();
}
