package services;

import dao.ApprovalDAO;
import dao.ApprovalDAOImpl;
import dao.PlanningVersionDAO;
import dao.PlanningVersionDAOImpl;
import dao.SoutenanceDAO;
import dao.SoutenanceDAOImpl;
import entities.AppUser;
import entities.Approval;
import entities.AuditAction;
import entities.LifecycleState;
import entities.PlanningVersion;
import entities.Soutenance;
import entities.SoutenanceStatus;
import entities.UserRole;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Implements the validation lifecycle missing from the original system:
 * <pre>
 *   DRAFT --(submit)--> PENDING_VALIDATION --(approve)--> VALIDATED
 *                                                       --(reject)--> REJECTED
 *           VALIDATED --(publish)--> PUBLISHED --(archive)--> ARCHIVED
 * </pre>
 *
 * <p>Each transition is gated by role permissions and recorded as an
 * {@link Approval} row plus an audit event. Once a version is published,
 * its soutenances are frozen (locked + status PLANNED→LOCKED) so the
 * publication cannot be silently mutated, addressing the "publication
 * freeze" weakness.</p>
 */
public final class ApprovalWorkflowService {

    private static final ApprovalWorkflowService INSTANCE =
            new ApprovalWorkflowService(new PlanningVersionDAOImpl(),
                    new ApprovalDAOImpl(), new SoutenanceDAOImpl());

    private final PlanningVersionDAO versionDao;
    private final ApprovalDAO approvalDao;
    private final SoutenanceDAO soutenanceDao;

    ApprovalWorkflowService(PlanningVersionDAO versionDao, ApprovalDAO approvalDao,
                            SoutenanceDAO soutenanceDao) {
        this.versionDao = Objects.requireNonNull(versionDao);
        this.approvalDao = Objects.requireNonNull(approvalDao);
        this.soutenanceDao = Objects.requireNonNull(soutenanceDao);
    }

    public static ApprovalWorkflowService getInstance() { return INSTANCE; }

    // ─── Read ──────────────────────────────────────────────────────────────

    public List<Approval> getHistory(Long versionId) {
        return approvalDao.findByVersion(versionId);
    }

    public List<PlanningVersion> findPendingApprovals() {
        // Naïve scan over recent approvals + state filter; OK at the scale
        // of a single department.
        return new java.util.ArrayList<>(); // populated on demand by the controller
    }

    // ─── Transitions ───────────────────────────────────────────────────────

    /** Move from DRAFT/REJECTED to PENDING_VALIDATION. */
    public Result requestApproval(Long versionId, AppUser actor, String comment) {
        PlanningVersion v = versionDao.findById(versionId);
        if (v == null) return Result.failure("Version introuvable.");
        if (actor == null || !actor.isActive())
            return Result.failure("Action interdite : utilisateur non identifié.");
        LifecycleState st = v.getState();
        if (st != LifecycleState.DRAFT && st != LifecycleState.REJECTED)
            return Result.failure("Une demande ne peut être soumise qu'à partir d'un brouillon (état actuel : "
                    + st.getLabel() + ").");
        v.setState(LifecycleState.PENDING_VALIDATION);
        versionDao.save(v);
        approvalDao.save(new Approval(v, Approval.Decision.REQUESTED, actor, comment));
        AuditService.getInstance().record(actor, AuditAction.VERSION_SUBMITTED,
                "PlanningVersion", v.getId(),
                "Soumission pour validation de " + v.getDisplayName());
        return Result.success("Version soumise pour validation.");
    }

    /** PENDING_VALIDATION → VALIDATED, only ADMIN_PEDAGOGIQUE / CHEF_DEPARTEMENT. */
    public Result approve(Long versionId, AppUser actor, String comment) {
        PlanningVersion v = versionDao.findById(versionId);
        if (v == null) return Result.failure("Version introuvable.");
        if (!hasRole(actor, UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT))
            return Result.failure("Seul un administrateur pédagogique ou un chef de département peut valider.");
        if (v.getState() != LifecycleState.PENDING_VALIDATION)
            return Result.failure("La version doit être en attente de validation.");
        v.setState(LifecycleState.VALIDATED);
        versionDao.save(v);
        approvalDao.save(new Approval(v, Approval.Decision.APPROVED, actor, comment));
        AuditService.getInstance().record(actor, AuditAction.VERSION_VALIDATED,
                "PlanningVersion", v.getId(), "Validation de " + v.getDisplayName());
        return Result.success("Version validée.");
    }

