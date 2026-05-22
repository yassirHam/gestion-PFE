package services;

import java.util.List;

public interface VerificationService {

    /**
     * Run the post-generation verification using the application defaults.
     */
    VerificationReport verifyGeneratedFiles(List<String> filieresFiltre);

    /**
     * Run the post-generation verification using a specific
     * {@link ConstraintSet}. Useful when the user has overridden values from
     * the configuration page.
     */
    VerificationReport verifyGeneratedFiles(List<String> filieresFiltre, ConstraintSet constraints);
}
