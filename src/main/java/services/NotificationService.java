package services;

import dao.NotificationDAO;
import dao.NotificationDAOImpl;
import dao.SoutenanceDAO;
import dao.SoutenanceDAOImpl;
import entities.AppSettings;
import entities.AppUser;
import entities.AuditAction;
import entities.Etudiant;
import entities.Jury;
import entities.Notification;
import entities.Professeur;
import entities.Soutenance;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;

/**
 * Communication workflow service. Generates and persists notifications
 * (convocations, reminders, exception notices), and dispatches them via
 * SMTP when configured. When SMTP is not configured, notifications are
 * stored as {@code QUEUED} records that operators can review/download
 * from the UI — preserving the audit chain even in air-gapped setups.
 *
 * <p>The SMTP transport is implemented in plain Java (no jakarta-mail
 * dependency) so the project keeps its current zero-extra-deps profile
 * for built-in features.</p>
 */
public final class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    private static final NotificationService INSTANCE =
            new NotificationService(new NotificationDAOImpl(), new SoutenanceDAOImpl());

    private final NotificationDAO dao;
    private final SoutenanceDAO soutenanceDao;

    NotificationService(NotificationDAO dao, SoutenanceDAO soutenanceDao) {
        this.dao = Objects.requireNonNull(dao);
        this.soutenanceDao = Objects.requireNonNull(soutenanceDao);
    }

    public static NotificationService getInstance() { return INSTANCE; }

    public List<Notification> recent(int limit) { return dao.findRecent(limit); }
    public int countQueued() { return dao.countByStatus(Notification.Status.QUEUED); }

    /**
     * Queue student & jury convocations for a single soutenance. Returns
     * the number of recipients queued.
     */
    public int queueConvocations(Soutenance s, AppUser actor) {
        if (s == null) return 0;
        int n = 0;
        Etudiant et = s.getEtudiant();
        if (et != null && et.getEmail() != null && !et.getEmail().isBlank()) {
            Notification ns = new Notification();
            ns.setKind(Notification.Kind.CONVOCATION_STUDENT);
            ns.setRecipient(et.getEmail());
            ns.setRecipientName((et.getNomE() == null ? "" : et.getNomE()) + " "
                    + (et.getPrenomE() == null ? "" : et.getPrenomE()));
            ns.setSubject(buildStudentSubject(s));
            ns.setBody(buildStudentBody(s));
            ns.setSoutenanceId(s.getIds());
            ns.setSessionId(s.getSession() == null ? null : s.getSession().getId());
            dao.save(ns);
            n++;
        }
        Jury jury = s.getJury();
        if (jury != null) {
            n += queueJuryMember(s, jury.getPresident(), "Président");
            n += queueJuryMember(s, jury.getRapporteur1(), "Rapporteur");
            n += queueJuryMember(s, jury.getRapporteur2(), "Rapporteur");
            n += queueJuryMember(s, jury.getInvite(), "Invité");
        }
        if (n > 0) {
            soutenanceDao.touchConvocation(s.getIds(), new Date());
            AuditService.getInstance().record(actor, AuditAction.NOTIFICATION_SENT,
                    "Soutenance", s.getIds(), n + " convocation(s) mises en file");
        }
        return n;
    }

    public int queueConvocationsForAll(List<Soutenance> soutenances, AppUser actor) {
        if (soutenances == null) return 0;
        int total = 0;
        for (Soutenance s : soutenances) total += queueConvocations(s, actor);
        return total;
    }

    public int queueReminders(List<Soutenance> soutenances, AppUser actor) {
        if (soutenances == null) return 0;
        int n = 0;
        for (Soutenance s : soutenances) {
            Etudiant et = s.getEtudiant();
            if (et != null && et.getEmail() != null && !et.getEmail().isBlank()) {
                Notification ns = new Notification();
                ns.setKind(Notification.Kind.REMINDER_DEFENSE);
                ns.setRecipient(et.getEmail());
                ns.setRecipientName((et.getNomE() == null ? "" : et.getNomE()) + " "
                        + (et.getPrenomE() == null ? "" : et.getPrenomE()));
                ns.setSubject("Rappel : votre soutenance approche");
                ns.setBody(buildReminderBody(s));
                ns.setSoutenanceId(s.getIds());
                dao.save(ns);
                n++;
            }
        }
        if (n > 0) {
            for (Soutenance s : soutenances) {
                soutenanceDao.touchReminder(s.getIds(), new Date());
            }
            AuditService.getInstance().record(actor, AuditAction.REMINDER_SENT,
                    null, null, n + " rappel(s) mis en file");
        }
        return n;
    }

    /**
     * Try to dispatch every QUEUED notification through SMTP. Returns the
     * number of successfully sent items. When SMTP is not configured, every
     * notification stays QUEUED and the caller can surface that in the UI.
     */
    public DispatchResult dispatchQueued(AppUser actor) {
        DispatchResult result = new DispatchResult();
        AppSettings settings = AppSettingsService.getInstance().get();
        if (!settings.isSmtpEnabled() || isBlank(settings.getSmtpHost())) {
            result.skipped = dao.countByStatus(Notification.Status.QUEUED);
            return result;
        }
        List<Notification> queued = dao.findQueued();
        for (Notification n : queued) {
            try {
                sendViaSmtp(settings, n);
                dao.updateStatus(n.getId(), Notification.Status.SENT, null);
                result.sent++;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "SMTP send failed for notification " + n.getId(), e);
                dao.updateStatus(n.getId(), Notification.Status.FAILED, e.getMessage());
                result.failed++;
            }
        }
        if (result.sent > 0) {
            AuditService.getInstance().record(actor, AuditAction.NOTIFICATION_SENT,
                    null, null, result.sent + " notification(s) envoyée(s) via SMTP");
        }
        return result;
    }

    public List<Notification> findBySoutenance(Long soutenanceId) {
        return dao.findBySoutenance(soutenanceId);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private int queueJuryMember(Soutenance s, Professeur p, String role) {
        if (p == null) return 0;
        if (p.getEmail() == null || p.getEmail().isBlank()) return 0;
        Notification ns = new Notification();
        ns.setKind(Notification.Kind.CONVOCATION_JURY);
        ns.setRecipient(p.getEmail());
        ns.setRecipientName(p.getNom() + " " + p.getPrenom());
        ns.setSubject(buildJurySubject(s, role));
        ns.setBody(buildJuryBody(s, role, p));
        ns.setSoutenanceId(s.getIds());
        dao.save(ns);
        return 1;
    }

    private static String safeFmtDate(Date d) {
        if (d == null) return "(date non précisée)";
        return new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(d);
    }

    private String buildStudentSubject(Soutenance s) {
        return "Convocation soutenance PFE — " + safeFmtDate(s.getDate());
    }

    private String buildStudentBody(Soutenance s) {
        AppSettings cfg = AppSettingsService.getInstance().get();
        StringBuilder sb = new StringBuilder();
        sb.append("Bonjour,\n\n");
        sb.append("Vous êtes convoqué(e) à la soutenance de votre Projet de Fin d'Études :\n\n");
        sb.append(" • Date  : ").append(safeFmtDate(s.getDate())).append("\n");
        sb.append(" • Heure : ").append(s.getHeure() == null ? "" : s.getHeure()).append("\n");
        sb.append(" • Salle : ").append(s.getSalle() == null ? "" : s.getSalle().getNum_salle()).append("\n");
        if (s.getJury() != null) sb.append(" • Jury  : ").append(juryDescription(s.getJury())).append("\n");
        if (s.getEtudiant() != null && s.getEtudiant().getSujet_stage() != null)
            sb.append(" • Sujet : ").append(s.getEtudiant().getSujet_stage()).append("\n");
        sb.append("\nMerci de vous présenter 15 minutes avant l'heure indiquée.\n\n");
        sb.append("Cordialement,\n").append(cfg.getInstitutionName() == null ? "" : cfg.getInstitutionName());
        return sb.toString();
    }

    private String buildJurySubject(Soutenance s, String role) {
        return "Convocation jury PFE (" + role + ") — " + safeFmtDate(s.getDate());
    }

    private String buildJuryBody(Soutenance s, String role, Professeur p) {
        AppSettings cfg = AppSettingsService.getInstance().get();
        StringBuilder sb = new StringBuilder();
        sb.append("Cher(e) Professeur ").append(p.getNom()).append(",\n\n");
        sb.append("Vous êtes désigné(e) comme ").append(role)
          .append(" de la soutenance suivante :\n\n");
        sb.append(" • Date  : ").append(safeFmtDate(s.getDate())).append("\n");
        sb.append(" • Heure : ").append(s.getHeure() == null ? "" : s.getHeure()).append("\n");
        sb.append(" • Salle : ").append(s.getSalle() == null ? "" : s.getSalle().getNum_salle()).append("\n");
        if (s.getEtudiant() != null) {
            sb.append(" • Étudiant : ").append(s.getEtudiant().getNomE()).append(" ")
              .append(s.getEtudiant().getPrenomE()).append("\n");
            if (s.getEtudiant().getSujet_stage() != null)
                sb.append(" • Sujet : ").append(s.getEtudiant().getSujet_stage()).append("\n");
        }
        sb.append("\nEn cas d'indisponibilité, merci de prévenir l'administration au plus vite.\n\n");
        sb.append("Cordialement,\n").append(cfg.getInstitutionName() == null ? "" : cfg.getInstitutionName());
        return sb.toString();
    }

    private String buildReminderBody(Soutenance s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bonjour,\n\n");
        sb.append("Petit rappel : votre soutenance est prévue le ").append(safeFmtDate(s.getDate()))
          .append(" à ").append(s.getHeure() == null ? "" : s.getHeure())
          .append(" en salle ").append(s.getSalle() == null ? "" : s.getSalle().getNum_salle())
          .append(".\n\nBonne préparation !\n");
        return sb.toString();
    }

    private static String juryDescription(Jury j) {
        List<String> names = new ArrayList<>();
        if (j.getPresident() != null) names.add(j.getPresident().getNom() + " " + j.getPresident().getPrenom() + " (Président)");
        if (j.getRapporteur1() != null) names.add(j.getRapporteur1().getNom() + " " + j.getRapporteur1().getPrenom());
        if (j.getRapporteur2() != null) names.add(j.getRapporteur2().getNom() + " " + j.getRapporteur2().getPrenom());
        if (j.getInvite() != null) names.add(j.getInvite().getNom() + " " + j.getInvite().getPrenom() + " (Invité)");
        return String.join(", ", names);
    }

    private static boolean isBlank(String v) { return v == null || v.trim().isEmpty(); }

    // ─── Minimal SMTP client (STARTTLS-aware) ──────────────────────────────

    private void sendViaSmtp(AppSettings cfg, Notification n) throws Exception {
        String host = cfg.getSmtpHost();
        int port = cfg.getSmtpPort() == null ? 587 : cfg.getSmtpPort();
        String user = cfg.getSmtpUsername();
        String pass = cfg.getSmtpPassword();
        String from = isBlank(cfg.getSmtpFrom()) ? user : cfg.getSmtpFrom();
        String to = n.getRecipient();
        if (isBlank(host) || isBlank(from) || isBlank(to)) {
            throw new IllegalStateException("SMTP host / from / recipient missing.");
        }

        Socket socket = new Socket(host, port);
        socket.setSoTimeout(15000);
        try {
            SmtpDialog dialog = new SmtpDialog(socket);
            dialog.expect(220);
            dialog.send("EHLO gestion-pfe");
            dialog.expectMulti(250);

            if (cfg.isSmtpStartTls()) {
                dialog.send("STARTTLS");
                dialog.expect(220);
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                javax.net.ssl.SSLSocket sslSocket = (javax.net.ssl.SSLSocket)
                        sslFactory.createSocket(socket, host, port, true);
                sslSocket.startHandshake();
                dialog = new SmtpDialog(sslSocket);
                dialog.send("EHLO gestion-pfe");
                dialog.expectMulti(250);
            }

            if (!isBlank(user) && !isBlank(pass)) {
                dialog.send("AUTH LOGIN");
                dialog.expect(334);
                dialog.send(Base64.getEncoder().encodeToString(user.getBytes("UTF-8")));
                dialog.expect(334);
                dialog.send(Base64.getEncoder().encodeToString(pass.getBytes("UTF-8")));
                dialog.expect(235);
            }

            dialog.send("MAIL FROM:<" + from + ">");
            dialog.expect(250);
            dialog.send("RCPT TO:<" + to + ">");
            dialog.expect(250);
            dialog.send("DATA");
            dialog.expect(354);

            StringBuilder data = new StringBuilder();
            data.append("From: ").append(from).append("\r\n");
            data.append("To: ").append(to).append("\r\n");
            data.append("Subject: ").append(safeHeader(n.getSubject())).append("\r\n");
            data.append("Content-Type: text/plain; charset=UTF-8\r\n");
            data.append("Content-Transfer-Encoding: 8bit\r\n");
            data.append("Date: ").append(rfc822Date(new Date())).append("\r\n");
            data.append("\r\n");
            data.append(n.getBody() == null ? "" : n.getBody().replace("\n", "\r\n"));
            data.append("\r\n.\r\n");
            dialog.sendRaw(data.toString());
            dialog.expect(250);

            dialog.send("QUIT");
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private static String safeHeader(String v) {
        if (v == null) return "";
        return v.replaceAll("[\r\n]", " ");
    }

    private static String rfc822Date(Date d) {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        return fmt.format(d);
    }

    /** Tiny helper that owns the in/out streams of an SMTP socket. */
    private static final class SmtpDialog {
        private final java.io.BufferedReader in;
        private final PrintStream out;
        SmtpDialog(Socket socket) throws Exception {
            in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), "UTF-8"));
            OutputStream os = socket.getOutputStream();
            out = new PrintStream(os, true, "UTF-8");
        }
        void send(String line) {
            out.print(line + "\r\n");
            out.flush();
        }
        void sendRaw(String payload) {
            out.print(payload);
            out.flush();
        }
        void expect(int code) throws Exception {
            String line = in.readLine();
            if (line == null) throw new java.io.EOFException("SMTP server closed connection");
            int got = Integer.parseInt(line.substring(0, 3));
            if (got != code) throw new RuntimeException("SMTP expected " + code + " got " + line);
        }
        /** Some servers reply with multiple 250-... lines (EHLO). */
        void expectMulti(int code) throws Exception {
            while (true) {
                String line = in.readLine();
                if (line == null) throw new java.io.EOFException("SMTP server closed connection");
                int got = Integer.parseInt(line.substring(0, 3));
                if (got != code) throw new RuntimeException("SMTP expected " + code + " got " + line);
                if (line.length() < 4 || line.charAt(3) == ' ') return;
            }
        }
    }

    public static final class DispatchResult {
        public int sent;
        public int failed;
        public int skipped;
        public boolean attempted() { return sent > 0 || failed > 0; }
    }
}
