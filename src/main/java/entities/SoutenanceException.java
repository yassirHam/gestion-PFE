package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Operational incident attached to a {@link Soutenance}: student absent,
 * professor absent, room unavailable, jury replacement requested, etc.
 *
 * <p>The {@code ExceptionManagementService} resolves these incidents by
 * re-planning the affected slot, swapping a jury member, or postponing
 * the session, while keeping the rest of the published planning frozen.</p>
 */
@Entity
@Table(name = "soutenance_exception",
        indexes = { @Index(name = "idx_exc_soutenance", columnList = "soutenance_id") })
public class SoutenanceException {

    public enum ResolutionStatus { OPEN, IN_PROGRESS, RESOLVED, IGNORED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "soutenance_id")
    private Soutenance soutenance;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ExceptionType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ResolutionStatus status = ResolutionStatus.OPEN;

    @Column(length = 2048)
    private String description;

    @Column(length = 2048, name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "reported_by_id")
    private Long reportedById;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "reported_at")
    private Date reportedAt;

    @Column(name = "resolved_by_id")
    private Long resolvedById;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "resolved_at")
    private Date resolvedAt;

    @Column(name = "replacement_prof_id")
    private Long replacementProfId;

    @Column(name = "replacement_salle_id")
    private Long replacementSalleId;

    public SoutenanceException() {}

    @PrePersist
    void prePersist() {
        if (reportedAt == null) reportedAt = new Date();
        if (status == null) status = ResolutionStatus.OPEN;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Soutenance getSoutenance() { return soutenance; }
    public void setSoutenance(Soutenance s) { this.soutenance = s; }
    public ExceptionType getType() { return type; }
    public void setType(ExceptionType v) { this.type = v; }
    public ResolutionStatus getStatus() { return status; }
    public void setStatus(ResolutionStatus v) { this.status = v == null ? ResolutionStatus.OPEN : v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String v) { this.resolutionNotes = v; }
    public Long getReportedById() { return reportedById; }
    public void setReportedById(Long v) { this.reportedById = v; }
    public Date getReportedAt() { return reportedAt; }
    public void setReportedAt(Date v) { this.reportedAt = v; }
    public Long getResolvedById() { return resolvedById; }
    public void setResolvedById(Long v) { this.resolvedById = v; }
    public Date getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Date v) { this.resolvedAt = v; }
    public Long getReplacementProfId() { return replacementProfId; }
    public void setReplacementProfId(Long v) { this.replacementProfId = v; }
    public Long getReplacementSalleId() { return replacementSalleId; }
    public void setReplacementSalleId(Long v) { this.replacementSalleId = v; }

    public boolean isResolved() { return status == ResolutionStatus.RESOLVED; }
}
