package dao;

import entities.AppUser;
import entities.UserRole;
import java.util.List;

public interface AppUserDAO {
    AppUser save(AppUser user);
    AppUser findById(Long id);
    AppUser findByUsername(String username);
    List<AppUser> findAll();
    List<AppUser> findByRole(UserRole role);
    long count();
    void deleteById(Long id);
    void touchLastLogin(Long id);
}
