package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Immutable, append-only record describing a single state change. Provides
 * the historical traceability missing from the original system: who did
 * what, when, on which artefact, and from which IP.
 */
@Entity
@Table(name = "audit_log",
        indexes = {
                @Index(name = "idx_audit_at", columnList = "occurred_at"),
                @Index(name = "idx_audit_actor", columnList = "actor_id"),
                @Index(name = "idx_audit_target", columnList = "target_type,target_id")
        })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "occurred_at", nullable = false)
    private Date occurredAt;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(length = 128, name = "actor_username")
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(length = 64, nullable = false)
    private AuditAction action;

    @Column(length = 64, name = "target_type")
    private String targetType;     // "Soutenance", "Affectation", "PlanningVersion", etc.

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 64, name = "session_code")
    private String sessionCode;    // optional academic session scope

    @Column(length = 1024)
    private String summary;

    @Lob
    @Column(name = "details")
    private String details;

    @Column(length = 64, name = "remote_ip")
    private String remoteIp;

    public AuditLog() {}

    public AuditLog(AuditAction action, AppUser actor, String targetType, Long targetId, String summary) {
        this.action = action;
        if (actor != null) {
            this.actorId = actor.getId();
            this.actorUsername = actor.getUsername();
        }
        this.targetType = targetType;
        this.targetId = targetId;
        this.summary = summary;
        this.occurredAt = new Date();
    }

    @PrePersist
    void prePersist() {
        if (occurredAt == null) occurredAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Date getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Date v) { this.occurredAt = v; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long v) { this.actorId = v; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String v) { this.actorUsername = v; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction v) { this.action = v; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String v) { this.targetType = v; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long v) { this.targetId = v; }
    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String v) { this.sessionCode = v; }
    public String getSummary() { return summary; }
    public void setSummary(String v) { this.summary = v; }
    public String getDetails() { return details; }
    public void setDetails(String v) { this.details = v; }
    public String getRemoteIp() { return remoteIp; }
    public void setRemoteIp(String v) { this.remoteIp = v; }
}
