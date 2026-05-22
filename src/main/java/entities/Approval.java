package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Single step of the approval chain
 * (DRAFT → PENDING_VALIDATION → VALIDATED → PUBLISHED).
 *
 * <p>A {@link PlanningVersion} can have multiple approvals (request,
 * validation, publication) and rejections all stored here for audit.</p>
 */
@Entity
@Table(name = "approval",
        indexes = { @Index(name = "idx_approval_version", columnList = "version_id") })
public class Approval {

    public enum Decision { REQUESTED, APPROVED, REJECTED, PUBLISHED, ARCHIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "version_id")
    private PlanningVersion version;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Decision decision;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(length = 128, name = "actor_username")
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, name = "actor_role")
    private UserRole actorRole;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "occurred_at")
    private Date occurredAt;

    @Column(length = 2048)
    private String comment;

    public Approval() {}

    public Approval(PlanningVersion version, Decision decision, AppUser actor, String comment) {
        this.version = version;
        this.decision = decision;
        this.comment = comment;
        if (actor != null) {
            this.actorId = actor.getId();
            this.actorUsername = actor.getUsername();
            this.actorRole = actor.getRole();
        }
        this.occurredAt = new Date();
    }

    @PrePersist
    void prePersist() {
        if (occurredAt == null) occurredAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PlanningVersion getVersion() { return version; }
    public void setVersion(PlanningVersion v) { this.version = v; }
    public Decision getDecision() { return decision; }
    public void setDecision(Decision v) { this.decision = v; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long v) { this.actorId = v; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String v) { this.actorUsername = v; }
    public UserRole getActorRole() { return actorRole; }
    public void setActorRole(UserRole v) { this.actorRole = v; }
    public Date getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Date v) { this.occurredAt = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }
}
