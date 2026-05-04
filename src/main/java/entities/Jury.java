package entities;


import jakarta.persistence.*;

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

    public Jury() {}

	public Jury(Long idJury, Professeur president, Professeur rapporteur1, Professeur rapporteur2) {
		super();
		this.idJury = idJury;
		this.president = president;
		this.rapporteur1 = rapporteur1;
		this.rapporteur2 = rapporteur2;
	}

	
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

	@Override
	public String toString() {
		return "Jury [idJury=" + idJury + ", president=" + president + ", rapporteur1=" + rapporteur1 + ", rapporteur2="
				+ rapporteur2 + "]";
	}
    
}