package entities;

import jakarta.persistence.*;

/**
 * Global application settings for the establishment using this deployment.
 *
 * <p>Stores branding (institution name, sub-title, academic year, logo) and
 * infrastructure configuration (history storage location, optional NLP API key)
 * so the application can be deployed without being tied to a specific school.</p>
 *
 * <p>Only ONE row of this table is ever used (id = 1). The {@code AppSettingsService}
 * loads it on startup and creates a default row if none exists.</p>
 */
@Entity
@Table(name = "app_settings")
public class AppSettings {

    public static final long SINGLETON_ID = 1L;

    public enum StorageMode {
        LOCAL,
        S3
    }

    @Id
    private Long id = SINGLETON_ID;

    // ─── Branding ───────────────────────────────────────────────────────────
    @Column(length = 255)
    private String institutionName;

    @Column(length = 255)
    private String institutionSubtitle;

    @Column(length = 64)
    private String academicYear;

    @Column(length = 64, name = "doc_title_affectation")
    private String documentTitleAffectation;

    @Column(length = 64, name = "doc_title_planning")
    private String documentTitlePlanning;

    @Lob
    @Column(name = "logo_bytes", columnDefinition = "LONGBLOB")
    private byte[] logoBytes;

    @Column(length = 64)
    private String logoMimeType;

    // ─── History storage ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private StorageMode storageMode = StorageMode.LOCAL;

    @Column(length = 1024)
    private String localStoragePath;

    @Column(length = 255)
    private String s3Endpoint;

    @Column(length = 255)
    private String s3Region;

    @Column(length = 255)
    private String s3Bucket;

    @Column(length = 255)
    private String s3AccessKey;

    @Column(length = 1024)
    private String s3SecretKey;

    @Column(length = 255)
    private String s3Prefix;

    @Column(name = "s3_path_style")
    private Boolean s3PathStyleAccess = true;

    // ─── NLP / AI ───────────────────────────────────────────────────────────
    @Column(name = "nlp_enabled")
    private Boolean nlpEnabled = false;

    @Column(length = 1024)
    private String nlpApiKey;

    @Column(length = 255)
    private String nlpBaseUrl;

    @Column(length = 255)
    private String nlpModel;

    // ─── SMTP / notifications ───────────────────────────────────────────────
    @Column(name = "smtp_enabled")
    private Boolean smtpEnabled = false;

