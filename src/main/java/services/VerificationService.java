package services;

import java.util.List;

public interface VerificationService {
    VerificationReport verifyGeneratedFiles(List<String> filieresFiltre);
}
