package entities;



import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "soutenance")
public class Soutenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ids;

    @Temporal(TemporalType.DATE)
    private Date date;

    private String heure; 

    @ManyToOne
    @JoinColumn(name = "id_salle")
    private Salle salle;

    @OneToOne	
    @JoinColumn(name = "ide")
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "id_jury")
    private Jury jury;

    /** Operational status (planned / locked / cancelled / postponed / completed / jury_replaced). */
    @Enumerated(EnumType.STRING)
    @Column(length = 32, name = "status")
    private SoutenanceStatus status = SoutenanceStatus.PLANNED;

    /** Locked soutenances cannot be moved by re-planning passes. */
    @Column(name = "locked")
    private Boolean locked = false;

    @Column(name = "manual_override")
    private Boolean manualOverride = false;

    @ManyToOne
    @JoinColumn(name = "version_id")
    private PlanningVersion version;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private AcademicSession session;

    /** Priority used for slot assignment when several projects compete for one slot. */
    @Column(name = "priority")
    private Integer priority = 0;     // 0 = normal, higher = more urgent

    @Column(length = 1024, name = "comment")
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "convocation_sent_at")
    private Date convocationSentAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "reminder_sent_at")
    private Date reminderSentAt;

    @Column(name = "locked_by_id")
    private Long lockedById;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "locked_at")
    private Date lockedAt;

    @Column(name = "created_by_id")
    private Long createdById;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified_at")
    private Date lastModifiedAt;

    @Column(name = "last_modified_by_id")
    private Long lastModifiedById;

    public Soutenance() {}

    
	public Soutenance(Long ids, Date date, String heure, Salle salle, Etudiant etudiant, Jury jury) {
		super();
		this.ids = ids;
		this.date = date;
		this.heure = heure;
		this.salle = salle;
		this.etudiant = etudiant;
		this.jury = jury;
	}

	@PrePersist
	void prePersist() {
		if (status == null) status = SoutenanceStatus.PLANNED;
		if (lastModifiedAt == null) lastModifiedAt = new Date();
	}

	@PreUpdate
	void preUpdate() { lastModifiedAt = new Date(); }


	public Long getIds() {
		return ids;
	}


	public void setIds(Long ids) {
		this.ids = ids;
	}


	public Date getDate() {
		return date;
	}


	public void setDate(Date date) {
		this.date = date;
	}


	public String getHeure() {
		return heure;
	}


	public void setHeure(String heure) {
		this.heure = heure;
	}


	public Salle getSalle() {
		return salle;
	}


	public void setSalle(Salle salle) {
		this.salle = salle;
	}


	public Etudiant getEtudiant() {
		return etudiant;
	}


	public void setEtudiant(Etudiant etudiant) {
		this.etudiant = etudiant;
	}


	public Jury getJury() {
		return jury;
	}


	public void setJury(Jury jury) {
		this.jury = jury;
	}

	public SoutenanceStatus getStatus() {
		return status == null ? SoutenanceStatus.PLANNED : status;
	}

	public void setStatus(SoutenanceStatus status) {
		this.status = status == null ? SoutenanceStatus.PLANNED : status;
	}

	public boolean isLocked() { return locked != null ? locked : false; }
	public void setLocked(Boolean locked) { this.locked = locked; }

	public boolean isManualOverride() { return manualOverride != null ? manualOverride : false; }
	public void setManualOverride(Boolean v) { this.manualOverride = v; }

	public PlanningVersion getVersion() { return version; }
	public void setVersion(PlanningVersion v) { this.version = v; }

	public AcademicSession getSession() { return session; }
	public void setSession(AcademicSession s) { this.session = s; }

	public int getPriority() { return priority != null ? priority : 0; }
	public void setPriority(Integer priority) { this.priority = priority; }

	public String getComment() { return comment; }
	public void setComment(String v) { this.comment = v; }

	public Date getConvocationSentAt() { return convocationSentAt; }
	public void setConvocationSentAt(Date v) { this.convocationSentAt = v; }

	public Date getReminderSentAt() { return reminderSentAt; }
	public void setReminderSentAt(Date v) { this.reminderSentAt = v; }

	public Long getLockedById() { return lockedById; }
	public void setLockedById(Long v) { this.lockedById = v; }

	public Date getLockedAt() { return lockedAt; }
	public void setLockedAt(Date v) { this.lockedAt = v; }

	public Long getCreatedById() { return createdById; }
	public void setCreatedById(Long v) { this.createdById = v; }

	public Date getLastModifiedAt() { return lastModifiedAt; }
	public void setLastModifiedAt(Date v) { this.lastModifiedAt = v; }

	public Long getLastModifiedById() { return lastModifiedById; }
	public void setLastModifiedById(Long v) { this.lastModifiedById = v; }

	/** True if the soutenance is frozen (published or completed). */
	public boolean isFrozen() {
		if (Boolean.TRUE.equals(locked)) return true;
		SoutenanceStatus st = getStatus();
		if (st == SoutenanceStatus.LOCKED || st == SoutenanceStatus.COMPLETED) return true;
		return version != null && version.isFrozen();
	}


	@Override
	public String toString() {
		return "Soutenance [ids=" + ids + ", date=" + date + ", heure=" + heure + ", salle=" + salle + ", etudiant="
				+ etudiant + ", jury=" + jury + ", status=" + status + "]";
	}
    
    
}
