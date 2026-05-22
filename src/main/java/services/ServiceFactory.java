package services;

import dao.AffectationDAOImpl;
import dao.EtudiantDAOImpl;
import dao.FichierListeDAOImpl;
import dao.JuryDAOImpl;
import dao.ProfesseurDAOImpl;
import dao.SalleDAOImpl;
import dao.SoutenanceDAOImpl;

/**
 * Lightweight factory used by the controller (no DI container). Holds
 * static getters for every singleton service; collaborator services are
 * exposed via their own {@code getInstance()} method.
 */
public final class ServiceFactory {

    private ServiceFactory() {
    }

    public static PfeService createPfeService() {
        PlanningService planningService = createPlanningService();
        VerificationService verificationService = createVerificationService();
        RecommendationService recommendationService = createRecommendationService();
        return new PfeServiceImpl(new AffectationDAOImpl(), new EtudiantDAOImpl(), new ProfesseurDAOImpl(),
                new FichierListeDAOImpl(), new SalleDAOImpl(),
                planningService, verificationService, recommendationService);
    }

    public static PlanningService createPlanningService() {
        return new PlanningServiceImpl(new AffectationDAOImpl(), new ProfesseurDAOImpl(), new SalleDAOImpl(),
                new JuryDAOImpl(), new SoutenanceDAOImpl(), new NlpServiceImpl(),
                new DefaultJurySelectionStrategy());
    }

    public static VerificationService createVerificationService() {
        return new VerificationServiceImpl(new AffectationDAOImpl(), new ProfesseurDAOImpl(), new SoutenanceDAOImpl());
    }

    public static RecommendationService createRecommendationService() {
        return new RecommendationServiceImpl();
    }

    // ─── Operational services (singletons) ─────────────────────────────────

    public static AuthService authService() { return AuthService.getInstance(); }
    public static AuditService auditService() { return AuditService.getInstance(); }
    public static SessionService sessionService() { return SessionService.getInstance(); }
    public static ApprovalWorkflowService approvalService() { return ApprovalWorkflowService.getInstance(); }
    public static ManualOverrideService overrideService() { return ManualOverrideService.getInstance(); }
    public static ExceptionManagementService exceptionService() { return ExceptionManagementService.getInstance(); }
    public static NotificationService notificationService() { return NotificationService.getInstance(); }
    public static GovernanceService governanceService() { return GovernanceService.getInstance(); }
}
