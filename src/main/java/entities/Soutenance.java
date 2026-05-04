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


	@Override
	public String toString() {
		return "Soutenance [ids=" + ids + ", date=" + date + ", heure=" + heure + ", salle=" + salle + ", etudiant="
				+ etudiant + ", jury=" + jury + "]";
	}
    
    
}