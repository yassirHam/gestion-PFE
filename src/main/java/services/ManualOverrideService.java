package services;

import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import entities.AppUser;
import entities.AuditAction;

import java.util.Objects;

/**
 * Operational human-override toolkit. The original system carried a much
 * larger set of overrides (lock/unlock soutenance, replace salle, swap a
 * jury member, etc.) wired to a richer admin UI. The simplified demo build
 * only keeps the two professor-level overrides invoked from the Governance
 * tab: temporarily exclude a professor or reinstate one that was excluded.
 *
 * <p>Every override goes through this service so the audit trail records
 * who did what, when, and why.</p>
 */
public final class ManualOverrideService {

    private static final ManualOverrideService INSTANCE = new ManualOverrideService(new ProfesseurDAOImpl());

    private final ProfesseurDAO profDao;

    ManualOverrideService(ProfesseurDAO profDao) {
        this.profDao = Objects.requireNonNull(profDao);
    }

    public static ManualOverrideService getInstance() { return INSTANCE; }

    public Result excludeProfessor(Long profId, AppUser actor, String reason) {
        if (profId == null) return Result.failure("Professeur introuvable.");
        profDao.setExcluded(profId, true, reason, actorId(actor));
        AuditService.getInstance().record(actor, AuditAction.PROF_EXCLUDED,
                "Professeur", profId,
                "Professeur exclu temporairement" + (reason == null || reason.isBlank() ? "" : " : " + reason));
        return Result.success("Professeur exclu temporairement.");
    }

    public Result reinstateProfessor(Long profId, AppUser actor) {
        if (profId == null) return Result.failure("Professeur introuvable.");
        profDao.setExcluded(profId, false, null, actorId(actor));
        AuditService.getInstance().record(actor, AuditAction.PROF_REINSTATED,
                "Professeur", profId, "Professeur réintégré");
        return Result.success("Professeur réintégré.");
    }

    private static Long actorId(AppUser u) { return u == null ? null : u.getId(); }

    public static final class Result {
        private final boolean success;
        private final String message;
        private Result(boolean s, String m) { this.success = s; this.message = m; }
        public static Result success(String m) { return new Result(true, m); }
        public static Result failure(String m) { return new Result(false, m); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
