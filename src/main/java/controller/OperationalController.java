package controller;

import entities.AcademicSession;
import entities.AppSettings;
import entities.AppUser;
import entities.AuditAction;
import entities.AuditLog;
import entities.Professeur;
import entities.ProfesseurAvailability;
import entities.ProfesseurGrade;
import services.AppSettingsService;
import services.AuditService;
import services.GovernanceService;
import services.ManualOverrideService;
import services.SessionService;
import services.UserDirectoryService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.ProfesseurDAOImpl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Servlet handling operational routes that complement the historical
 * {@code FrontController}. Kept separate so the historical controller stays
 * focused on planning/affectation/PV generation while this one owns the
 * professor governance, availability, audit and SMTP settings flows.
 *
 * <p>Each handler is short by design: heavy lifting is delegated to the
 * services in the {@code services} package.</p>
 */
@WebServlet(urlPatterns = {
        // Professor governance & exclusion
        "/profGovernance.do", "/saveProfGovernance.do",
        "/excludeProf.do", "/reinstateProf.do",
        // Professor availability declarations
        "/profAvailability.do", "/saveProfAvailability.do", "/deleteProfAvailability.do",
        // Audit log
        "/audit.do",
        // SMTP settings
        "/saveSmtp.do"
})
public class OperationalController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    private void dispatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Make branding available to every JSP
        try {
            req.setAttribute("appSettings", AppSettingsService.getInstance().get());
            req.setAttribute("auditActor", AuditContext.currentActor(req));
        } catch (Throwable ignored) {}

        passFlashFromSession(req, "flashOk", "flashError", "flashInfo");

        String path = req.getServletPath();
        try {
            switch (path) {
                case "/profGovernance.do":     doProfGovernance(req, resp); break;
                case "/saveProfGovernance.do": doSaveProfGovernance(req, resp); break;
                case "/excludeProf.do":        doExcludeProf(req, resp); break;
                case "/reinstateProf.do":      doReinstateProf(req, resp); break;
                case "/profAvailability.do":   doProfAvailability(req, resp); break;
                case "/saveProfAvailability.do": doSaveProfAvailability(req, resp); break;
                case "/deleteProfAvailability.do": doDeleteProfAvailability(req, resp); break;
                case "/audit.do":              doAudit(req, resp); break;
                case "/saveSmtp.do":           doSaveSmtp(req, resp); break;
                default:
                    resp.sendRedirect(req.getContextPath() + "/dashboard.do");
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.getSession().setAttribute("flashError",
                    "Erreur interne : " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            resp.sendRedirect(req.getContextPath() + "/dashboard.do");
        }
    }

    // ─── Professor governance ─────────────────────────────────────────────

    private void doProfGovernance(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "governance");
        req.setAttribute("profs", new ProfesseurDAOImpl().findAll());
        req.setAttribute("grades", ProfesseurGrade.values());
        forward(req, resp, "/profGovernance.jsp");
    }

    private void doSaveProfGovernance(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuditContext.currentActor(req);
        Long profId = parseLong(req.getParameter("id"));
        if (profId == null) {
            req.getSession().setAttribute("flashError", "Identifiant manquant.");
            resp.sendRedirect(req.getContextPath() + "/profGovernance.do");
            return;
        }
        Professeur p = new ProfesseurDAOImpl().findById(profId);
        if (p == null) {
            req.getSession().setAttribute("flashError", "Professeur introuvable.");
            resp.sendRedirect(req.getContextPath() + "/profGovernance.do");
            return;
        }
        // Update operational fields only — never identity (nom/prenom)
        Professeur input = new Professeur();
        input.setIdp(profId);
        try { input.setGrade(ProfesseurGrade.valueOf(req.getParameter("grade").trim().toUpperCase())); }
        catch (Exception ignored) { input.setGrade(p.getGrade()); }
        input.setInternal("on".equals(req.getParameter("internal")) || "true".equals(req.getParameter("internal")));
        input.setVip("on".equals(req.getParameter("vip")) || "true".equals(req.getParameter("vip")));
        input.setEmail(trimOrNull(req.getParameter("email")));
        input.setPhone(trimOrNull(req.getParameter("phone")));
        input.setLanguages(trimOrNull(req.getParameter("languages")));
        input.setMaxSoutenancesPerDay(parseInteger(req.getParameter("maxSoutenancesPerDay")));
        GovernanceService.getInstance().updateProfesseurOperational(input, actor);
        req.getSession().setAttribute("flashOk", "Données opérationnelles mises à jour.");
        resp.sendRedirect(req.getContextPath() + "/profGovernance.do");
    }

    private void doExcludeProf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long profId = parseLong(req.getParameter("id"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .excludeProfessor(profId, AuditContext.currentActor(req), req.getParameter("reason"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/profGovernance.do");
    }

    private void doReinstateProf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long profId = parseLong(req.getParameter("id"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .reinstateProfessor(profId, AuditContext.currentActor(req));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/profGovernance.do");
    }

    // ─── Professor availability ───────────────────────────────────────────

    private void doProfAvailability(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "governance");
        AcademicSession active = SessionService.getInstance().getActive();
        req.setAttribute("activeSession", active);
        req.setAttribute("availabilities", GovernanceService.getInstance().findAvailability(
                active == null ? null : active.getId()));
        req.setAttribute("profs", new ProfesseurDAOImpl().findAll());
        req.setAttribute("periods", ProfesseurAvailability.Period.values());
        req.setAttribute("kinds", ProfesseurAvailability.Kind.values());
        forward(req, resp, "/profAvailability.jsp");
    }

    private void doSaveProfAvailability(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuditContext.currentActor(req);
        ProfesseurAvailability av = new ProfesseurAvailability();
        Long profId = parseLong(req.getParameter("professeurId"));
        if (profId != null) av.setProfesseur(new ProfesseurDAOImpl().findById(profId));
        AcademicSession active = SessionService.getInstance().getActive();
        Long sessionId = parseLong(req.getParameter("sessionId"));
        if (sessionId != null) av.setSession(SessionService.getInstance().findById(sessionId));
        else av.setSession(active);
        av.setTheDate(parseDate(req.getParameter("theDate")));
        try { av.setPeriod(ProfesseurAvailability.Period.valueOf(req.getParameter("period").trim().toUpperCase())); }
        catch (Exception ignored) {}
        try { av.setKind(ProfesseurAvailability.Kind.valueOf(req.getParameter("kind").trim().toUpperCase())); }
        catch (Exception ignored) {}
        av.setReason(trimOrNull(req.getParameter("reason")));
        GovernanceService.getInstance().declareAvailability(av, actor);
        req.getSession().setAttribute("flashOk", "Disponibilité enregistrée.");
        resp.sendRedirect(req.getContextPath() + "/profAvailability.do");
    }

    private void doDeleteProfAvailability(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("id"));
        if (id != null) {
            GovernanceService.getInstance().deleteAvailability(id, AuditContext.currentActor(req));
            req.getSession().setAttribute("flashOk", "Disponibilité supprimée.");
        }
        resp.sendRedirect(req.getContextPath() + "/profAvailability.do");
    }

    // ─── Audit log ────────────────────────────────────────────────────────

    private void doAudit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "audit");
        int limit = 200;
        try { limit = Math.max(10, Math.min(1000, Integer.parseInt(req.getParameter("limit")))); }
        catch (Exception ignored) {}
        List<AuditLog> logs;
        Long actorId = parseLong(req.getParameter("actorId"));
        if (actorId != null) {
            logs = AuditService.getInstance().forActor(actorId, limit);
        } else {
            logs = AuditService.getInstance().recent(limit);
        }
        req.setAttribute("logs", logs);
        req.setAttribute("users", UserDirectoryService.getInstance().findAll());
        req.setAttribute("filterActorId", actorId);
        req.setAttribute("limit", limit);
        forward(req, resp, "/audit.jsp");
    }

    // ─── SMTP settings ────────────────────────────────────────────────────

    private void doSaveSmtp(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuditContext.currentActor(req);
        AppSettings cur = AppSettingsService.getInstance().get();
        cur.setSmtpEnabled("on".equals(req.getParameter("smtpEnabled")) || "true".equals(req.getParameter("smtpEnabled")));
        cur.setSmtpHost(trimOrNull(req.getParameter("smtpHost")));
        cur.setSmtpPort(parseInteger(req.getParameter("smtpPort")));
        cur.setSmtpUsername(trimOrNull(req.getParameter("smtpUsername")));
        String pwd = req.getParameter("smtpPassword");
        if (pwd != null && !pwd.isBlank()) cur.setSmtpPassword(pwd);
        cur.setSmtpFrom(trimOrNull(req.getParameter("smtpFrom")));
        cur.setSmtpStartTls(!"false".equals(req.getParameter("smtpStartTls")));
        AppSettingsService.getInstance().update(cur);
        AuditService.getInstance().record(actor, AuditAction.SETTINGS_CHANGED, "AppSettings", null,
                "Paramètres SMTP mis à jour");
        req.getSession().setAttribute("flashOk", "Paramètres SMTP enregistrés.");
        resp.sendRedirect(req.getContextPath() + "/settings.do");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private void forward(HttpServletRequest req, HttpServletResponse resp, String jsp)
            throws ServletException, IOException {
        req.getRequestDispatcher(jsp).forward(req, resp);
    }

    private void flash(HttpServletRequest req, ManualOverrideService.Result r) {
        if (r == null) return;
        req.getSession().setAttribute(r.isSuccess() ? "flashOk" : "flashError", r.getMessage());
    }

    private void passFlashFromSession(HttpServletRequest req, String... names) {
        HttpSession s = req.getSession(false);
        if (s == null) return;
        for (String name : names) {
            Object v = s.getAttribute(name);
            if (v != null) {
                req.setAttribute(name, v);
                s.removeAttribute(name);
            }
        }
    }

    private static Long parseLong(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Long.parseLong(v.trim()); }
        catch (Exception e) { return null; }
    }

    private static Integer parseInteger(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v.trim()); }
        catch (Exception e) { return null; }
    }

    private static String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static Date parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return new SimpleDateFormat("yyyy-MM-dd").parse(iso.trim()); }
        catch (Exception e) { return null; }
    }
}