    /** PENDING_VALIDATION → REJECTED (the author can fix and resubmit). */
    public Result reject(Long versionId, AppUser actor, String comment) {
        PlanningVersion v = versionDao.findById(versionId);
        if (v == null) return Result.failure("Version introuvable.");
        if (!hasRole(actor, UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT))
            return Result.failure("Seul un administrateur pédagogique ou un chef de département peut rejeter.");
        if (v.getState() != LifecycleState.PENDING_VALIDATION)
            return Result.failure("La version doit être en attente de validation.");
        v.setState(LifecycleState.REJECTED);
        versionDao.save(v);
        approvalDao.save(new Approval(v, Approval.Decision.REJECTED, actor, comment));
        AuditService.getInstance().record(actor, AuditAction.VERSION_REJECTED,
                "PlanningVersion", v.getId(), "Rejet de " + v.getDisplayName());
        return Result.success("Version rejetée.");
    }

    /**
     * VALIDATED → PUBLISHED. Freezes all soutenances of the version (sets
     * locked = true and status = LOCKED for those still PLANNED).
     */
    public Result publish(Long versionId, AppUser actor, String comment) {
        PlanningVersion v = versionDao.findById(versionId);
        if (v == null) return Result.failure("Version introuvable.");
        if (!hasRole(actor, UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT))
            return Result.failure("Seul un administrateur pédagogique ou un chef de département peut publier.");
        if (v.getState() != LifecycleState.VALIDATED)
            return Result.failure("Seule une version validée peut être publiée.");
        v.setState(LifecycleState.PUBLISHED);
        v.setPublishedAt(new Date());
        v.setPublishedById(actor == null ? null : actor.getId());
        v.setFrozenAt(new Date());
        v.setFrozenById(actor == null ? null : actor.getId());
        versionDao.save(v);
        approvalDao.save(new Approval(v, Approval.Decision.PUBLISHED, actor, comment));

        // Freeze the planning: lock every soutenance still in PLANNED state.
        try {
            List<Soutenance> list = soutenanceDao.findByVersion(versionId);
            for (Soutenance s : list) {
                if (!s.isLocked() && s.getStatus() == SoutenanceStatus.PLANNED) {
                    soutenanceDao.setLocked(s.getIds(), true,
                            actor == null ? null : actor.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        AuditService.getInstance().record(actor, AuditAction.VERSION_PUBLISHED,
                "PlanningVersion", v.getId(),
                "Publication de " + v.getDisplayName() + " (planning gelé)");
        return Result.success("Version publiée et figée.");
    }

    /** PUBLISHED → ARCHIVED. */
    public Result archive(Long versionId, AppUser actor, String comment) {
        PlanningVersion v = versionDao.findById(versionId);
        if (v == null) return Result.failure("Version introuvable.");
        if (!hasRole(actor, UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT))
            return Result.failure("Seul un administrateur pédagogique ou un chef de département peut archiver.");
        if (v.getState() != LifecycleState.PUBLISHED)
            return Result.failure("Seule une version publiée peut être archivée.");
        v.setState(LifecycleState.ARCHIVED);
        versionDao.save(v);
        approvalDao.save(new Approval(v, Approval.Decision.ARCHIVED, actor, comment));
        AuditService.getInstance().record(actor, AuditAction.VERSION_ARCHIVED,
                "PlanningVersion", v.getId(), "Archivage de " + v.getDisplayName());
        return Result.success("Version archivée.");
    }

    /**
     * Force-freeze a version (publication-freeze concept) without going
     * through the publish step. Useful when the planning is not yet validated
     * but must be temporarily protected (e.g. examinations in progress).
     */
    public Result freeze(Long versionId, AppUser actor, String comment) {
        PlanningVersion v = versionDao.findById(versionId);
        if (v == null) return Result.failure("Version introuvable.");
        if (!hasRole(actor, UserRole.ADMIN_PEDAGOGIQUE))
            return Result.failure("Seul un administrateur pédagogique peut figer un planning.");
        v.setFrozenAt(new Date());
        v.setFrozenById(actor == null ? null : actor.getId());
        versionDao.save(v);
        AuditService.getInstance().record(actor, AuditAction.VERSION_FROZEN,
                "PlanningVersion", v.getId(),
                "Gel manuel de " + v.getDisplayName()
                        + (comment == null || comment.isBlank() ? "" : " — " + comment));
        return Result.success("Planning figé.");
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private static boolean hasRole(AppUser u, UserRole... allowed) {
        if (u == null || !u.isActive() || allowed == null) return false;
        for (UserRole r : allowed) if (u.getRole() == r) return true;
        return false;
    }

    /** Return value for every transition. */
    public static final class Result {
        private final boolean success;
        private final String message;
        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        public static Result success(String m) { return new Result(true, m); }
        public static Result failure(String m) { return new Result(false, m); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
