package dao;

import entities.Jury;
import entities.Professeur;
import java.util.List;

public interface JuryDAO {
    Jury save(Jury jury);
    Jury findById(Long id);
    List<Jury> findAll();
    void deleteAll();

    /**
     * Replace a single member of an existing jury.
     *
     * @param juryId       the jury to modify
     * @param role         "P" (president), "R1", "R2" or "INV"
     * @param replacement  the new professor (must not already be in the jury)
     * @param actorId      the AppUser performing the change (for audit)
     * @param reason       optional swap reason persisted on the jury
     */
    void swapMember(Long juryId, String role, Professeur replacement, Long actorId, String reason);

    void setLocked(Long juryId, boolean locked, Long actorId);
}
