package services;

import dao.AffectationDAO;
import dao.AffectationDAOImpl;
import dao.JuryDAO;
import dao.JuryDAOImpl;
import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import dao.SalleDAO;
import dao.SalleDAOImpl;
import dao.SoutenanceDAO;
import dao.SoutenanceDAOImpl;
import entities.Affectation;
import entities.AppUser;
import entities.AuditAction;
import entities.Etudiant;
import entities.Jury;
import entities.LifecycleState;
import entities.Professeur;
import entities.Salle;
import entities.Soutenance;
import entities.SoutenanceStatus;

import java.util.Date;
import java.util.Objects;

/**
 * Operational human-override toolkit. Provides every action the original
 * system was missing:
 * <ul>
 *   <li>swap a single jury member without re-running the whole planning</li>
 *   <li>lock / unlock a soutenance so future re-plans don't move it</li>
 *   <li>force a specific affectation (encadrant override)</li>
 *   <li>temporarily exclude a professor from new juries</li>
 *   <li>cancel / postpone / replace salle of a single soutenance</li>
 * </ul>
 *
 * <p>Every override goes through this service so the audit trail records
 * who did what, when, and why.</p>
 */
public final class ManualOverrideService {

    private static final ManualOverrideService INSTANCE = new ManualOverrideService(
            new SoutenanceDAOImpl(), new JuryDAOImpl(), new AffectationDAOImpl(),
            new ProfesseurDAOImpl(), new SalleDAOImpl());

    private final SoutenanceDAO soutenanceDao;
    private final JuryDAO juryDao;
    private final AffectationDAO affectationDao;
    private final ProfesseurDAO profDao;
    private final SalleDAO salleDao;

    ManualOverrideService(SoutenanceDAO soutenanceDao, JuryDAO juryDao, AffectationDAO affectationDao,
                          ProfesseurDAO profDao, SalleDAO salleDao) {
        this.soutenanceDao = Objects.requireNonNull(soutenanceDao);
        this.juryDao = Objects.requireNonNull(juryDao);
        this.affectationDao = Objects.requireNonNull(affectationDao);
        this.profDao = Objects.requireNonNull(profDao);
        this.salleDao = Objects.requireNonNull(salleDao);
    }

    public static ManualOverrideService getInstance() { return INSTANCE; }

    // ─── Soutenance overrides ──────────────────────────────────────────────

    public Result lockSoutenance(Long soutenanceId, AppUser actor) {
        Soutenance s = soutenanceDao.findById(soutenanceId);
        if (s == null) return Result.failure("Soutenance introuvable.");
        if (s.isFrozen()) return Result.failure("Cette soutenance est déjà figée.");
        soutenanceDao.setLocked(soutenanceId, true, actorId(actor));
        AuditService.getInstance().record(actor, AuditAction.SOUTENANCE_LOCKED,
                "Soutenance", soutenanceId,
                "Verrouillage manuel de la soutenance #" + soutenanceId);
        return Result.success("Soutenance verrouillée.");
    }

    public Result unlockSoutenance(Long soutenanceId, AppUser actor) {
        Soutenance s = soutenanceDao.findById(soutenanceId);
        if (s == null) return Result.failure("Soutenance introuvable.");
        if (s.getVersion() != null && s.getVersion().isFrozen()) {
            return Result.failure("Impossible : la version contenant cette soutenance est publiée.");
        }
        soutenanceDao.setLocked(soutenanceId, false, actorId(actor));
        AuditService.getInstance().record(actor, AuditAction.SOUTENANCE_UNLOCKED,
                "Soutenance", soutenanceId,
                "Déverrouillage manuel de la soutenance #" + soutenanceId);
        return Result.success("Soutenance déverrouillée.");
    }

