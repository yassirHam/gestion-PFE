package entities;

import jakarta.persistence.*;

/**
 * Organizational unit (département / faculté / campus) that owns rooms,
 * professors, filières and quotas. Introduces the governance layer that
 * was missing in the mono-admin model.
 */
@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String code;

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String campus;

    @Column(name = "head_user_id")
    private Long headUserId;       // chef de département (links to AppUser.id)

    @Column(name = "max_soutenances_per_day")
    private Integer maxSoutenancesPerDay;

    @Column(name = "max_external_members")
    private Integer maxExternalMembers;

    @Column(name = "filiere_codes", length = 1024)
    private String filiereCodes;   // comma-separated filière codes owned by this dept

    @Column(name = "active")
    private boolean active = true;

    public Department() {}

    public Department(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public Long getHeadUserId() { return headUserId; }
    public void setHeadUserId(Long headUserId) { this.headUserId = headUserId; }
    public Integer getMaxSoutenancesPerDay() { return maxSoutenancesPerDay; }
    public void setMaxSoutenancesPerDay(Integer v) { this.maxSoutenancesPerDay = v; }
    public Integer getMaxExternalMembers() { return maxExternalMembers; }
    public void setMaxExternalMembers(Integer v) { this.maxExternalMembers = v; }
    public String getFiliereCodes() { return filiereCodes; }
    public void setFiliereCodes(String v) { this.filiereCodes = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public java.util.List<String> getFiliereList() {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (filiereCodes == null) return out;
        for (String token : filiereCodes.split("[,;\\s]+")) {
            String t = token.trim();
            if (!t.isEmpty()) out.add(t.toUpperCase());
        }
        return out;
    }
}
