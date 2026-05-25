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

    @Column(length = 255)
    private String email;

    @Column(length = 64)
    private String phone;

    /** Academic grade (PES / PH / PA / MC / MA / VAC / EXTERNE). */
    @Enumerated(EnumType.STRING)
    @Column(length = 16, name = "grade")
    private ProfesseurGrade grade = ProfesseurGrade.AUTRE;

    /** True for professors of the establishment, false for external/industry guests. */
    @Column(name = "internal")
    private Boolean internal = true;

    /** Comma-separated language codes: "fr,en,ar". */
    @Column(name = "languages", length = 128)
    private String languages;

    /** Maximum number of soutenances/day this professor accepts. Null = use global default. */
    @Column(name = "max_soutenances_per_day")
    private Integer maxSoutenancesPerDay;

    /** When true, the professor is temporarily excluded from any new jury. */
    @Column(name = "excluded")
    private Boolean excluded = false;

    @Column(length = 1024, name = "exclusion_reason")
    private String exclusionReason;

    /** Soft hint: VIP professors are preferred for some defenses but not required. */
    @Column(name = "vip")
    private Boolean vip = false;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

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

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }

	public ProfesseurGrade getGrade() {
		return grade == null ? ProfesseurGrade.AUTRE : grade;
	}

	public void setGrade(ProfesseurGrade grade) {
		this.grade = grade == null ? ProfesseurGrade.AUTRE : grade;
	}

	public boolean isInternal() { return internal != null ? internal : true; }
	public void setInternal(Boolean internal) { this.internal = internal; }

	public String getLanguages() { return languages; }
	public void setLanguages(String languages) { this.languages = languages; }

	public Integer getMaxSoutenancesPerDay() { return maxSoutenancesPerDay; }
	public void setMaxSoutenancesPerDay(Integer v) { this.maxSoutenancesPerDay = v; }

	public boolean isExcluded() { return excluded != null ? excluded : false; }
	public void setExcluded(Boolean excluded) { this.excluded = excluded; }

	public String getExclusionReason() { return exclusionReason; }
	public void setExclusionReason(String exclusionReason) { this.exclusionReason = exclusionReason; }

	public boolean isVip() { return vip != null ? vip : false; }
	public void setVip(Boolean vip) { this.vip = vip; }

	public Department getDepartment() { return department; }
	public void setDepartment(Department department) { this.department = department; }

	public boolean speaksLanguage(String code) {
		if (code == null || languages == null) return false;
		String c = code.trim().toLowerCase();
		for (String token : languages.split("[,;\\s]+")) {
			if (token.trim().toLowerCase().equals(c)) return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "Professeur [discipline=" + discipline + ", idp=" + idp + ", nom=" + nom + ", prenom=" + prenom + "]";
	}
    
}