    public Result cancelSoutenance(Long soutenanceId, AppUser actor, String reason) {
        Soutenance s = soutenanceDao.findById(soutenanceId);
        if (s == null) return Result.failure("Soutenance introuvable.");
        soutenanceDao.setStatus(soutenanceId, SoutenanceStatus.CANCELLED, actorId(actor), reason);
        AuditService.getInstance().record(actor, AuditAction.SOUTENANCE_CANCELLED,
                "Soutenance", soutenanceId,
                "Soutenance annulée : " + (reason == null ? "(aucun motif)" : reason));
        return Result.success("Soutenance annulée.");
    }

    public Result postponeSoutenance(Long soutenanceId, AppUser actor, String reason) {
        Soutenance s = soutenanceDao.findById(soutenanceId);
        if (s == null) return Result.failure("Soutenance introuvable.");
        soutenanceDao.setStatus(soutenanceId, SoutenanceStatus.POSTPONED, actorId(actor), reason);
        AuditService.getInstance().record(actor, AuditAction.SOUTENANCE_POSTPONED,
                "Soutenance", soutenanceId,
                "Soutenance reportée : " + (reason == null ? "" : reason));
        return Result.success("Soutenance reportée.");
    }

    /** Re-schedule a soutenance to a new date / time / salle in one go. */
    public Result replanSoutenance(Long soutenanceId, AppUser actor,
                                   Date newDate, String newHeure, Long newSalleId) {
        Soutenance s = soutenanceDao.findById(soutenanceId);
        if (s == null) return Result.failure("Soutenance introuvable.");
        if (s.getVersion() != null && s.getVersion().isFrozen())
            return Result.failure("Impossible : la version est publiée et figée.");
        if (newDate != null) s.setDate(newDate);
        if (newHeure != null && !newHeure.isBlank()) s.setHeure(newHeure.trim());
        if (newSalleId != null) {
            Salle sl = salleDao.findById(newSalleId);
            if (sl != null) s.setSalle(sl);
        }
        s.setManualOverride(true);
        s.setLastModifiedById(actorId(actor));
        s.setLastModifiedAt(new Date());
        // Keep the status as PLANNED (or LOCKED/POSTPONED if already set):
        soutenanceDao.save(s);
        AuditService.getInstance().record(actor, AuditAction.SOUTENANCE_REPLANNED,
                "Soutenance", soutenanceId, "Soutenance replanifiée manuellement");
        return Result.success("Soutenance replanifiée.");
    }

    public Result replaceSalle(Long soutenanceId, Long salleId, AppUser actor) {
        Soutenance s = soutenanceDao.findById(soutenanceId);
        if (s == null) return Result.failure("Soutenance introuvable.");
        if (s.getVersion() != null && s.getVersion().isFrozen())
            return Result.failure("Impossible : la version est publiée et figée.");
        Salle salle = salleDao.findById(salleId);
        if (salle == null) return Result.failure("Salle introuvable.");
        soutenanceDao.replaceSalle(soutenanceId, salleId, actorId(actor));
        AuditService.getInstance().record(actor, AuditAction.SALLE_REPLACED,
                "Soutenance", soutenanceId,
                "Salle remplacée par " + salle.getNum_salle());
        return Result.success("Salle changée pour " + salle.getNum_salle() + ".");
    }

    // ─── Jury overrides ────────────────────────────────────────────────────

    /**
     * Replace a single member of an existing jury. Doesn't touch any other
     * jury or soutenance.
     */
    public Result swapJuryMember(Long juryId, String role, Long replacementProfId,
                                 AppUser actor, String reason) {
        Jury j = juryDao.findById(juryId);
        if (j == null) return Result.failure("Jury introuvable.");
        Professeur replacement = profDao.findById(replacementProfId);
        if (replacement == null) return Result.failure("Professeur de remplacement introuvable.");
        if (replacement.isExcluded()) return Result.failure("Ce professeur est temporairement exclu.");
        if (j.contains(replacement)) return Result.failure("Ce professeur fait déjà partie du jury.");
        juryDao.swapMember(juryId, role, replacement, actorId(actor), reason);
        AuditService.getInstance().record(actor, AuditAction.JURY_MEMBER_SWAPPED,
                "Jury", juryId,
                "Membre " + role + " remplacé par " + replacement.getNom() + " " + replacement.getPrenom()
                        + (reason == null || reason.isBlank() ? "" : " — " + reason));
        return Result.success("Membre de jury remplacé.");
    }

