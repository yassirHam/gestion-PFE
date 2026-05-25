package controller;

import entities.AppUser;
import entities.UserRole;

import javax.servlet.http.HttpServletRequest;

/**
 * Provides a stable local actor for audit and ownership metadata.
 *
 * <p>The application has no authentication layer: requests are not logged in,
 * redirected, or access-checked by user category. Business operations still pass an {@link AppUser}
 * to audit-aware services so existing audit records keep a useful actor name.</p>
 */
public final class AuditContext {

    public static final String REQUEST_ACTOR = "auditActor";

    private static final AppUser LOCAL_ACTOR = new AppUser();

    static {
        LOCAL_ACTOR.setId(0L);
        LOCAL_ACTOR.setUsername("local-operator");
        LOCAL_ACTOR.setFullName("Operateur local");
        LOCAL_ACTOR.setRole(UserRole.ADMIN_PEDAGOGIQUE);
        LOCAL_ACTOR.setActive(true);
    }

    private AuditContext() {
    }

    public static AppUser currentActor(HttpServletRequest req) {
        if (req == null) return LOCAL_ACTOR;
        Object attr = req.getAttribute(REQUEST_ACTOR);
        if (attr instanceof AppUser) return (AppUser) attr;
        req.setAttribute(REQUEST_ACTOR, LOCAL_ACTOR);
        return LOCAL_ACTOR;
    }
}
