package entities;


import jakarta.persistence.*;

@Entity
@Table(name = "professeur")
public class Professeur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idp;

    private String nom;
    private String prenom;
    private String discipline;
    private String specialite ;

    public Professeur() {}

    public Professeur(Long idp, String nom, String prenom, String discipline, String specialite) {
		super();
		this.idp = idp;
		this.nom = nom;
		this.prenom = prenom;
		this.discipline = discipline;
		this.specialite = specialite;
	}

	public Long getIdp() {
		return idp;
	}

	public void setIdp(Long idp) {
		this.idp = idp;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public String getDiscipline() {
		return discipline;
	}

	public void setDiscipline(String discipline) {
		this.discipline = discipline;
	}

	

	public String getSpecialite() {
		return specialite;
	}

	public void setSpecialite(String specialite) {
		this.specialite = specialite;
	}

	@Override
	public String toString() {
		return "Professeur [discipline=" + discipline + ", idp=" + idp + ", nom=" + nom + ", prenom=" + prenom + "]";
	}
    
}