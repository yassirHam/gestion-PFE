package services;

import dao.DepartmentDAO;
import dao.DepartmentDAOImpl;
import dao.ProfesseurAvailabilityDAO;
import dao.ProfesseurAvailabilityDAOImpl;
import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import dao.SalleDAO;
import dao.SalleDAOImpl;
import entities.AppUser;
import entities.AuditAction;
import entities.Department;
import entities.Professeur;
import entities.ProfesseurAvailability;
import entities.Salle;

import java.util.List;
import java.util.Objects;

/**
 * Department / filière governance layer. Lets the chef de département and
 * coordinateur filière own rooms, professors, quotas, and per-prof
 * availability.
 */
public final class GovernanceService {

    private static final GovernanceService INSTANCE = new GovernanceService(
            new DepartmentDAOImpl(), new ProfesseurAvailabilityDAOImpl(),
            new ProfesseurDAOImpl(), new SalleDAOImpl());

    private final DepartmentDAO departmentDao;
    private final ProfesseurAvailabilityDAO availabilityDao;
    private final ProfesseurDAO profDao;
    private final SalleDAO salleDao;

    GovernanceService(DepartmentDAO departmentDao, ProfesseurAvailabilityDAO availabilityDao,
                      ProfesseurDAO profDao, SalleDAO salleDao) {
        this.departmentDao = Objects.requireNonNull(departmentDao);
        this.availabilityDao = Objects.requireNonNull(availabilityDao);
        this.profDao = Objects.requireNonNull(profDao);
        this.salleDao = Objects.requireNonNull(salleDao);
    }

    public static GovernanceService getInstance() { return INSTANCE; }

    // ─── Departments ───────────────────────────────────────────────────────

    public List<Department> findAllDepartments() { return departmentDao.findAll(); }

    public Department findDepartment(Long id) { return departmentDao.findById(id); }

    public Department saveDepartment(Department d, AppUser actor) {
        Department saved = departmentDao.save(d);
        AuditService.getInstance().record(actor,
                d == null || d.getId() == null ? AuditAction.DEPARTMENT_CREATED
                                              : AuditAction.DEPARTMENT_UPDATED,
                "Department", saved == null ? null : saved.getId(),
                "Département : " + (saved == null ? "" : saved.getName()));
        return saved;
    }

    public void deleteDepartment(Long id, AppUser actor) {
        departmentDao.deleteById(id);
        AuditService.getInstance().record(actor, AuditAction.DEPARTMENT_UPDATED,
                "Department", id, "Département supprimé");
    }

    // ─── Salle ownership ───────────────────────────────────────────────────

    public void assignSalleToDepartment(Long salleId, Long departmentId, AppUser actor) {
        Salle salle = salleDao.findById(salleId);
        if (salle == null) return;
        Department d = departmentId == null ? null : departmentDao.findById(departmentId);
        salle.setDepartment(d);
        salleDao.save(salle);
        AuditService.getInstance().record(actor, AuditAction.DEPARTMENT_UPDATED,
                "Salle", salleId,
                "Salle " + salle.getNum_salle() + " rattachée à "
                        + (d == null ? "(aucun)" : d.getName()));
    }

    public void setSallePriority(Long salleId, int priority, AppUser actor) {
        Salle salle = salleDao.findById(salleId);
        if (salle == null) return;
        salle.setPriority(priority);
        salleDao.save(salle);
        AuditService.getInstance().record(actor, AuditAction.DEPARTMENT_UPDATED,
                "Salle", salleId,
                "Salle " + salle.getNum_salle() + " : priorité = " + priority);
    }

    public void setSalleAvailable(Long salleId, boolean available, AppUser actor) {
        salleDao.setAvailable(salleId, available);
        AuditService.getInstance().record(actor, AuditAction.DEPARTMENT_UPDATED,
                "Salle", salleId,
                available ? "Salle remise en service" : "Salle mise hors-service");
    }

    // ─── Professor governance ──────────────────────────────────────────────

    public void assignProfesseurToDepartment(Long profId, Long departmentId, AppUser actor) {
        Professeur p = profDao.findById(profId);
        if (p == null) return;
        Department d = departmentId == null ? null : departmentDao.findById(departmentId);
        p.setDepartment(d);
        profDao.save(p);
        AuditService.getInstance().record(actor, AuditAction.DEPARTMENT_UPDATED,
                "Professeur", profId,
                "Professeur " + p.getNom() + " rattaché à "
                        + (d == null ? "(aucun)" : d.getName()));
    }

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

    public List<ProfesseurAvailability> findAllAvailability() { return availabilityDao.findAll(); }

    public List<ProfesseurAvailability> findAvailability(Long sessionId) {
        return availabilityDao.findBySession(sessionId);
    }

    public List<ProfesseurAvailability> findAvailabilityForProf(Long profId) {
        return availabilityDao.findByProfesseur(profId);
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
