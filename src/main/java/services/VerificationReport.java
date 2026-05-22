package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VerificationReport {
    private boolean affectationDataAvailable;
    private boolean planningDataAvailable;
    private double encadrementAverage;
    private int encadrementMinExpected;
    private int encadrementMaxExpected;
    private String nlpSummary;
    private final List<VerificationIssue> issues = new ArrayList<>();

    public boolean isAffectationDataAvailable() {
        return affectationDataAvailable;
    }

    public void setAffectationDataAvailable(boolean affectationDataAvailable) {
        this.affectationDataAvailable = affectationDataAvailable;
    }

    public boolean isPlanningDataAvailable() {
        return planningDataAvailable;
    }

    public void setPlanningDataAvailable(boolean planningDataAvailable) {
        this.planningDataAvailable = planningDataAvailable;
    }

    public double getEncadrementAverage() {
        return encadrementAverage;
    }

    public String getEncadrementAverageFormatted() {
        return String.format(java.util.Locale.US, "%.2f", encadrementAverage);
    }

    public void setEncadrementAverage(double encadrementAverage) {
        this.encadrementAverage = encadrementAverage;
    }

    public int getEncadrementMinExpected() {
        return encadrementMinExpected;
    }

    public void setEncadrementMinExpected(int encadrementMinExpected) {
        this.encadrementMinExpected = encadrementMinExpected;
    }

    public int getEncadrementMaxExpected() {
        return encadrementMaxExpected;
    }

    public void setEncadrementMaxExpected(int encadrementMaxExpected) {
        this.encadrementMaxExpected = encadrementMaxExpected;
    }

    public String getNlpSummary() {
        return nlpSummary;
    }

    public void setNlpSummary(String nlpSummary) {
        this.nlpSummary = nlpSummary;
    }

    public void addIssue(String severity, String category, String title, String detail) {
        issues.add(new VerificationIssue(severity, category, title, detail));
    }

    public List<VerificationIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public boolean isCompliant() {
        return getCriticalCount() == 0 && getWarningCount() == 0;
    }

    public int getCriticalCount() {
        return countBySeverity("CRITIQUE");
    }

    public int getWarningCount() {
        return countBySeverity("ALERTE");
    }

    public int getInfoCount() {
        return countBySeverity("INFO");
    }

    public int getTotalIssues() {
        return issues.size();
    }

    private int countBySeverity(String severity) {
        int count = 0;
        for (VerificationIssue issue : issues) {
            if (severity.equals(issue.getSeverity())) count++;
        }
        return count;
    }
}
