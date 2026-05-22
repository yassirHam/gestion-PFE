package entities;


import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "jury")
public class Jury {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idJury;

    @ManyToOne
    @JoinColumn(name = "id_president")
    private Professeur president;

    @ManyToOne
    @JoinColumn(name = "id_rapporteur1")
    private Professeur rapporteur1;

    @ManyToOne
    @JoinColumn(name = "id_rapporteur2")
    private Professeur rapporteur2;

    /** Optional invited / external member (e.g. industry guest). */
    @ManyToOne
    @JoinColumn(name = "id_invite")
    private Professeur invite;

    /** Locked juries cannot be re-shuffled by the auto-planner. */
    @Column(name = "locked")
    private boolean locked;

    @Column(name = "manual_override")
    private boolean manualOverride;

    /** When non-null, this jury is a replacement of {@link #replacesJuryId}. */
    @Column(name = "replaces_jury_id")
    private Long replacesJuryId;

    @Column(length = 1024, name = "swap_reason")
    private String swapReason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified_at")
    private Date lastModifiedAt;

    @Column(name = "last_modified_by_id")
    private Long lastModifiedById;

    public Jury() {}

	public Jury(Long idJury, Professeur president, Professeur rapporteur1, Professeur rapporteur2) {
		super();
		this.idJury = idJury;
		this.president = president;
		this.rapporteur1 = rapporteur1;
		this.rapporteur2 = rapporteur2;
	}

	@PrePersist
	void prePersist() {
		if (lastModifiedAt == null) lastModifiedAt = new Date();
	}

	@PreUpdate
	void preUpdate() { lastModifiedAt = new Date(); }

	
	public Long getIdJury() {
		return idJury;
	}

	public void setIdJury(Long idJury) {
		this.idJury = idJury;
	}

	public Professeur getPresident() {
		return president;
	}

	public void setPresident(Professeur president) {
		this.president = president;
	}

	public Professeur getRapporteur1() {
		return rapporteur1;
	}

	public void setRapporteur1(Professeur rapporteur1) {
		this.rapporteur1 = rapporteur1;
	}

	public Professeur getRapporteur2() {
		return rapporteur2;
	}

	public void setRapporteur2(Professeur rapporteur2) {
		this.rapporteur2 = rapporteur2;
	}

	public Professeur getInvite() { return invite; }
	public void setInvite(Professeur invite) { this.invite = invite; }

	public boolean isLocked() { return locked; }
	public void setLocked(boolean locked) { this.locked = locked; }

	public boolean isManualOverride() { return manualOverride; }
	public void setManualOverride(boolean manualOverride) { this.manualOverride = manualOverride; }

	public Long getReplacesJuryId() { return replacesJuryId; }
	public void setReplacesJuryId(Long replacesJuryId) { this.replacesJuryId = replacesJuryId; }

	public String getSwapReason() { return swapReason; }
	public void setSwapReason(String swapReason) { this.swapReason = swapReason; }

	public Date getLastModifiedAt() { return lastModifiedAt; }
	public void setLastModifiedAt(Date v) { this.lastModifiedAt = v; }

	public Long getLastModifiedById() { return lastModifiedById; }
	public void setLastModifiedById(Long v) { this.lastModifiedById = v; }

	/** Replace the jury member at a given role (P=president, R1, R2, INV). */
	public void replaceMember(String role, Professeur replacement) {
		if (role == null) return;
		switch (role.toUpperCase()) {
			case "P":   this.president = replacement; break;
			case "R1":  this.rapporteur1 = replacement; break;
			case "R2":  this.rapporteur2 = replacement; break;
			case "INV": this.invite = replacement; break;
		}
	}

	public boolean contains(Professeur p) {
		if (p == null || p.getIdp() == null) return false;
		Long id = p.getIdp();
		if (president != null && id.equals(president.getIdp())) return true;
		if (rapporteur1 != null && id.equals(rapporteur1.getIdp())) return true;
		if (rapporteur2 != null && id.equals(rapporteur2.getIdp())) return true;
		if (invite != null && id.equals(invite.getIdp())) return true;
		return false;
	}

	@Override
	public String toString() {
		return "Jury [idJury=" + idJury + ", president=" + president + ", rapporteur1=" + rapporteur1 + ", rapporteur2="
				+ rapporteur2 + ", invite=" + invite + "]";
	}
    
}
