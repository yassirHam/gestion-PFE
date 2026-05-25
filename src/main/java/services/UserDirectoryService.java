package services;

import dao.AppUserDAO;
import dao.AppUserDAOImpl;
import entities.AppUser;

import java.util.List;
import java.util.Objects;

/**
 * Read-only access to persisted operator records used by the audit page.
 */
public final class UserDirectoryService {

    private static final UserDirectoryService INSTANCE = new UserDirectoryService(new AppUserDAOImpl());
    private final AppUserDAO dao;

    UserDirectoryService(AppUserDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public static UserDirectoryService getInstance() { return INSTANCE; }

    public List<AppUser> findAll() { return dao.findAll(); }
}
