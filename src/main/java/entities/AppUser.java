package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Application user with role-based permissions. Replaces the previous
 * mono-admin assumption.
 *
 * <p>Passwords are stored as PBKDF2 hashes via
 * {@code services.AuthService.hashPassword}. Plaintext is never persisted.</p>
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

    @Column(length = 512, name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private UserRole role = UserRole.CONSULTATION;

    /** Optional link to the underlying Professeur record (when the user is a teacher). */
    @ManyToOne
    @JoinColumn(name = "professeur_id")
    private Professeur professeur;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    /** Comma-separated filière codes the user is responsible for. */
    @Column(name = "scoped_filieres", length = 1024)
    private String scopedFilieres;

    @Column(name = "active")
    private boolean active = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_login_at")
    private Date lastLoginAt;

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
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole r) { this.role = r == null ? UserRole.CONSULTATION : r; }
    public Professeur getProfesseur() { return professeur; }
    public void setProfesseur(Professeur p) { this.professeur = p; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department d) { this.department = d; }
    public String getScopedFilieres() { return scopedFilieres; }
    public void setScopedFilieres(String v) { this.scopedFilieres = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date v) { this.lastLoginAt = v; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date v) { this.createdAt = v; }

    public java.util.List<String> getScopedFiliereList() {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (scopedFilieres == null) return out;
        for (String token : scopedFilieres.split("[,;\\s]+")) {
            String t = token.trim();
            if (!t.isEmpty()) out.add(t.toUpperCase());
        }
        return out;
    }

    public boolean canSeeFiliere(String filiere) {
        if (filiere == null) return true;
        if (role == UserRole.ADMIN_PEDAGOGIQUE || role == UserRole.CHEF_DEPARTEMENT) return true;
        java.util.List<String> scoped = getScopedFiliereList();
        if (scoped.isEmpty()) return true;
        return scoped.contains(filiere.toUpperCase());
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.isEmpty()) return fullName;
        return username;
    }
}
