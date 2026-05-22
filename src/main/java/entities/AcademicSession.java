package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Single planning session ("Session Juin 2026", "Session Septembre 2026"…).
 * A session groups one or more {@link PlanningVersion}s and is the unit
 * over which all data (affectations, soutenances, juries) is scoped.
 */
@Entity
@Table(name = "academic_session")
public class AcademicSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String code;            // e.g. "JUIN-2026"

    @Column(length = 255)
    private String label;           // e.g. "Session de juin 2026"

    @Column(length = 64)
    private String academicYear;    // e.g. "2025-2026"

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;  // optional — null = global / multi-département

    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "deadline_date")
    private Date deadlineDate;      // last possible defense date

    @Column(name = "active")
    private boolean active;         // exactly one session is "active" at a time

    @Column(name = "closed")
    private boolean closed;         // true once the session is fully archived

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "created_by_id")
    private Long createdById;

    public AcademicSession() {}

    public AcademicSession(String code, String label, String academicYear) {
        this.code = code;
        this.label = label;
        this.academicYear = academicYear;
        this.createdAt = new Date();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String v) { this.academicYear = v; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department d) { this.department = d; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date v) { this.startDate = v; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date v) { this.endDate = v; }
    public Date getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(Date v) { this.deadlineDate = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean v) { this.closed = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { this.createdAt = v; }
    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long v) { this.createdById = v; }

    public String getDisplayName() {
        if (label != null && !label.isEmpty()) return label;
        if (code != null) return code;
        return "Session #" + id;
    }
}
