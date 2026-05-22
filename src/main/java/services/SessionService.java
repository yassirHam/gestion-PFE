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

import java.util.List;
import java.util.Objects;

/**
 * Manages {@link AcademicSession}s ("Session Juin 2026", "Session Septembre 2026")
 * and their {@link PlanningVersion}s ("Version 1", "Version finale"). Provides
 * the missing notion of session/version that the original system lacked.
 *
 * <p>Exactly one session is "active" at a time and exactly one version per
 * session is the "current" one. Activating a different session is a deliberate
 * administrative decision logged in the audit trail.</p>
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

    public List<AcademicSession> findAll() { return sessionDao.findAll(); }

    public AcademicSession findById(Long id) { return sessionDao.findById(id); }

    public AcademicSession findByCode(String code) { return sessionDao.findByCode(code); }

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

    public AcademicSession ensureBootstrapSession() {
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

    public AcademicSession create(AcademicSession session, AppUser actor) {
        if (session == null) return null;
        AcademicSession saved = sessionDao.save(session);
        AuditService.getInstance().record(actor, AuditAction.SESSION_CREATED,
                "AcademicSession", saved.getId(),
                "Création de la session " + saved.getDisplayName());
        return saved;
    }

    public void activate(Long sessionId, AppUser actor) {
        if (sessionId == null) return;
        sessionDao.setActive(sessionId);
        AppSettings cur = AppSettingsService.getInstance().get();
        cur.setActiveSessionId(sessionId);
        AppSettingsService.getInstance().update(cur);
        AcademicSession s = sessionDao.findById(sessionId);
        AuditService.getInstance().record(actor, AuditAction.SESSION_ACTIVATED,
                "AcademicSession", sessionId,
                "Session activée : " + (s == null ? sessionId : s.getDisplayName()));
    }

    public void close(Long sessionId, AppUser actor) {
        if (sessionId == null) return;
        sessionDao.close(sessionId);
        AcademicSession s = sessionDao.findById(sessionId);
        AuditService.getInstance().record(actor, AuditAction.SESSION_CLOSED,
                "AcademicSession", sessionId,
                "Session fermée : " + (s == null ? sessionId : s.getDisplayName()));
    }

    public void update(AcademicSession session, AppUser actor) {
        sessionDao.save(session);
        AuditService.getInstance().record(actor, AuditAction.SESSION_CREATED,
                "AcademicSession", session == null ? null : session.getId(),
                "Mise à jour de la session " + (session == null ? "?" : session.getDisplayName()));
    }

    // ─── Versions ──────────────────────────────────────────────────────────

    public List<PlanningVersion> findVersions(Long sessionId) {
        return versionDao.findBySession(sessionId);
    }

    public PlanningVersion findVersion(Long id) { return versionDao.findById(id); }

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

    public void setCurrent(Long sessionId, Long versionId, AppUser actor) {
        if (sessionId == null || versionId == null) return;
        versionDao.setCurrent(sessionId, versionId);
        AuditService.getInstance().record(actor, AuditAction.VERSION_CREATED,
                "PlanningVersion", versionId,
                "Version courante définie sur #" + versionId);
    }

    public PlanningVersion saveVersion(PlanningVersion v) {
        return versionDao.save(v);
    }

    public void deleteVersion(Long versionId, AppUser actor) {
        PlanningVersion v = versionDao.findById(versionId);
        if (v == null) return;
        if (v.isFrozen()) return; // cannot delete a frozen version
        versionDao.deleteById(versionId);
        AuditService.getInstance().record(actor, AuditAction.VERSION_REJECTED,
                "PlanningVersion", versionId,
                "Suppression d'une version brouillon : " + v.getDisplayName());
    }
}