    public Result lockJury(Long juryId, AppUser actor) {
        Jury j = juryDao.findById(juryId);
        if (j == null) return Result.failure("Jury introuvable.");
        juryDao.setLocked(juryId, true, actorId(actor));
        AuditService.getInstance().record(actor, AuditAction.SOUTENANCE_LOCKED,
                "Jury", juryId, "Jury verrouillé");
        return Result.success("Jury verrouillé.");
    }

    public Result unlockJury(Long juryId, AppUser actor) {
        juryDao.setLocked(juryId, false, actorId(actor));
        AuditService.getInstance().record(actor, AuditAction.SOUTENANCE_UNLOCKED,
                "Jury", juryId, "Jury déverrouillé");
        return Result.success("Jury déverrouillé.");
    }

    // ─── Affectation overrides ─────────────────────────────────────────────

    /** Force / re-assign an encadrant for a single student. */
    public Result forceAffectation(Long affectationId, Long encadrantId, AppUser actor, String reason) {
        Affectation a = affectationDao.findById(affectationId);
        if (a == null) return Result.failure("Affectation introuvable.");
        if (a.isFrozen()) return Result.failure("Cette affectation est figée.");
        Professeur p = profDao.findById(encadrantId);
        if (p == null) return Result.failure("Professeur introuvable.");
        if (p.isExcluded()) return Result.failure("Ce professeur est temporairement exclu.");
        a.setEncadrant(p);
        a.setManualOverride(true);
        a.setOverrideReason(reason);
        a.setLastModifiedById(actorId(actor));
        a.setLastModifiedAt(new Date());
        affectationDao.update(a);
        Etudiant e = a.getEtudiant();
        AuditService.getInstance().record(actor, AuditAction.AFFECTATION_FORCED,
                "Affectation", affectationId,
                "Encadrant forcé pour " + (e == null ? "?" : e.getNomE()) + " → " + p.getNom() + " " + p.getPrenom()
                        + (reason == null || reason.isBlank() ? "" : " — " + reason));
        return Result.success("Encadrement forcé.");
    }

    public Result lockAffectation(Long affectationId, boolean locked, AppUser actor) {
        Affectation a = affectationDao.findById(affectationId);
        if (a == null) return Result.failure("Affectation introuvable.");
        affectationDao.setLocked(affectationId, locked, actorId(actor));
        AuditService.getInstance().record(actor, AuditAction.AFFECTATION_LOCKED,
                "Affectation", affectationId,
                locked ? "Affectation verrouillée" : "Affectation déverrouillée");
        return Result.success(locked ? "Affectation verrouillée." : "Affectation déverrouillée.");
    }

    public Result transitionAffectation(Long affectationId, LifecycleState target, AppUser actor) {
        Affectation a = affectationDao.findById(affectationId);
        if (a == null) return Result.failure("Affectation introuvable.");
        if (target == null) return Result.failure("État cible non précisé.");
        affectationDao.updateLifecycleState(affectationId, target, actorId(actor));
        AuditAction action = target == LifecycleState.VALIDATED
                ? AuditAction.AFFECTATION_VALIDATED
                : AuditAction.AFFECTATION_CREATED;
        AuditService.getInstance().record(actor, action,
                "Affectation", affectationId,
                "Affectation passée à l'état " + target.getLabel());
        return Result.success("Affectation : " + target.getLabel() + ".");
    }

    // ─── Professor overrides ───────────────────────────────────────────────

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

    // ─── Helpers ───────────────────────────────────────────────────────────

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
