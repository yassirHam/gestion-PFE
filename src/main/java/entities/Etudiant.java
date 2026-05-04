package entities;


import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "etudiant")
public class Etudiant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ide;

    private String cne;
    private String nomE;
    private String prenomE;
    private String filiere;
    private String email;
    private String sujet_stage;

    @Temporal(TemporalType.DATE)
    private Date date_debut_stage;

    public Etudiant() {}
    
    
    public Etudiant(Long ide, String cne, String nomE, String prenomE, String filiere, String email, String sujet_stage,
			Date date_debut_stage) {
		super();
		this.ide = ide;
		this.cne = cne;
		this.nomE = nomE;
		this.prenomE = prenomE;
		this.filiere = filiere;
		this.email = email;
		this.sujet_stage = sujet_stage;
		this.date_debut_stage = date_debut_stage;
	}


	public Long getIde() {
		return ide;
	}

	public void setIde(Long ide) {
		this.ide = ide;
	}

	public String getCne() {
		return cne;
	}

	public void setCne(String cne) {
		this.cne = cne;
	}

	public String getNomE() {
		return nomE;
	}

	public void setNomE(String nomE) {
		this.nomE = nomE;
	}

	public String getPrenomE() {
		return prenomE;
	}

	public void setPrenomE(String prenomE) {
		this.prenomE = prenomE;
	}

	public String getFiliere() {
		return filiere;
	}

	public void setFiliere(String filiere) {
		this.filiere = filiere;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getDate_debut_stage() {
		return date_debut_stage;
	}

	public void setDate_debut_stage(Date date_debut_stage) {
		this.date_debut_stage = date_debut_stage;
	}

	public String getSujet_stage() {
		return sujet_stage;
	}

	public void setSujet_stage(String sujet_stage) {
		this.sujet_stage = sujet_stage;
	}

	
    
    
}