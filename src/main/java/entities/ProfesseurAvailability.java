package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Professor unavailability / preference declaration for a given session.
 * Replaces the previous "global excluded dates" approach with per-prof,
 * per-session granularity that the planning engine consumes.
 */
@Entity
@Table(name = "professeur_availability",
        indexes = { @Index(name = "idx_avail_prof", columnList = "professeur_id") })
public class ProfesseurAvailability {

    public enum Period { ALL_DAY, MORNING, AFTERNOON }
    public enum Kind   { UNAVAILABLE, PREFERRED, EXCLUDED_GLOBAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professeur_id")
    private Professeur professeur;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private AcademicSession session;   // null = applies to every session

    @Temporal(TemporalType.DATE)
    @Column(name = "the_date")
    private Date theDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Period period = Period.ALL_DAY;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Kind kind = Kind.UNAVAILABLE;

    @Column(length = 1024)
    private String reason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "declared_at")
    private Date declaredAt;

    @Column(name = "declared_by_id")
    private Long declaredById;

    public ProfesseurAvailability() {}

    @PrePersist
    void prePersist() {
        if (declaredAt == null) declaredAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Professeur getProfesseur() { return professeur; }
    public void setProfesseur(Professeur p) { this.professeur = p; }
    public AcademicSession getSession() { return session; }
    public void setSession(AcademicSession s) { this.session = s; }
    public Date getTheDate() { return theDate; }
    public void setTheDate(Date v) { this.theDate = v; }
    public Period getPeriod() { return period; }
    public void setPeriod(Period v) { this.period = v == null ? Period.ALL_DAY : v; }
    public Kind getKind() { return kind; }
    public void setKind(Kind v) { this.kind = v == null ? Kind.UNAVAILABLE : v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public Date getDeclaredAt() { return declaredAt; }
    public void setDeclaredAt(Date v) { this.declaredAt = v; }
    public Long getDeclaredById() { return declaredById; }
    public void setDeclaredById(Long v) { this.declaredById = v; }
}
