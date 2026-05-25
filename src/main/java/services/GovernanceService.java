package services;

import dao.ProfesseurAvailabilityDAO;
import dao.ProfesseurAvailabilityDAOImpl;
import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import entities.AppUser;
import entities.AuditAction;
import entities.Professeur;
import entities.ProfesseurAvailability;

import java.util.List;
import java.util.Objects;

/**
 * Governance layer for professor operational data and per-session
 * availability declarations.
 */
public final class GovernanceService {

    private static final GovernanceService INSTANCE = new GovernanceService(
            new ProfesseurAvailabilityDAOImpl(), new ProfesseurDAOImpl());

    private final ProfesseurAvailabilityDAO availabilityDao;
    private final ProfesseurDAO profDao;

    GovernanceService(ProfesseurAvailabilityDAO availabilityDao, ProfesseurDAO profDao) {
        this.availabilityDao = Objects.requireNonNull(availabilityDao);
        this.profDao = Objects.requireNonNull(profDao);
    }

    public static GovernanceService getInstance() { return INSTANCE; }

    // ─── Professor operational data ────────────────────────────────────────

    /**
     * Update operational fields (grade, internal/external, languages,
     * max-soutenances/day, VIP, email, phone) without touching identity.
     */
    public void updateProfesseurOperational(Professeur input, AppUser actor) {
        if (input == null || input.getIdp() == null) return;
        Professeur p = profDao.findById(input.getIdp());
        if (p == null) return;
        if (input.getGrade() != null) p.setGrade(input.getGrade());
        p.setInternal(input.isInternal());
        p.setVip(input.isVip());
        if (input.getEmail() != null) p.setEmail(input.getEmail());
        if (input.getPhone() != null) p.setPhone(input.getPhone());
        if (input.getLanguages() != null) p.setLanguages(input.getLanguages());
        p.setMaxSoutenancesPerDay(input.getMaxSoutenancesPerDay());
        profDao.save(p);
        AuditService.getInstance().record(actor, AuditAction.DEPARTMENT_UPDATED,
                "Professeur", p.getIdp(),
                "Mise à jour opérationnelle de " + p.getNom() + " " + p.getPrenom());
    }

    // ─── Professor availability ────────────────────────────────────────────

    public List<ProfesseurAvailability> findAvailability(Long sessionId) {
        return availabilityDao.findBySession(sessionId);
    }

    public ProfesseurAvailability declareAvailability(ProfesseurAvailability av, AppUser actor) {
        if (av == null) return null;
        if (av.getDeclaredById() == null && actor != null) av.setDeclaredById(actor.getId());
        ProfesseurAvailability saved = availabilityDao.save(av);
        AuditService.getInstance().record(actor, AuditAction.PROF_AVAILABILITY_DECLARED,
                "ProfesseurAvailability", saved == null ? null : saved.getId(),
                "Disponibilité déclarée : "
                        + (av.getProfesseur() == null ? "?" : av.getProfesseur().getNom())
                        + " — " + (av.getKind() == null ? "?" : av.getKind().name()));
        return saved;
    }

    public void deleteAvailability(Long id, AppUser actor) {
        availabilityDao.deleteById(id);
        AuditService.getInstance().record(actor, AuditAction.PROF_AVAILABILITY_DECLARED,
                "ProfesseurAvailability", id, "Disponibilité supprimée");
    }
}
