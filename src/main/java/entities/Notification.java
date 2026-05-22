package entities;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Outgoing notification (email convocation, reminder, internal message).
 * The {@code NotificationService} produces the message, persists it here,
 * then dispatches it through the configured transport (SMTP if available,
 * otherwise stored as a "queued" record for manual download).
 */
@Entity
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_notif_status", columnList = "status"),
                @Index(name = "idx_notif_sent_at", columnList = "sent_at")
        })
public class Notification {

    public enum Channel { EMAIL, INTERNAL }
    public enum Status  { QUEUED, SENT, FAILED, CANCELLED }
    public enum Kind {
        CONVOCATION_STUDENT,
        CONVOCATION_JURY,
        REMINDER_DEFENSE,
        EXCEPTION_NOTICE,
        APPROVAL_REQUEST,
        PUBLICATION_ANNOUNCE,
        GENERIC
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Channel channel = Channel.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Kind kind = Kind.GENERIC;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Status status = Status.QUEUED;

    @Column(length = 255)
    private String recipient;       // email address or username

    @Column(length = 255, name = "recipient_name")
    private String recipientName;

    @Column(length = 255)
    private String subject;

    @Lob
    @Column(name = "body")
    private String body;

    @Column(name = "soutenance_id")
    private Long soutenanceId;

    @Column(name = "session_id")
    private Long sessionId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "queued_at")
    private Date queuedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sent_at")
    private Date sentAt;

    @Column(length = 1024, name = "error_message")
    private String errorMessage;

    public Notification() {}

    @PrePersist
    void prePersist() {
        if (queuedAt == null) queuedAt = new Date();
        if (status == null) status = Status.QUEUED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel v) { this.channel = v; }
    public Kind getKind() { return kind; }
    public void setKind(Kind v) { this.kind = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String v) { this.recipient = v; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String v) { this.recipientName = v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { this.subject = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public Long getSoutenanceId() { return soutenanceId; }
    public void setSoutenanceId(Long v) { this.soutenanceId = v; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long v) { this.sessionId = v; }
    public Date getQueuedAt() { return queuedAt; }
    public void setQueuedAt(Date v) { this.queuedAt = v; }
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date v) { this.sentAt = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
}
