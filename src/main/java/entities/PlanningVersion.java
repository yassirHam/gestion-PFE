package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * One iteration of a planning inside a {@link AcademicSession}.
 *
 * <p>Several versions can co-exist for the same session ("Version 1",
 * "Version corrigée", "Version finale") and only one is the
 * <em>current</em> version. Once a version is published it becomes
 * frozen and immutable until it is archived.</p>
 */
@Entity
@Table(name = "planning_version")
public class PlanningVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private AcademicSession session;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private LifecycleState state = LifecycleState.DRAFT;

    /**
     * When non-null, the planning is frozen and can no longer be edited
     * by anyone but an admin pédagogique.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "frozen_at")
    private Date frozenAt;

    @Column(name = "frozen_by_id")
    private Long frozenById;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "published_at")
    private Date publishedAt;

    @Column(name = "published_by_id")
    private Long publishedById;

    @Column(name = "is_current")
    private boolean current;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "created_by_id")
    private Long createdById;

    /** Snapshot of the {@link services.PlanningConfig} used to generate the version. */
    @Lob
    @Column(name = "config_snapshot")
    private String configSnapshot;

    public PlanningVersion() {}

    public PlanningVersion(AcademicSession session, int versionNumber, String label) {
        this.session = session;
        this.versionNumber = versionNumber;
        this.label = label;
        this.createdAt = new Date();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = new Date();
        if (state == null) state = LifecycleState.DRAFT;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AcademicSession getSession() { return session; }
    public void setSession(AcademicSession session) { this.session = session; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int n) { this.versionNumber = n; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public LifecycleState getState() { return state; }
    public void setState(LifecycleState v) { this.state = v == null ? LifecycleState.DRAFT : v; }
    public Date getFrozenAt() { return frozenAt; }
    public void setFrozenAt(Date v) { this.frozenAt = v; }
    public Long getFrozenById() { return frozenById; }
    public void setFrozenById(Long v) { this.frozenById = v; }
    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date v) { this.publishedAt = v; }
    public Long getPublishedById() { return publishedById; }
    public void setPublishedById(Long v) { this.publishedById = v; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean v) { this.current = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { this.createdAt = v; }
    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long v) { this.createdById = v; }
    public String getConfigSnapshot() { return configSnapshot; }
    public void setConfigSnapshot(String v) { this.configSnapshot = v; }

    public boolean isFrozen() { return state != null && state.isFrozen(); }
    public boolean isEditable() { return state != null && state.isEditable(); }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder("Version ").append(versionNumber);
        if (label != null && !label.isEmpty()) sb.append(" — ").append(label);
        return sb.toString();
    }
}
