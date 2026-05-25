package services;

import dao.AuditLogDAO;
import dao.AuditLogDAOImpl;
import entities.AppUser;
import entities.AuditAction;
import entities.AuditLog;

import java.util.List;
import java.util.Objects;

/**
 * Append-only audit trail. Records who performed what action, on which
 * artefact, when, from which IP.
 *
 * <p>The service deliberately catches and swallows persistence failures so
 * a failing audit write never breaks the calling business operation.</p>
 */
public final class AuditService {

    private static final AuditService INSTANCE =
            new AuditService(new AuditLogDAOImpl());

    private final AuditLogDAO dao;

    AuditService(AuditLogDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public static AuditService getInstance() { return INSTANCE; }

    // ─── Recording ─────────────────────────────────────────────────────────

    public void record(AppUser actor, AuditAction action, String targetType, Long targetId, String summary) {
        if (action == null) return;
        try {
            AuditLog log = new AuditLog(action, actor, targetType, targetId, summary);
            try {
                Long active = AppSettingsService.getInstance().get().getActiveSessionId();
                if (active != null) log.setSessionCode(String.valueOf(active));
            } catch (Exception ignored) {}
            dao.save(log);
        } catch (Exception e) {
            // Audit must never break the business call. Log to stderr only.
            e.printStackTrace();
        }
    }

    // ─── Querying ──────────────────────────────────────────────────────────

    public List<AuditLog> recent(int limit) { return dao.findRecent(limit); }

    public List<AuditLog> forActor(Long actorId, int limit) {
        return dao.findByActor(actorId, limit);
    }
}