    @Column(length = 255, name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(length = 255, name = "smtp_username")
    private String smtpUsername;

    @Column(length = 1024, name = "smtp_password")
    private String smtpPassword;

    @Column(length = 255, name = "smtp_from")
    private String smtpFrom;

    @Column(name = "smtp_starttls")
    private Boolean smtpStartTls = true;

    // ─── Multi-tenant / governance ──────────────────────────────────────────
    @Column(name = "active_session_id")
    private Long activeSessionId;

    @Column(length = 64, name = "default_campus")
    private String defaultCampus;

    @Column(name = "freeze_on_publish")
    private Boolean freezeOnPublish = true;

    @Column(name = "require_approval_chain")
    private Boolean requireApprovalChain = true;

    // ─── Planning configuration (persisted across server restarts) ─────────
    @Column(name = "planning_number_of_days")
    private Integer planningNumberOfDays;

    @Column(length = 32, name = "planning_start_date")
    private String planningStartDate;     // ISO yyyy-MM-dd

    @Column(name = "planning_start_hour")
    private Integer planningStartHour;

    @Column(name = "planning_end_hour")
    private Integer planningEndHour;

    @Column(name = "planning_duration_minutes")
    private Integer planningDurationMinutes;

    @Column(name = "planning_break_minutes")
    private Integer planningBreakMinutes;

    @Lob
    @Column(name = "planning_constraints_json", columnDefinition = "LONGTEXT")
    private String planningConstraintsJson;

    public AppSettings() {}

    // ─── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String v) { this.institutionName = v; }

    public String getInstitutionSubtitle() { return institutionSubtitle; }
    public void setInstitutionSubtitle(String v) { this.institutionSubtitle = v; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String v) { this.academicYear = v; }

    public String getDocumentTitleAffectation() { return documentTitleAffectation; }
    public void setDocumentTitleAffectation(String v) { this.documentTitleAffectation = v; }

    public String getDocumentTitlePlanning() { return documentTitlePlanning; }
    public void setDocumentTitlePlanning(String v) { this.documentTitlePlanning = v; }

    public byte[] getLogoBytes() { return logoBytes; }
    public void setLogoBytes(byte[] v) { this.logoBytes = v; }

    public String getLogoMimeType() { return logoMimeType; }
    public void setLogoMimeType(String v) { this.logoMimeType = v; }

    public StorageMode getStorageMode() { return storageMode; }
    public void setStorageMode(StorageMode v) { this.storageMode = v; }

    public String getLocalStoragePath() { return localStoragePath; }
    public void setLocalStoragePath(String v) { this.localStoragePath = v; }

    public String getS3Endpoint() { return s3Endpoint; }
    public void setS3Endpoint(String v) { this.s3Endpoint = v; }

    public String getS3Region() { return s3Region; }
    public void setS3Region(String v) { this.s3Region = v; }

    public String getS3Bucket() { return s3Bucket; }
    public void setS3Bucket(String v) { this.s3Bucket = v; }

    public String getS3AccessKey() { return s3AccessKey; }
    public void setS3AccessKey(String v) { this.s3AccessKey = v; }

    public String getS3SecretKey() { return s3SecretKey; }
    public void setS3SecretKey(String v) { this.s3SecretKey = v; }

    public String getS3Prefix() { return s3Prefix; }
    public void setS3Prefix(String v) { this.s3Prefix = v; }

    public Boolean getS3PathStyleAccess() { return s3PathStyleAccess != null && s3PathStyleAccess; }
    public void setS3PathStyleAccess(Boolean v) { this.s3PathStyleAccess = v; }

    public Boolean getNlpEnabled() { return nlpEnabled != null && nlpEnabled; }
    public void setNlpEnabled(Boolean v) { this.nlpEnabled = v; }

    public String getNlpApiKey() { return nlpApiKey; }
    public void setNlpApiKey(String v) { this.nlpApiKey = v; }

    public String getNlpBaseUrl() { return nlpBaseUrl; }
    public void setNlpBaseUrl(String v) { this.nlpBaseUrl = v; }

    public String getNlpModel() { return nlpModel; }
    public void setNlpModel(String v) { this.nlpModel = v; }

    public boolean hasLogo() { return logoBytes != null && logoBytes.length > 0; }

    // ─── SMTP getters/setters ──────────────────────────────────────────────
    public Boolean getSmtpEnabled() { return smtpEnabled != null && smtpEnabled; }
    public void setSmtpEnabled(Boolean v) { this.smtpEnabled = v; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String v) { this.smtpHost = v; }
    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer v) { this.smtpPort = v; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String v) { this.smtpUsername = v; }
    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String v) { this.smtpPassword = v; }
    public String getSmtpFrom() { return smtpFrom; }
    public void setSmtpFrom(String v) { this.smtpFrom = v; }
    public Boolean getSmtpStartTls() { return smtpStartTls != null && smtpStartTls; }
    public void setSmtpStartTls(Boolean v) { this.smtpStartTls = v; }

    // ─── Tenant / governance ───────────────────────────────────────────────
    public Long getActiveSessionId() { return activeSessionId; }
    public void setActiveSessionId(Long v) { this.activeSessionId = v; }
    public String getDefaultCampus() { return defaultCampus; }
    public void setDefaultCampus(String v) { this.defaultCampus = v; }
    public Boolean getFreezeOnPublish() { return freezeOnPublish != null && freezeOnPublish; }
    public void setFreezeOnPublish(Boolean v) { this.freezeOnPublish = v; }
    public Boolean getRequireApprovalChain() { return requireApprovalChain != null && requireApprovalChain; }
    public void setRequireApprovalChain(Boolean v) { this.requireApprovalChain = v; }

    // ─── Planning configuration ────────────────────────────────────────────
    public Integer getPlanningNumberOfDays() { return planningNumberOfDays; }
    public void setPlanningNumberOfDays(Integer v) { this.planningNumberOfDays = v; }
    public String getPlanningStartDate() { return planningStartDate; }
    public void setPlanningStartDate(String v) { this.planningStartDate = v; }
    public Integer getPlanningStartHour() { return planningStartHour; }
    public void setPlanningStartHour(Integer v) { this.planningStartHour = v; }
    public Integer getPlanningEndHour() { return planningEndHour; }
    public void setPlanningEndHour(Integer v) { this.planningEndHour = v; }
    public Integer getPlanningDurationMinutes() { return planningDurationMinutes; }
    public void setPlanningDurationMinutes(Integer v) { this.planningDurationMinutes = v; }
    public Integer getPlanningBreakMinutes() { return planningBreakMinutes; }
    public void setPlanningBreakMinutes(Integer v) { this.planningBreakMinutes = v; }
    public String getPlanningConstraintsJson() { return planningConstraintsJson; }
    public void setPlanningConstraintsJson(String v) { this.planningConstraintsJson = v; }
}
