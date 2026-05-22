package services;

import dao.SoutenanceDAO;
import dao.SoutenanceDAOImpl;
import dao.SoutenanceExceptionDAO;
import dao.SoutenanceExceptionDAOImpl;
import entities.AppUser;
import entities.AuditAction;
import entities.ExceptionType;
import entities.Soutenance;
import entities.SoutenanceException;
import entities.SoutenanceStatus;

import java.util.List;
import java.util.Objects;

/**
 * Operational exception workflow: students/professors that fall sick,
 * rooms that become unavailable, sudden cancellations. Each incident is
 * persisted as a {@link SoutenanceException} with its own resolution state
 * so the team can track what happened, what was decided, and by whom — the
 * audit chain that the original system was missing.
 *
 * <p>The service deliberately separates "report" from "resolve": reporting
 * is permissive (any authenticated user can flag an issue), resolution
 * delegates to {@link ManualOverrideService} for the actual operational
 * change (jury swap, postpone, replace salle).</p>
 */
public final class ExceptionManagementService {

    private static final ExceptionManagementService INSTANCE = new ExceptionManagementService(
            new SoutenanceExceptionDAOImpl(), new SoutenanceDAOImpl());

    private final SoutenanceExceptionDAO dao;
    private final SoutenanceDAO soutenanceDao;

    ExceptionManagementService(SoutenanceExceptionDAO dao, SoutenanceDAO soutenanceDao) {
        this.dao = Objects.requireNonNull(dao);
        this.soutenanceDao = Objects.requireNonNull(soutenanceDao);
    }

    public static ExceptionManagementService getInstance() { return INSTANCE; }

    public List<SoutenanceException> findOpen() { return dao.findOpen(); }
    public List<SoutenanceException> recent(int limit) { return dao.findRecent(limit); }
    public List<SoutenanceException> findBySoutenance(Long soutenanceId) {
        return dao.findBySoutenance(soutenanceId);
    }

    /** Report a new incident. Does not change the planning yet — see resolve(). */
    public SoutenanceException report(Long soutenanceId, ExceptionType type, String description, AppUser actor) {
        Soutenance s = soutenanceDao.findById(soutenanceId);
        if (s == null) return null;
        SoutenanceException ex = new SoutenanceException();
        ex.setSoutenance(s);
        ex.setType(type == null ? ExceptionType.OTHER : type);
        ex.setDescription(description);
        ex.setReportedById(actor == null ? null : actor.getId());
        ex.setStatus(SoutenanceException.ResolutionStatus.OPEN);
        SoutenanceException saved = dao.save(ex);
        AuditService.getInstance().record(actor, AuditAction.EXCEPTION_REPORTED,
                "Soutenance", soutenanceId,
                "Incident signalé : " + (type == null ? "autre" : type.getLabel())
                        + (description == null || description.isBlank() ? "" : " — " + description));
        return saved;
    }

    /**
     * Resolve an incident by combining the new state with an optional
     * operational consequence (postpone the soutenance, mark cancelled,
     * etc.). The actual jury swap / salle replacement are issued through
     * {@link ManualOverrideService} from the controller.
     */
    public ManualOverrideService.Result resolve(Long exceptionId, AppUser actor,
                                                ResolutionAction action, String resolutionNotes) {
        SoutenanceException ex = dao.findById(exceptionId);
        if (ex == null) return ManualOverrideService.Result.failure("Incident introuvable.");
        Soutenance s = ex.getSoutenance();
        ManualOverrideService.Result opResult = null;
        if (action != null && s != null) {
            ManualOverrideService mos = ManualOverrideService.getInstance();
            switch (action) {
                case POSTPONE:
                    opResult = mos.postponeSoutenance(s.getIds(), actor, resolutionNotes); break;
                case CANCEL:
                    opResult = mos.cancelSoutenance(s.getIds(), actor, resolutionNotes); break;
                case MARK_COMPLETED:
                    soutenanceDao.setStatus(s.getIds(), SoutenanceStatus.COMPLETED,
                            actor == null ? null : actor.getId(), resolutionNotes);
                    opResult = ManualOverrideService.Result.success("Soutenance marquée comme terminée.");
                    break;
                case ACKNOWLEDGE:
                default:
                    opResult = ManualOverrideService.Result.success("Incident acquitté sans changement opérationnel.");
            }
        }
        dao.updateStatus(exceptionId, SoutenanceException.ResolutionStatus.RESOLVED,
                resolutionNotes, actor == null ? null : actor.getId());
        AuditService.getInstance().record(actor, AuditAction.EXCEPTION_RESOLVED,
                "SoutenanceException", exceptionId,
                "Incident résolu" + (resolutionNotes == null || resolutionNotes.isBlank()
                        ? "" : " : " + resolutionNotes));
        return opResult == null ? ManualOverrideService.Result.success("Incident résolu.") : opResult;
    }

    public void ignore(Long exceptionId, AppUser actor, String notes) {
        dao.updateStatus(exceptionId, SoutenanceException.ResolutionStatus.IGNORED,
                notes, actor == null ? null : actor.getId());
        AuditService.getInstance().record(actor, AuditAction.EXCEPTION_RESOLVED,
                "SoutenanceException", exceptionId, "Incident ignoré");
    }

    /** Operational consequence picked when an incident is resolved. */
    public enum ResolutionAction {
        ACKNOWLEDGE,
        POSTPONE,
        CANCEL,
        MARK_COMPLETED;

        public String getLabel() {
            switch (this) {
                case POSTPONE:        return "Reporter la soutenance";
                case CANCEL:          return "Annuler la soutenance";
                case MARK_COMPLETED:  return "Marquer comme terminée";
                default:              return "Acquitter sans changement";
            }
        }

        public static ResolutionAction fromString(String value) {
            if (value == null) return ACKNOWLEDGE;
            try { return ResolutionAction.valueOf(value.trim().toUpperCase()); }
            catch (Exception e) { return ACKNOWLEDGE; }
        }
    }
}
