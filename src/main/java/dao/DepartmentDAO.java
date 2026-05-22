package dao;

import entities.Department;
import java.util.List;

public interface DepartmentDAO {
    Department save(Department department);
    Department findById(Long id);
    Department findByCode(String code);
    List<Department> findAll();
    void deleteById(Long id);
}
