package entities;


import jakarta.persistence.*;

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

    public Affectation() {}

	public Affectation(Long ida, Etudiant etudiant, Professeur encadrant) {
		super();
		this.ida = ida;
		this.etudiant = etudiant;
		this.encadrant = encadrant;
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

	
    
}