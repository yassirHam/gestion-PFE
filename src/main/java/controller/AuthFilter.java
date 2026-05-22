package controller;

import entities.AppUser;
import entities.UserRole;
import services.AuthService;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enforces authentication and per-route role-based access control.
 *
 * <p>The filter intercepts every {@code *.do} URL. Public routes (login,
 * logout, static resources, and the AJAX recommendations endpoint when
 * accessed by a logged-in user) are allowed to pass through; everything
 * else requires a session attribute named {@code currentUser}.</p>
 *
 * <p>On a successful authentication, the {@code AppUser} is stored in the
 * HTTP session under the {@code currentUser} key. Other servlets/filters
 * can call {@link #currentUser(HttpServletRequest)} to retrieve it.</p>
 */
@WebFilter(filterName = "authFilter", urlPatterns = {"*.do"})
public class AuthFilter implements Filter {

    public static final String SESSION_USER = "currentUser";

    /** Routes accessible without authentication. */
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/login.do",
            "/logout.do",
            "/logo.do"  // login page may render the logo
    ));

    /** Per-route minimum role(s). Routes not listed here only require login. */
    private static final Map<String, UserRole[]> ROUTE_ROLES = new HashMap<>();
    static {
        // Tenant management — admin pédagogique only
        ROUTE_ROLES.put("/users.do",                new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/saveUser.do",             new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/deleteUser.do",           new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/changeUserPassword.do",   new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/saveBranding.do",         new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/saveStorage.do",          new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/testStorage.do",          new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/saveNlp.do",              new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/saveSmtp.do",             new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});

        // Sessions / versions / approvals — admin péda or chef département
        ROUTE_ROLES.put("/sessions.do",             new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/createSession.do",        new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/activateSession.do",      new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/closeSession.do",         new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/createVersion.do",        new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT, UserRole.COORDINATEUR_FILIERE});
        ROUTE_ROLES.put("/setCurrentVersion.do",    new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/deleteVersion.do",        new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/approve.do",              new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/reject.do",               new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/publishVersion.do",       new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/freezeVersion.do",        new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/archiveVersion.do",       new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});

        // Manual overrides — anyone with edit rights
        ROUTE_ROLES.put("/lockSoutenance.do",       editRoles());
        ROUTE_ROLES.put("/unlockSoutenance.do",     editRoles());
        ROUTE_ROLES.put("/cancelSoutenance.do",     editRoles());
        ROUTE_ROLES.put("/postponeSoutenance.do",   editRoles());
        ROUTE_ROLES.put("/replanSoutenance.do",     editRoles());
        ROUTE_ROLES.put("/replaceSalle.do",         editRoles());
        ROUTE_ROLES.put("/swapJuryMember.do",       editRoles());
        ROUTE_ROLES.put("/lockJury.do",             editRoles());
        ROUTE_ROLES.put("/forceAffectation.do",     editRoles());
        ROUTE_ROLES.put("/lockAffectation.do",      editRoles());
        ROUTE_ROLES.put("/transitionAffectation.do",editRoles());
        ROUTE_ROLES.put("/excludeProf.do",          editRoles());
        ROUTE_ROLES.put("/reinstateProf.do",        editRoles());

        // Exceptions — coordinateur, chef, admin
        ROUTE_ROLES.put("/exceptions.do",           anyAuth());
        ROUTE_ROLES.put("/reportException.do",      anyAuth());
        ROUTE_ROLES.put("/resolveException.do",     editRoles());

        // Notifications — coordinateur, chef, admin
        ROUTE_ROLES.put("/notifications.do",        anyAuth());
        ROUTE_ROLES.put("/sendConvocations.do",     editRoles());
        ROUTE_ROLES.put("/sendReminders.do",        editRoles());
        ROUTE_ROLES.put("/dispatchNotifications.do",editRoles());

        // Governance — chef département + admin
        ROUTE_ROLES.put("/departments.do",          new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/saveDepartment.do",       new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/deleteDepartment.do",     new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/profGovernance.do",       new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/saveProfGovernance.do",   new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/profAvailability.do",     anyAuth());
        ROUTE_ROLES.put("/saveProfAvailability.do", anyAuth());
        ROUTE_ROLES.put("/deleteProfAvailability.do", anyAuth());
        ROUTE_ROLES.put("/salleGovernance.do",      new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});
        ROUTE_ROLES.put("/saveSalleGovernance.do",  new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});

        // Audit — admin & chef département can read
        ROUTE_ROLES.put("/audit.do",                new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT});

        // Critical edit ops on existing routes — restrict to edit roles
        ROUTE_ROLES.put("/lancerAffectation.do",    editRoles());
        ROUTE_ROLES.put("/uploadData.do",           editRoles());
        ROUTE_ROLES.put("/lancerPlanning.do",       editRoles());
        ROUTE_ROLES.put("/supprimerListes.do",      editRoles());
        ROUTE_ROLES.put("/restoreAffectation.do",   editRoles());
        ROUTE_ROLES.put("/clearHistory.do",         new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
        ROUTE_ROLES.put("/addSalle.do",             editRoles());
        ROUTE_ROLES.put("/addSalleBulk.do",         editRoles());
        ROUTE_ROLES.put("/deleteSalle.do",          editRoles());
        ROUTE_ROLES.put("/deleteAllSalles.do",      new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE});
    }

    private static UserRole[] editRoles() {
        return new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT, UserRole.COORDINATEUR_FILIERE};
    }
    private static UserRole[] anyAuth() {
        return new UserRole[]{UserRole.ADMIN_PEDAGOGIQUE, UserRole.CHEF_DEPARTEMENT,
                UserRole.COORDINATEUR_FILIERE, UserRole.PROFESSEUR, UserRole.CONSULTATION};
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // Ensure a bootstrap admin exists so the very first deployment can log in.
        try {
            AuthService.getInstance().ensureBootstrapAdmin();
        } catch (Exception ignored) {
            // SessionFactory may not yet be ready depending on classloader order;
            // the call is repeated lazily on the first authenticate() too.
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getServletPath();

        // Always allow public routes through.
        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        AppUser user = currentUser(req);
        if (user == null) {
            // Save the originally requested URL so we can redirect back after login.
            String originalUrl = req.getRequestURI();
            String query = req.getQueryString();
            if (query != null && !query.isEmpty()) originalUrl = originalUrl + "?" + query;
            req.getSession(true).setAttribute("postLoginRedirect", originalUrl);
            resp.sendRedirect(req.getContextPath() + "/login.do");
            return;
        }

        // Per-route role check
        UserRole[] required = ROUTE_ROLES.get(path);
        if (required != null) {
            UserRole role = user.getRole();
            boolean ok = false;
            for (UserRole r : required) if (r == role) { ok = true; break; }
            if (!ok) {
                req.getSession().setAttribute("flashError",
                        "Accès refusé : votre rôle (" + role.getLabel() + ") ne permet pas cette action.");
                resp.sendRedirect(req.getContextPath() + "/dashboard.do");
                return;
            }
        }

        // Make the user available as a request attribute so JSPs can render menu state.
        req.setAttribute("currentUser", user);
        chain.doFilter(request, response);
    }

    public static AppUser currentUser(HttpServletRequest req) {
        if (req == null) return null;
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        Object o = s.getAttribute(SESSION_USER);
        return o instanceof AppUser ? (AppUser) o : null;
    }

    @Override
    public void destroy() { /* no-op */ }
}
