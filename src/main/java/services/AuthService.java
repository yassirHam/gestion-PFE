package services;

import dao.AppUserDAO;
import dao.AppUserDAOImpl;
import entities.AppUser;
import entities.UserRole;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Authentication / user-management service. Replaces the previous
 * mono-admin assumption with a proper role-based access control layer.
 *
 * <p>Passwords are stored as PBKDF2-WithHmacSHA256 hashes (210 000 iterations,
 * 256-bit key, per-user 16-byte salt) encoded as
 * {@code pbkdf2$ITERATIONS$BASE64SALT$BASE64HASH} so we don't pull in any
 * external dependency.</p>
 *
 * <p>On first call, {@link #ensureBootstrapAdmin()} creates a default
 * {@code admin / admin} user when no user exists yet, so a freshly deployed
 * instance is immediately usable.</p>
 */
public final class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    private static final String ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;

    private static final AuthService INSTANCE =
            new AuthService(new AppUserDAOImpl());

    private final AppUserDAO dao;

    AuthService(AppUserDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public static AuthService getInstance() { return INSTANCE; }

    // ─── Authentication ────────────────────────────────────────────────────

    /**
     * Verify a username/password pair. Returns the matching user when valid
     * and {@code null} otherwise. Lazily creates the bootstrap admin on the
     * very first call.
     */
    public AppUser authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null) return null;
        ensureBootstrapAdmin();
        AppUser user = dao.findByUsername(username.trim());
        if (user == null || !user.isActive()) return null;
        if (!verifyPassword(password, user.getPasswordHash())) return null;
        dao.touchLastLogin(user.getId());
        return user;
    }

    public AppUser findById(Long id) { return dao.findById(id); }

    public AppUser findByUsername(String username) {
        return username == null ? null : dao.findByUsername(username);
    }

    public List<AppUser> findAll() { return dao.findAll(); }

    public List<AppUser> findByRole(UserRole role) { return dao.findByRole(role); }

    /**
     * Persist a new user, hashing the password if it has not been hashed yet.
     */
    public AppUser createUser(AppUser user, String plaintextPassword) {
        if (user == null) return null;
        if (plaintextPassword != null && !plaintextPassword.isEmpty()) {
            user.setPasswordHash(hashPassword(plaintextPassword));
        }
        return dao.save(user);
    }

    public AppUser updateUser(AppUser user) {
        return dao.save(user);
    }

    /**
     * Change the password of an existing user.
     */
    public void changePassword(Long userId, String newPlaintext) {
        if (userId == null || newPlaintext == null || newPlaintext.isEmpty()) return;
        AppUser u = dao.findById(userId);
        if (u == null) return;
        u.setPasswordHash(hashPassword(newPlaintext));
        dao.save(u);
    }

    public void deactivate(Long userId) {
        if (userId == null) return;
        AppUser u = dao.findById(userId);
        if (u == null) return;
        u.setActive(false);
        dao.save(u);
    }

    public void deleteUser(Long userId) {
        dao.deleteById(userId);
    }

    public long count() { return dao.count(); }

    /**
     * Create a default {@code admin/admin} user when no user exists yet.
     * Idempotent: subsequent calls are no-ops once any user is present.
     */
    public AppUser ensureBootstrapAdmin() {
        try {
            if (dao.count() > 0) return null;
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setFullName("Administrateur");
            admin.setRole(UserRole.ADMIN_PEDAGOGIQUE);
            admin.setActive(true);
            admin.setPasswordHash(hashPassword("admin"));
            AppUser saved = dao.save(admin);
            LOG.info("Bootstrap admin user created (username='admin', password='admin'). "
                    + "Change the password immediately from the user management page.");
            return saved;
        } catch (Exception e) {
            LOG.warning("Could not create bootstrap admin: " + e.getMessage());
            return null;
        }
    }

    // ─── Permissions helpers ───────────────────────────────────────────────

    public boolean canEdit(AppUser user) {
        return user != null && user.isActive() && user.getRole().canEdit();
    }

    public boolean canApprove(AppUser user) {
        return user != null && user.isActive() && user.getRole().canApprove();
    }

    public boolean canPublish(AppUser user) {
        return user != null && user.isActive() && user.getRole().canPublish();
    }

    public boolean canManageTenant(AppUser user) {
        return user != null && user.isActive() && user.getRole().canManageTenant();
    }

    public boolean canSeeFiliere(AppUser user, String filiere) {
        if (user == null) return false;
        return user.canSeeFiliere(filiere);
    }

    // ─── PBKDF2 ────────────────────────────────────────────────────────────

    public static String hashPassword(String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext");
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(plaintext.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            Base64.Encoder enc = Base64.getEncoder().withoutPadding();
            return "pbkdf2$" + ITERATIONS + "$" + enc.encodeToString(salt) + "$" + enc.encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash password", e);
        }
    }

    public static boolean verifyPassword(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null) return false;
        try {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] computed = pbkdf2(plaintext.toCharArray(), salt, iterations, expected.length * 8);
            // Constant-time comparison to mitigate timing attacks.
            if (computed.length != expected.length) return false;
            int diff = 0;
            for (int i = 0; i < computed.length; i++) diff |= computed[i] ^ expected[i];
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGO);
        try {
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
