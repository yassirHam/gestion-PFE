package controller;

import entities.AcademicSession;
import entities.Affectation;
import entities.AppSettings;
import entities.AppUser;
import entities.AuditAction;
import entities.AuditLog;
import entities.Department;
import entities.ExceptionType;
import entities.LifecycleState;
import entities.Notification;
import entities.PlanningVersion;
import entities.Professeur;
import entities.ProfesseurAvailability;
import entities.ProfesseurGrade;
import entities.Salle;
import entities.Soutenance;
import entities.SoutenanceException;
import entities.UserRole;
import services.AppSettingsService;
import services.ApprovalWorkflowService;
import services.AuditService;
import services.AuthService;
import services.ExceptionManagementService;
import services.GovernanceService;
import services.ManualOverrideService;
import services.NotificationService;
import services.SessionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.AppUserDAOImpl;
import dao.JuryDAOImpl;
import dao.PlanningVersionDAOImpl;
import dao.ProfesseurDAOImpl;
import dao.SalleDAOImpl;
import dao.SoutenanceDAOImpl;
import dao.AffectationDAOImpl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Servlet handling every "operational" route added on top of the original
 * {@code FrontController}. Kept separate so the historical controller stays
 * focused on planning/affectation/PV generation while this one owns the
 * new lifecycle, sessions/versions, manual overrides, exceptions,
 * notifications and governance flows.
 *
 * <p>Each handler is short by design: heavy lifting is delegated to the
 * services in the {@code services} package.</p>
 */
