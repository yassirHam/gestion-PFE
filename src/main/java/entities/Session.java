package entities;


import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "session_pfe")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_ses;

    @OneToOne
    @JoinColumn(name = "ide")
    private Etudiant etudiant;

    @Temporal(TemporalType.DATE)
    private Date date_soutenance;

    private int numeroSession; 
    public Session() {}

	public Session(Long id_ses, Etudiant etudiant, Date date_soutenance, int numeroSession) {
		super();
		this.id_ses = id_ses;
		this.etudiant = etudiant;
		this.date_soutenance = date_soutenance;
		this.numeroSession = numeroSession;
	}

	public Long getId_ses() {
		return id_ses;
	}

	public void setId_ses(Long id_ses) {
		this.id_ses = id_ses;
	}

	public Etudiant getEtudiant() {
		return etudiant;
	}

	public void setEtudiant(Etudiant etudiant) {
		this.etudiant = etudiant;
	}

	public Date getDate_soutenance() {
		return date_soutenance;
	}

	public void setDate_soutenance(Date date_soutenance) {
		this.date_soutenance = date_soutenance;
	}

	public int getNumeroSession() {
		return numeroSession;
	}

	public void setNumeroSession(int numeroSession) {
		this.numeroSession = numeroSession;
	}
    
    
}