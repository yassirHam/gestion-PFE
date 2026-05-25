package entities;


import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "affectation")
public class Affectation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ida;

    @ManyToOne
    @JoinColumn(name = "ide")
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "idp")
    private Professeur encadrant;

    /** Lifecycle of this affectation (draft / pending / validated / published / archived). */
    @Enumerated(EnumType.STRING)
    @Column(length = 32, name = "lifecycle_state")
    private LifecycleState lifecycleState = LifecycleState.DRAFT;

    /** Set to true when an administrator forced this assignment manually. */
    @Column(name = "manual_override")
    private Boolean manualOverride = false;

    /** Locked affectations cannot be modified by the auto-affectation algorithm. */
    @Column(name = "locked")
    private Boolean locked = false;

    /** Active session this affectation belongs to. */
    @ManyToOne
    @JoinColumn(name = "session_id")
    private AcademicSession session;

    @Column(name = "validated_by_id")
    private Long validatedById;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "validated_at")
    private Date validatedAt;

    @Column(name = "created_by_id")
    private Long createdById;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "last_modified_by_id")
    private Long lastModifiedById;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified_at")
    private Date lastModifiedAt;

    @Column(length = 1024, name = "override_reason")
    private String overrideReason;

    public Affectation() {}

	public Affectation(Long ida, Etudiant etudiant, Professeur encadrant) {
		super();
		this.ida = ida;
		this.etudiant = etudiant;
		this.encadrant = encadrant;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) createdAt = new Date();
		if (lastModifiedAt == null) lastModifiedAt = createdAt;
		if (lifecycleState == null) lifecycleState = LifecycleState.DRAFT;
	}

	@PreUpdate
	void preUpdate() {
		lastModifiedAt = new Date();
	}

	public Long getIda() {
		return ida;
	}

	public void setIda(Long ida) {
		this.ida = ida;
	}

	public Etudiant getEtudiant() {
		return etudiant;
	}

	public void setEtudiant(Etudiant etudiant) {
		this.etudiant = etudiant;
	}

	public Professeur getEncadrant() {
		return encadrant;
	}

	public void setEncadrant(Professeur encadrant) {
		this.encadrant = encadrant;
	}

	public LifecycleState getLifecycleState() {
		return lifecycleState == null ? LifecycleState.DRAFT : lifecycleState;
	}

	public void setLifecycleState(LifecycleState lifecycleState) {
		this.lifecycleState = lifecycleState == null ? LifecycleState.DRAFT : lifecycleState;
	}

	public boolean isManualOverride() {
		return manualOverride != null ? manualOverride : false;
	}

	public void setManualOverride(Boolean manualOverride) {
		this.manualOverride = manualOverride;
	}

	public boolean isLocked() {
		return locked != null ? locked : false;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}

	public AcademicSession getSession() { return session; }
	public void setSession(AcademicSession session) { this.session = session; }

	public Long getValidatedById() { return validatedById; }
	public void setValidatedById(Long v) { this.validatedById = v; }

	public Date getValidatedAt() { return validatedAt; }
	public void setValidatedAt(Date v) { this.validatedAt = v; }

	public Long getCreatedById() { return createdById; }
	public void setCreatedById(Long v) { this.createdById = v; }

	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date v) { this.createdAt = v; }

	public Long getLastModifiedById() { return lastModifiedById; }
	public void setLastModifiedById(Long v) { this.lastModifiedById = v; }

	public Date getLastModifiedAt() { return lastModifiedAt; }
	public void setLastModifiedAt(Date v) { this.lastModifiedAt = v; }

	public String getOverrideReason() { return overrideReason; }
	public void setOverrideReason(String v) { this.overrideReason = v; }

	public boolean isFrozen() {
		return getLifecycleState().isFrozen();
	}
}