@WebServlet(urlPatterns = {
        // Authentication
        "/login.do", "/logout.do",
        // User management
        "/users.do", "/saveUser.do", "/deleteUser.do", "/changeUserPassword.do",
        // Sessions
        "/sessions.do", "/createSession.do", "/activateSession.do", "/closeSession.do",
        "/createVersion.do", "/setCurrentVersion.do", "/deleteVersion.do",
        // Approvals
        "/approvals.do", "/requestApproval.do", "/approve.do", "/reject.do",
        "/publishVersion.do", "/freezeVersion.do", "/archiveVersion.do",
        // Manual overrides
        "/lockSoutenance.do", "/unlockSoutenance.do",
        "/cancelSoutenance.do", "/postponeSoutenance.do",
        "/replanSoutenance.do", "/replaceSalle.do",
        "/swapJuryMember.do", "/lockJury.do",
        "/forceAffectation.do", "/lockAffectation.do", "/transitionAffectation.do",
        "/excludeProf.do", "/reinstateProf.do",
        // Exceptions
        "/exceptions.do", "/reportException.do", "/resolveException.do",
        // Notifications
        "/notifications.do", "/sendConvocations.do", "/sendReminders.do",
        "/dispatchNotifications.do",
        // Governance
        "/departments.do", "/saveDepartment.do", "/deleteDepartment.do",
        "/profGovernance.do", "/saveProfGovernance.do",
        "/profAvailability.do", "/saveProfAvailability.do", "/deleteProfAvailability.do",
        "/salleGovernance.do", "/saveSalleGovernance.do",
        // Audit
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
        } catch (Exception ignored) {}

        passFlashFromSession(req, "flashOk", "flashError", "flashInfo");

        String path = req.getServletPath();
        try {
            switch (path) {
                // Auth
                case "/login.do":              doLogin(req, resp); break;
                case "/logout.do":             doLogout(req, resp); break;
                // Users
                case "/users.do":              doUsers(req, resp); break;
                case "/saveUser.do":           doSaveUser(req, resp); break;
                case "/deleteUser.do":         doDeleteUser(req, resp); break;
                case "/changeUserPassword.do": doChangeUserPassword(req, resp); break;
                // Sessions / versions
                case "/sessions.do":           doSessions(req, resp); break;
                case "/createSession.do":      doCreateSession(req, resp); break;
                case "/activateSession.do":    doActivateSession(req, resp); break;
                case "/closeSession.do":       doCloseSession(req, resp); break;
                case "/createVersion.do":      doCreateVersion(req, resp); break;
                case "/setCurrentVersion.do":  doSetCurrentVersion(req, resp); break;
                case "/deleteVersion.do":      doDeleteVersion(req, resp); break;
                // Approvals
                case "/approvals.do":          doApprovals(req, resp); break;
                case "/requestApproval.do":    doRequestApproval(req, resp); break;
                case "/approve.do":            doApprove(req, resp); break;
                case "/reject.do":             doReject(req, resp); break;
                case "/publishVersion.do":     doPublishVersion(req, resp); break;
                case "/freezeVersion.do":      doFreezeVersion(req, resp); break;
                case "/archiveVersion.do":     doArchiveVersion(req, resp); break;
                // Manual overrides
                case "/lockSoutenance.do":     doSoutenanceOverride(req, resp, "LOCK"); break;
                case "/unlockSoutenance.do":   doSoutenanceOverride(req, resp, "UNLOCK"); break;
                case "/cancelSoutenance.do":   doSoutenanceOverride(req, resp, "CANCEL"); break;
                case "/postponeSoutenance.do": doSoutenanceOverride(req, resp, "POSTPONE"); break;
                case "/replanSoutenance.do":   doReplanSoutenance(req, resp); break;
                case "/replaceSalle.do":       doReplaceSalle(req, resp); break;
                case "/swapJuryMember.do":     doSwapJuryMember(req, resp); break;
                case "/lockJury.do":           doLockJury(req, resp); break;
                case "/forceAffectation.do":   doForceAffectation(req, resp); break;
                case "/lockAffectation.do":    doLockAffectation(req, resp); break;
                case "/transitionAffectation.do": doTransitionAffectation(req, resp); break;
                case "/excludeProf.do":        doExcludeProf(req, resp); break;
                case "/reinstateProf.do":      doReinstateProf(req, resp); break;
                // Exceptions
                case "/exceptions.do":         doExceptions(req, resp); break;
                case "/reportException.do":    doReportException(req, resp); break;
                case "/resolveException.do":   doResolveException(req, resp); break;
                // Notifications
                case "/notifications.do":      doNotifications(req, resp); break;
                case "/sendConvocations.do":   doSendConvocations(req, resp); break;
                case "/sendReminders.do":      doSendReminders(req, resp); break;
                case "/dispatchNotifications.do": doDispatchNotifications(req, resp); break;
                // Governance
                case "/departments.do":        doDepartments(req, resp); break;
                case "/saveDepartment.do":     doSaveDepartment(req, resp); break;
                case "/deleteDepartment.do":   doDeleteDepartment(req, resp); break;
                case "/profGovernance.do":     doProfGovernance(req, resp); break;
                case "/saveProfGovernance.do": doSaveProfGovernance(req, resp); break;
                case "/profAvailability.do":   doProfAvailability(req, resp); break;
                case "/saveProfAvailability.do": doSaveProfAvailability(req, resp); break;
                case "/deleteProfAvailability.do": doDeleteProfAvailability(req, resp); break;
                case "/salleGovernance.do":    doSalleGovernance(req, resp); break;
                case "/saveSalleGovernance.do":doSaveSalleGovernance(req, resp); break;
                // Audit
                case "/audit.do":              doAudit(req, resp); break;
                // SMTP
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

    // ─── Authentication ───────────────────────────────────────────────────

    private void doLogin(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            AppUser u = AuthService.getInstance().authenticate(username, password);
            if (u == null) {
                AuditService.getInstance().record(null, AuditAction.LOGIN_FAILED, "AppUser", null,
                        "Tentative de connexion echouee pour '" + username + "'");
                req.setAttribute("loginError", "Identifiants invalides ou compte désactivé.");
                req.setAttribute("attemptedUsername", username);
                forward(req, resp, "/login.jsp");
                return;
            }
            HttpSession session = req.getSession(true);
            session.setAttribute(AuthFilter.SESSION_USER, u);
            AuditService.getInstance().record(u, AuditAction.LOGIN, "AppUser", u.getId(),
                    "Connexion utilisateur");
            String back = (String) session.getAttribute("postLoginRedirect");
            session.removeAttribute("postLoginRedirect");
            if (back != null && !back.isBlank() && !back.contains("/login.do") && !back.contains("/logout.do")) {
                resp.sendRedirect(back);
            } else {
                resp.sendRedirect(req.getContextPath() + "/dashboard.do");
            }
            return;
        }
        // Already logged in?
        AppUser cur = AuthFilter.currentUser(req);
        if (cur != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard.do");
            return;
        }
        forward(req, resp, "/login.jsp");
    }

    private void doLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser cur = AuthFilter.currentUser(req);
        if (cur != null) {
            AuditService.getInstance().record(cur, AuditAction.LOGOUT, "AppUser", cur.getId(),
                    "Deconnexion");
        }
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        resp.sendRedirect(req.getContextPath() + "/login.do");
    }

    // ─── User management ──────────────────────────────────────────────────

    private void doUsers(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "users");
        req.setAttribute("users", AuthService.getInstance().findAll());
        req.setAttribute("roles", UserRole.values());
        req.setAttribute("departments", GovernanceService.getInstance().findAllDepartments());
        req.setAttribute("professeurs", new ProfesseurDAOImpl().findAll());
        forward(req, resp, "/users.jsp");
    }

    private void doSaveUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
        Long id = parseLong(req.getParameter("id"));
        AppUser u = id == null ? new AppUser() : AuthService.getInstance().findById(id);
        if (u == null) u = new AppUser();
        if (u.getId() == null) u.setUsername(trimOrNull(req.getParameter("username")));
        u.setFullName(trimOrNull(req.getParameter("fullName")));
        u.setEmail(trimOrNull(req.getParameter("email")));
        u.setRole(UserRole.fromString(req.getParameter("role")));
        u.setActive("on".equals(req.getParameter("active")) || "true".equals(req.getParameter("active")));
        u.setScopedFilieres(trimOrNull(req.getParameter("scopedFilieres")));
        Long deptId = parseLong(req.getParameter("departmentId"));
        u.setDepartment(deptId == null ? null : GovernanceService.getInstance().findDepartment(deptId));
        Long profId = parseLong(req.getParameter("professeurId"));
        u.setProfesseur(profId == null ? null : new ProfesseurDAOImpl().findById(profId));
        String pwd = req.getParameter("password");
        AppUser saved;
        if (id == null) {
            // For new users a password is required.
            if (pwd == null || pwd.isBlank()) {
                req.getSession().setAttribute("flashError", "Mot de passe requis pour un nouvel utilisateur.");
                resp.sendRedirect(req.getContextPath() + "/users.do");
                return;
            }
            saved = AuthService.getInstance().createUser(u, pwd);
            AuditService.getInstance().record(actor, AuditAction.USER_CREATED, "AppUser",
                    saved == null ? null : saved.getId(),
                    "Création utilisateur " + u.getUsername());
        } else {
            saved = AuthService.getInstance().updateUser(u);
            if (pwd != null && !pwd.isBlank()) {
                AuthService.getInstance().changePassword(saved.getId(), pwd);
                AuditService.getInstance().record(actor, AuditAction.USER_PASSWORD_CHANGED,
                        "AppUser", saved.getId(), "Changement de mot de passe");
            }
            AuditService.getInstance().record(actor, AuditAction.USER_UPDATED, "AppUser",
                    saved == null ? null : saved.getId(), "Mise à jour utilisateur");
        }
        req.getSession().setAttribute("flashOk", "Utilisateur enregistré.");
        resp.sendRedirect(req.getContextPath() + "/users.do");
    }

    private void doDeleteUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
        Long id = parseLong(req.getParameter("id"));
        if (id != null && (actor == null || !id.equals(actor.getId()))) {
            AuthService.getInstance().deactivate(id);
            AuditService.getInstance().record(actor, AuditAction.USER_DEACTIVATED, "AppUser", id,
                    "Désactivation d'un utilisateur");
            req.getSession().setAttribute("flashOk", "Utilisateur désactivé.");
        } else {
            req.getSession().setAttribute("flashError", "Vous ne pouvez pas vous désactiver vous-même.");
        }
        resp.sendRedirect(req.getContextPath() + "/users.do");
    }

    private void doChangeUserPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
        Long id = parseLong(req.getParameter("id"));
        String pwd = req.getParameter("password");
        if (id != null && pwd != null && !pwd.isBlank()) {
            AuthService.getInstance().changePassword(id, pwd);
            AuditService.getInstance().record(actor, AuditAction.USER_PASSWORD_CHANGED,
                    "AppUser", id, "Changement de mot de passe");
            req.getSession().setAttribute("flashOk", "Mot de passe modifié.");
        }
        resp.sendRedirect(req.getContextPath() + "/users.do");
    }

    // ─── Sessions / Versions ──────────────────────────────────────────────

    private void doSessions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "sessions");
        req.setAttribute("sessions", SessionService.getInstance().findAll());
        AcademicSession active = SessionService.getInstance().getActive();
        req.setAttribute("activeSession", active);
        if (active != null) {
            req.setAttribute("versions", SessionService.getInstance().findVersions(active.getId()));
        } else {
            req.setAttribute("versions", new ArrayList<>());
        }
        req.setAttribute("departments", GovernanceService.getInstance().findAllDepartments());
        forward(req, resp, "/sessions.jsp");
    }

    private void doCreateSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
        AcademicSession s = new AcademicSession(
                trimOrNull(req.getParameter("code")),
                trimOrNull(req.getParameter("label")),
                trimOrNull(req.getParameter("academicYear")));
        s.setStartDate(parseDate(req.getParameter("startDate")));
        s.setEndDate(parseDate(req.getParameter("endDate")));
        s.setDeadlineDate(parseDate(req.getParameter("deadlineDate")));
        Long deptId = parseLong(req.getParameter("departmentId"));
        if (deptId != null) s.setDepartment(GovernanceService.getInstance().findDepartment(deptId));
        boolean activate = "on".equals(req.getParameter("activate")) || "true".equals(req.getParameter("activate"));
        s.setActive(activate);
        AcademicSession saved = SessionService.getInstance().create(s, actor);
        if (activate && saved != null) {
            SessionService.getInstance().activate(saved.getId(), actor);
        }
        // Always start with a draft version.
        SessionService.getInstance().createVersion(saved, "Version initiale", actor);
        req.getSession().setAttribute("flashOk", "Session créée.");
        resp.sendRedirect(req.getContextPath() + "/sessions.do");
    }

    private void doActivateSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("id"));
        if (id != null) {
            SessionService.getInstance().activate(id, AuthFilter.currentUser(req));
            req.getSession().setAttribute("flashOk", "Session activée.");
        }
        resp.sendRedirect(req.getContextPath() + "/sessions.do");
    }

    private void doCloseSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("id"));
        if (id != null) {
            SessionService.getInstance().close(id, AuthFilter.currentUser(req));
            req.getSession().setAttribute("flashOk", "Session clôturée.");
        }
        resp.sendRedirect(req.getContextPath() + "/sessions.do");
    }

    private void doCreateVersion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long sessionId = parseLong(req.getParameter("sessionId"));
        String label = trimOrNull(req.getParameter("label"));
        AcademicSession s = sessionId == null
                ? SessionService.getInstance().getActive()
                : SessionService.getInstance().findById(sessionId);
        if (s != null) {
            SessionService.getInstance().createVersion(s, label, AuthFilter.currentUser(req));
            req.getSession().setAttribute("flashOk", "Nouvelle version créée.");
        }
        resp.sendRedirect(req.getContextPath() + "/sessions.do");
    }

    private void doSetCurrentVersion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long sessionId = parseLong(req.getParameter("sessionId"));
        Long versionId = parseLong(req.getParameter("versionId"));
        if (sessionId != null && versionId != null) {
            SessionService.getInstance().setCurrent(sessionId, versionId, AuthFilter.currentUser(req));
            req.getSession().setAttribute("flashOk", "Version courante mise à jour.");
        }
        resp.sendRedirect(req.getContextPath() + "/sessions.do");
    }

    private void doDeleteVersion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long versionId = parseLong(req.getParameter("id"));
        if (versionId != null) {
            SessionService.getInstance().deleteVersion(versionId, AuthFilter.currentUser(req));
            req.getSession().setAttribute("flashOk", "Version supprimée (si possible).");
        }
        resp.sendRedirect(req.getContextPath() + "/sessions.do");
    }

    // ─── Approvals ────────────────────────────────────────────────────────

    private void doApprovals(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "approvals");
        AcademicSession active = SessionService.getInstance().getActive();
        req.setAttribute("activeSession", active);
        if (active != null) {
            List<PlanningVersion> versions = SessionService.getInstance().findVersions(active.getId());
            req.setAttribute("versions", versions);
            // Approval history per version
            java.util.Map<Long, List<entities.Approval>> historyByVersion = new java.util.LinkedHashMap<>();
            for (PlanningVersion v : versions) {
                historyByVersion.put(v.getId(),
                        ApprovalWorkflowService.getInstance().getHistory(v.getId()));
            }
            req.setAttribute("historyByVersion", historyByVersion);
        } else {
            req.setAttribute("versions", new ArrayList<>());
        }
        forward(req, resp, "/approvals.jsp");
    }

    private void doRequestApproval(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("versionId"));
        ApprovalWorkflowService.Result r = ApprovalWorkflowService.getInstance()
                .requestApproval(id, AuthFilter.currentUser(req), req.getParameter("comment"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/approvals.do");
    }

    private void doApprove(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("versionId"));
        ApprovalWorkflowService.Result r = ApprovalWorkflowService.getInstance()
                .approve(id, AuthFilter.currentUser(req), req.getParameter("comment"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/approvals.do");
    }

    private void doReject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("versionId"));
        ApprovalWorkflowService.Result r = ApprovalWorkflowService.getInstance()
                .reject(id, AuthFilter.currentUser(req), req.getParameter("comment"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/approvals.do");
    }

    private void doPublishVersion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("versionId"));
        ApprovalWorkflowService.Result r = ApprovalWorkflowService.getInstance()
                .publish(id, AuthFilter.currentUser(req), req.getParameter("comment"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/approvals.do");
    }

    private void doFreezeVersion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("versionId"));
        ApprovalWorkflowService.Result r = ApprovalWorkflowService.getInstance()
                .freeze(id, AuthFilter.currentUser(req), req.getParameter("comment"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/approvals.do");
    }

    private void doArchiveVersion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("versionId"));
        ApprovalWorkflowService.Result r = ApprovalWorkflowService.getInstance()
                .archive(id, AuthFilter.currentUser(req), req.getParameter("comment"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/approvals.do");
    }

    // ─── Manual overrides ─────────────────────────────────────────────────

    private void doSoutenanceOverride(HttpServletRequest req, HttpServletResponse resp, String op) throws IOException {
        Long id = parseLong(req.getParameter("id"));
        AppUser actor = AuthFilter.currentUser(req);
        ManualOverrideService.Result r;
        switch (op) {
            case "LOCK":     r = ManualOverrideService.getInstance().lockSoutenance(id, actor); break;
            case "UNLOCK":   r = ManualOverrideService.getInstance().unlockSoutenance(id, actor); break;
            case "CANCEL":   r = ManualOverrideService.getInstance().cancelSoutenance(id, actor, req.getParameter("reason")); break;
            case "POSTPONE": r = ManualOverrideService.getInstance().postponeSoutenance(id, actor, req.getParameter("reason")); break;
            default:         r = ManualOverrideService.Result.failure("Opération inconnue.");
        }
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/planning.do");
    }

    private void doReplanSoutenance(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("id"));
        Date newDate = parseDate(req.getParameter("date"));
        String newHeure = trimOrNull(req.getParameter("heure"));
        Long newSalleId = parseLong(req.getParameter("salleId"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .replanSoutenance(id, AuthFilter.currentUser(req), newDate, newHeure, newSalleId);
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/planning.do");
    }

    private void doReplaceSalle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("id"));
        Long salleId = parseLong(req.getParameter("salleId"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .replaceSalle(id, salleId, AuthFilter.currentUser(req));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/planning.do");
    }

    private void doSwapJuryMember(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long juryId = parseLong(req.getParameter("juryId"));
        String role = trimOrNull(req.getParameter("role"));   // "P", "R1", "R2", "INV"
        Long replacementProfId = parseLong(req.getParameter("replacementProfId"));
        String reason = trimOrNull(req.getParameter("reason"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .swapJuryMember(juryId, role, replacementProfId, AuthFilter.currentUser(req), reason);
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/planning.do");
    }

    private void doLockJury(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long juryId = parseLong(req.getParameter("juryId"));
        boolean lock = !"false".equalsIgnoreCase(req.getParameter("lock"));
        ManualOverrideService.Result r = lock
                ? ManualOverrideService.getInstance().lockJury(juryId, AuthFilter.currentUser(req))
                : ManualOverrideService.getInstance().unlockJury(juryId, AuthFilter.currentUser(req));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/planning.do");
    }

    private void doForceAffectation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long affId = parseLong(req.getParameter("id"));
        Long encadrantId = parseLong(req.getParameter("encadrantId"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .forceAffectation(affId, encadrantId, AuthFilter.currentUser(req),
                        req.getParameter("reason"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/affectation.do");
    }

    private void doLockAffectation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long affId = parseLong(req.getParameter("id"));
        boolean lock = !"false".equalsIgnoreCase(req.getParameter("lock"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .lockAffectation(affId, lock, AuthFilter.currentUser(req));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/affectation.do");
    }

    private void doTransitionAffectation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long affId = parseLong(req.getParameter("id"));
        LifecycleState target;
        try { target = LifecycleState.valueOf(req.getParameter("state").trim().toUpperCase()); }
        catch (Exception e) { target = null; }
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .transitionAffectation(affId, target, AuthFilter.currentUser(req));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/affectation.do");
    }

    private void doExcludeProf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long profId = parseLong(req.getParameter("id"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .excludeProfessor(profId, AuthFilter.currentUser(req), req.getParameter("reason"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/profGovernance.do");
    }

    private void doReinstateProf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long profId = parseLong(req.getParameter("id"));
        ManualOverrideService.Result r = ManualOverrideService.getInstance()
                .reinstateProfessor(profId, AuthFilter.currentUser(req));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/profGovernance.do");
    }

    // ─── Exceptions ───────────────────────────────────────────────────────

    private void doExceptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "exceptions");
        req.setAttribute("openExceptions", ExceptionManagementService.getInstance().findOpen());
        req.setAttribute("recentExceptions", ExceptionManagementService.getInstance().recent(50));
        req.setAttribute("exceptionTypes", ExceptionType.values());
        req.setAttribute("resolutionActions", ExceptionManagementService.ResolutionAction.values());
        req.setAttribute("soutenances", new SoutenanceDAOImpl().findAllWithDetails());
        forward(req, resp, "/exceptions.jsp");
    }

    private void doReportException(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long soutenanceId = parseLong(req.getParameter("soutenanceId"));
        ExceptionType type = ExceptionType.OTHER;
        try { type = ExceptionType.valueOf(req.getParameter("type").trim().toUpperCase()); }
        catch (Exception ignored) {}
        SoutenanceException created = ExceptionManagementService.getInstance()
                .report(soutenanceId, type, req.getParameter("description"), AuthFilter.currentUser(req));
        if (created != null) {
            req.getSession().setAttribute("flashOk", "Incident enregistré.");
        } else {
            req.getSession().setAttribute("flashError", "Soutenance introuvable.");
        }
        resp.sendRedirect(req.getContextPath() + "/exceptions.do");
    }

    private void doResolveException(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("id"));
        ExceptionManagementService.ResolutionAction action =
                ExceptionManagementService.ResolutionAction.fromString(req.getParameter("action"));
        ManualOverrideService.Result r = ExceptionManagementService.getInstance()
                .resolve(id, AuthFilter.currentUser(req), action, req.getParameter("notes"));
        flash(req, r);
        resp.sendRedirect(req.getContextPath() + "/exceptions.do");
    }

    // ─── Notifications ────────────────────────────────────────────────────

    private void doNotifications(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "notifications");
        req.setAttribute("notifications", NotificationService.getInstance().recent(200));
        req.setAttribute("queuedCount", NotificationService.getInstance().countQueued());
        forward(req, resp, "/notifications.jsp");
    }

    private void doSendConvocations(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
        String idParam = req.getParameter("soutenanceId");
        int n;
        if (idParam != null && !idParam.isBlank()) {
            Long id = parseLong(idParam);
            Soutenance s = new SoutenanceDAOImpl().findByIdWithDetails(id);
            n = s == null ? 0 : NotificationService.getInstance().queueConvocations(s, actor);
        } else {
            List<Soutenance> all = new SoutenanceDAOImpl().findAllWithDetails();
            n = NotificationService.getInstance().queueConvocationsForAll(all, actor);
        }
        req.getSession().setAttribute("flashOk", n + " convocation(s) en file.");
        resp.sendRedirect(req.getContextPath() + "/notifications.do");
    }

    private void doSendReminders(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
        List<Soutenance> all = new SoutenanceDAOImpl().findAllWithDetails();
        int n = NotificationService.getInstance().queueReminders(all, actor);
        req.getSession().setAttribute("flashOk", n + " rappel(s) en file.");
        resp.sendRedirect(req.getContextPath() + "/notifications.do");
    }

    private void doDispatchNotifications(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        NotificationService.DispatchResult r = NotificationService.getInstance()
                .dispatchQueued(AuthFilter.currentUser(req));
        if (r.attempted()) {
            req.getSession().setAttribute("flashOk", r.sent + " envoyée(s), " + r.failed + " échec(s).");
        } else if (r.skipped > 0) {
            req.getSession().setAttribute("flashError",
                    "SMTP non configuré : " + r.skipped + " notification(s) restent en file.");
        } else {
            req.getSession().setAttribute("flashInfo", "Aucune notification en file.");
        }
        resp.sendRedirect(req.getContextPath() + "/notifications.do");
    }

    // ─── Governance ───────────────────────────────────────────────────────

    private void doDepartments(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "governance");
        req.setAttribute("departments", GovernanceService.getInstance().findAllDepartments());
        forward(req, resp, "/departments.jsp");
    }

    private void doSaveDepartment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
        Long id = parseLong(req.getParameter("id"));
        Department d = id == null ? new Department() : GovernanceService.getInstance().findDepartment(id);
        if (d == null) d = new Department();
        d.setCode(trimOrNull(req.getParameter("code")));
        d.setName(trimOrNull(req.getParameter("name")));
        d.setCampus(trimOrNull(req.getParameter("campus")));
        d.setMaxSoutenancesPerDay(parseInteger(req.getParameter("maxSoutenancesPerDay")));
        d.setMaxExternalMembers(parseInteger(req.getParameter("maxExternalMembers")));
        d.setFiliereCodes(trimOrNull(req.getParameter("filiereCodes")));
        d.setHeadUserId(parseLong(req.getParameter("headUserId")));
        d.setActive(!"false".equals(req.getParameter("active")));
        GovernanceService.getInstance().saveDepartment(d, actor);
        req.getSession().setAttribute("flashOk", "Département enregistré.");
        resp.sendRedirect(req.getContextPath() + "/departments.do");
    }

    private void doDeleteDepartment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long id = parseLong(req.getParameter("id"));
        if (id != null) {
            GovernanceService.getInstance().deleteDepartment(id, AuthFilter.currentUser(req));
            req.getSession().setAttribute("flashOk", "Département supprimé.");
        }
        resp.sendRedirect(req.getContextPath() + "/departments.do");
    }

    private void doProfGovernance(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "governance");
        req.setAttribute("profs", new ProfesseurDAOImpl().findAll());
        req.setAttribute("departments", GovernanceService.getInstance().findAllDepartments());
        req.setAttribute("grades", ProfesseurGrade.values());
        forward(req, resp, "/profGovernance.jsp");
    }

    private void doSaveProfGovernance(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
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
        // Department change is handled separately
        Long deptId = parseLong(req.getParameter("departmentId"));
        GovernanceService.getInstance().assignProfesseurToDepartment(profId, deptId, actor);
        req.getSession().setAttribute("flashOk", "Données opérationnelles mises à jour.");
        resp.sendRedirect(req.getContextPath() + "/profGovernance.do");
    }

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
        AppUser actor = AuthFilter.currentUser(req);
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
            GovernanceService.getInstance().deleteAvailability(id, AuthFilter.currentUser(req));
            req.getSession().setAttribute("flashOk", "Disponibilité supprimée.");
        }
        resp.sendRedirect(req.getContextPath() + "/profAvailability.do");
    }

    private void doSalleGovernance(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("activeTab", "governance");
        req.setAttribute("salles", new SalleDAOImpl().findAll());
        req.setAttribute("departments", GovernanceService.getInstance().findAllDepartments());
        forward(req, resp, "/salleGovernance.jsp");
    }

    private void doSaveSalleGovernance(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
        Long salleId = parseLong(req.getParameter("id"));
        if (salleId == null) {
            req.getSession().setAttribute("flashError", "Salle introuvable.");
            resp.sendRedirect(req.getContextPath() + "/salleGovernance.do");
            return;
        }
        Salle s = new SalleDAOImpl().findById(salleId);
        if (s == null) {
            resp.sendRedirect(req.getContextPath() + "/salleGovernance.do");
            return;
        }
        s.setBlock(trimOrNull(req.getParameter("block")));
        s.setCampus(trimOrNull(req.getParameter("campus")));
        Integer cap = parseInteger(req.getParameter("capacity"));
        s.setCapacity(cap);
        s.setEquipment(trimOrNull(req.getParameter("equipment")));
        Integer prio = parseInteger(req.getParameter("priority"));
        s.setPriority(prio == null ? 0 : prio);
        s.setAvailable(!"false".equals(req.getParameter("available")));
        new SalleDAOImpl().save(s);
        Long deptId = parseLong(req.getParameter("departmentId"));
        GovernanceService.getInstance().assignSalleToDepartment(salleId, deptId, actor);
        req.getSession().setAttribute("flashOk", "Salle mise à jour.");
        resp.sendRedirect(req.getContextPath() + "/salleGovernance.do");
    }

    // ─── Audit ────────────────────────────────────────────────────────────

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
        req.setAttribute("users", AuthService.getInstance().findAll());
        req.setAttribute("filterActorId", actorId);
        req.setAttribute("limit", limit);
        forward(req, resp, "/audit.jsp");
    }

    // ─── SMTP ────────────────────────────────────────────────────────────

    private void doSaveSmtp(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppUser actor = AuthFilter.currentUser(req);
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

    private void flash(HttpServletRequest req, ApprovalWorkflowService.Result r) {
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
