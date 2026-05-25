package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Operator metadata retained for audit history and ownership fields.
 *
 * <p>The application no longer authenticates requests. Existing operator
 * records remain useful as historical actors in the audit log.</p>
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false, unique = true)
    private String username;

    @Column(length = 255)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private UserRole role = UserRole.CONSULTATION;

    /** Optional link to the underlying Professeur record for audit context. */
    @ManyToOne
    @JoinColumn(name = "professeur_id")
    private Professeur professeur;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "active")
    private boolean active = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    public AppUser() {}

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole r) { this.role = r == null ? UserRole.CONSULTATION : r; }
    public Professeur getProfesseur() { return professeur; }
    public void setProfesseur(Professeur p) { this.professeur = p; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department d) { this.department = d; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { this.createdAt = v; }

    public String getDisplayName() {
        if (fullName != null && !fullName.isEmpty()) return fullName;
        return username;
    }
}
