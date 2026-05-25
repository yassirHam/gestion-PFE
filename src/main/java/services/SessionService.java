package services;

import dao.AcademicSessionDAO;
import dao.AcademicSessionDAOImpl;
import dao.PlanningVersionDAO;
import dao.PlanningVersionDAOImpl;
import entities.AcademicSession;
import entities.AppSettings;
import entities.AppUser;
import entities.AuditAction;
import entities.LifecycleState;
import entities.PlanningVersion;

import java.util.Objects;

/**
 * Manages {@link AcademicSession}s and their {@link PlanningVersion}s.
 * Soutenances are tagged with the active session and version so the
 * persisted history can be filtered by session.
 *
 * <p>Exactly one session is "active" at a time and exactly one version per
 * session is the "current" one. The simplified demo build bootstraps a
 * default session on first use; the fuller administrative UI for managing
 * sessions and versions has been removed.</p>
 */
public final class SessionService {

    private static final SessionService INSTANCE = new SessionService(
            new AcademicSessionDAOImpl(), new PlanningVersionDAOImpl());

    private final AcademicSessionDAO sessionDao;
    private final PlanningVersionDAO versionDao;

    SessionService(AcademicSessionDAO sessionDao, PlanningVersionDAO versionDao) {
        this.sessionDao = Objects.requireNonNull(sessionDao);
        this.versionDao = Objects.requireNonNull(versionDao);
    }

    public static SessionService getInstance() { return INSTANCE; }

    // ─── Sessions ──────────────────────────────────────────────────────────

    public AcademicSession findById(Long id) { return sessionDao.findById(id); }

    /**
     * Returns the currently active session. If no session has been created
     * yet, one is bootstrapped from the {@link AppSettings#getAcademicYear()}
     * value so the rest of the system always has a non-null session to work
     * with.
     */
    public AcademicSession getActive() {
        AcademicSession active = sessionDao.findActive();
        if (active != null) return active;
        return ensureBootstrapSession();
    }

    private AcademicSession ensureBootstrapSession() {
        AcademicSession existing = sessionDao.findActive();
        if (existing != null) return existing;

        AppSettings settings = AppSettingsService.getInstance().get();
        String year = settings.getAcademicYear();
        if (year == null || year.isBlank()) year = "2025-2026";
        String code = "DEFAULT-" + year.replace(" ", "_");
        AcademicSession s = new AcademicSession(code, "Session par défaut " + year, year);
        s.setActive(true);
        AcademicSession saved = sessionDao.save(s);
        AppSettings cur = AppSettingsService.getInstance().get();
        cur.setActiveSessionId(saved.getId());
        AppSettingsService.getInstance().update(cur);
        return saved;
    }

    // ─── Versions ──────────────────────────────────────────────────────────

    public PlanningVersion getCurrentVersion(Long sessionId) {
        return versionDao.findCurrent(sessionId);
    }

    /**
     * Create a new version inside the supplied session and mark it as the
     * "current" one (existing versions remain on disk for history purposes).
     */
    public PlanningVersion createVersion(AcademicSession session, String label, AppUser actor) {
        if (session == null) return null;
        int n = versionDao.nextVersionNumber(session.getId());
        PlanningVersion v = new PlanningVersion(session, n,
                label == null || label.isBlank() ? "Version " + n : label.trim());
        v.setCurrent(true);
        v.setState(LifecycleState.DRAFT);
        if (actor != null) v.setCreatedById(actor.getId());
        PlanningVersion saved = versionDao.save(v);
        versionDao.setCurrent(session.getId(), saved.getId());
        AuditService.getInstance().record(actor, AuditAction.VERSION_CREATED,
                "PlanningVersion", saved.getId(),
                "Nouvelle version " + saved.getDisplayName() + " dans la session "
                        + session.getDisplayName());
        return saved;
    }
}
