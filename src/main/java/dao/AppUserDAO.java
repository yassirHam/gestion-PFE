package dao;

import entities.AppUser;
import java.util.List;

public interface AppUserDAO {
    List<AppUser> findAll();
}
