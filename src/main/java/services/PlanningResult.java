package services;

import entities.Soutenance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of {@link PlanningService#genererPlanning} including the generated
 * soutenances, the constraint violations encountered (HARD = blocking, SOFT =
 * warning), the list of unscheduled students, and recommended next steps.
 *
 * <p>If {@link #isSuccess()} is {@code false}, the planning is NOT persisted to
 * the database and the controller must surface the violations + suggestions to
 * the user.</p>
 */
public class PlanningResult {

    private boolean success;
    private final List<Soutenance> soutenances = new ArrayList<>();
    private final List<String> unscheduledProjects = new ArrayList<>();
    private final List<ConstraintViolation> hardViolations = new ArrayList<>();
    private final List<ConstraintViolation> softViolations = new ArrayList<>();
    private final List<Recommendation> suggestions = new ArrayList<>();
    private final List<String> debugLog = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<Soutenance> getSoutenances() { return Collections.unmodifiableList(soutenances); }
    public void addSoutenances(List<Soutenance> s) { if (s != null) soutenances.addAll(s); }
    public void clearSoutenances() { soutenances.clear(); }

    public List<String> getUnscheduledProjects() { return Collections.unmodifiableList(unscheduledProjects); }
    public void addUnscheduledProject(String name) { if (name != null) unscheduledProjects.add(name); }

    public List<ConstraintViolation> getHardViolations() { return Collections.unmodifiableList(hardViolations); }
    public List<ConstraintViolation> getSoftViolations() { return Collections.unmodifiableList(softViolations); }

    public void addViolation(ConstraintViolation v) {
        if (v == null) return;
        if (v.isHard()) hardViolations.add(v); else softViolations.add(v);
    }

    public List<Recommendation> getSuggestions() { return Collections.unmodifiableList(suggestions); }
    public void addSuggestion(Recommendation r) { if (r != null) suggestions.add(r); }

    public List<String> getDebugLog() { return Collections.unmodifiableList(debugLog); }
    public void addDebug(String line) { if (line != null) debugLog.add(line); }

    public boolean hasBlockingIssues() {
        return !hardViolations.isEmpty() || !unscheduledProjects.isEmpty();
    }

    public int getTotalSoutenances() { return soutenances.size(); }
}
