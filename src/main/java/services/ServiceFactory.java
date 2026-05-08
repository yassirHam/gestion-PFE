package services;

import dao.AffectationDAOImpl;
import dao.EtudiantDAOImpl;
import dao.FichierListeDAOImpl;
import dao.JuryDAOImpl;
import dao.ProfesseurDAOImpl;
import dao.SalleDAOImpl;
import dao.SoutenanceDAOImpl;

public final class ServiceFactory {

    private ServiceFactory() {
    }

    public static PfeService createPfeService() {
        PlanningService planningService = createPlanningService();
        VerificationService verificationService = createVerificationService();

        return new PfeServiceImpl(
                new AffectationDAOImpl(),
                new EtudiantDAOImpl(),
                new ProfesseurDAOImpl(),
                new FichierListeDAOImpl(),
                new SalleDAOImpl(),
                planningService,
                verificationService);
    }

    public static PlanningService createPlanningService() {
        return new PlanningServiceImpl(
                new AffectationDAOImpl(),
                new ProfesseurDAOImpl(),
                new SalleDAOImpl(),
                new JuryDAOImpl(),
                new SoutenanceDAOImpl(),
                new NlpServiceImpl(),
                PlanningConfig.defaults(),
                new DefaultJurySelectionStrategy());
    }

    public static VerificationService createVerificationService() {
        return new VerificationServiceImpl(
                new AffectationDAOImpl(),
                new ProfesseurDAOImpl(),
                new SoutenanceDAOImpl());
    }
}
