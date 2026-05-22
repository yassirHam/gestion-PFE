<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Approbations" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-circle-check me-2 text-primary"></i> Cycle de validation et publication</h2>

<c:if test="${empty activeSession}">
    <div class="panel">
        <p class="text-muted mb-0">Aucune session active. Créez d'abord une session.</p>
    </div>
</c:if>

<c:if test="${not empty activeSession}">
    <div class="panel">
        <h2 class="mb-1">Session : <c:out value="${activeSession.displayName}"/></h2>
        <p class="text-tiny mb-0">
            DRAFT &rarr; PENDING_VALIDATION &rarr; VALIDATED &rarr; PUBLISHED &rarr; ARCHIVED
        </p>
    </div>

    <c:forEach var="v" items="${versions}">
        <div class="panel">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-2">
                <div>
                    <h2 class="mb-1">
                        <c:out value="${v.displayName}"/>
                        <c:if test="${v.current}"><span class="badge bg-primary ms-1">courante</span></c:if>
                        <span class="badge bg-${v.state.bootstrap} ms-1"><c:out value="${v.state.label}"/></span>
                        <c:if test="${not empty v.frozenAt}">
                            <span class="badge bg-info ms-1"><i class="fa-solid fa-snowflake"></i> figée</span>
                        </c:if>
                    </h2>
                    <div class="text-tiny">
                        Créée le <fmt:formatDate value="${v.createdAt}" pattern="yyyy-MM-dd HH:mm"/>
                        <c:if test="${not empty v.publishedAt}">
                            &middot; Publiée le <fmt:formatDate value="${v.publishedAt}" pattern="yyyy-MM-dd HH:mm"/>
                        </c:if>
                    </div>
                </div>
                <div class="btn-group">
                    <c:if test="${v.state == 'DRAFT' or v.state == 'REJECTED'}">
                        <button class="btn btn-sm btn-primary" data-bs-toggle="modal" data-bs-target="#act-${v.id}-request">
                            <i class="fa-solid fa-paper-plane me-1"></i> Soumettre
                        </button>
                    </c:if>
                    <c:if test="${v.state == 'PENDING_VALIDATION'}">
                        <c:if test="${currentUser.role == 'ADMIN_PEDAGOGIQUE' or currentUser.role == 'CHEF_DEPARTEMENT'}">
                            <button class="btn btn-sm btn-success" data-bs-toggle="modal" data-bs-target="#act-${v.id}-approve">
                                <i class="fa-solid fa-circle-check me-1"></i> Approuver
                            </button>
                            <button class="btn btn-sm btn-outline-danger" data-bs-toggle="modal" data-bs-target="#act-${v.id}-reject">
                                <i class="fa-solid fa-circle-xmark me-1"></i> Rejeter
                            </button>
                        </c:if>
                    </c:if>
                    <c:if test="${v.state == 'VALIDATED'}">
                        <c:if test="${currentUser.role == 'ADMIN_PEDAGOGIQUE' or currentUser.role == 'CHEF_DEPARTEMENT'}">
                            <button class="btn btn-sm btn-success" data-bs-toggle="modal" data-bs-target="#act-${v.id}-publish">
                                <i class="fa-solid fa-bullhorn me-1"></i> Publier
                            </button>
                        </c:if>
                    </c:if>
                    <c:if test="${v.state == 'PUBLISHED'}">
                        <c:if test="${currentUser.role == 'ADMIN_PEDAGOGIQUE' or currentUser.role == 'CHEF_DEPARTEMENT'}">
                            <button class="btn btn-sm btn-secondary" data-bs-toggle="modal" data-bs-target="#act-${v.id}-archive">
                                <i class="fa-solid fa-box-archive me-1"></i> Archiver
                            </button>
                        </c:if>
                    </c:if>
                    <c:if test="${currentUser.role == 'ADMIN_PEDAGOGIQUE' and not v.frozen}">
                        <button class="btn btn-sm btn-outline-info" data-bs-toggle="modal" data-bs-target="#act-${v.id}-freeze">
                            <i class="fa-solid fa-snowflake me-1"></i> Geler
                        </button>
                    </c:if>
                </div>
            </div>

            <details>
                <summary class="text-tiny text-muted">Historique des décisions (${not empty historyByVersion[v.id] ? fn:length(historyByVersion[v.id]) : 0})</summary>
                <table class="table table-sm mb-0">
                    <thead><tr><th>Date</th><th>Acteur</th><th>Décision</th><th>Commentaire</th></tr></thead>
                    <tbody>
                    <c:forEach var="a" items="${historyByVersion[v.id]}">
                        <tr>
                            <td><fmt:formatDate value="${a.occurredAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                            <td><c:out value="${a.actorUsername}"/> <span class="text-tiny">(<c:out value="${a.actorRole}"/>)</span></td>
                            <td><span class="badge bg-secondary"><c:out value="${a.decision}"/></span></td>
                            <td><c:out value="${a.comment}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty historyByVersion[v.id]}">
                        <tr><td colspan="4" class="text-center text-muted">Aucun événement.</td></tr>
                    </c:if>
                    </tbody>
                </table>
            </details>
        </div>

        <!-- Action modals (one form each) -->
        <c:set var="actions" value="request:Soumettre pour validation:requestApproval.do:primary,approve:Approuver:approve.do:success,reject:Rejeter:reject.do:danger,publish:Publier (gel automatique):publishVersion.do:success,archive:Archiver:archiveVersion.do:secondary,freeze:Geler manuellement:freezeVersion.do:info"/>
        <c:forTokens var="entry" items="${actions}" delims=",">
            <c:set var="parts" value="${fn:split(entry, ':')}"/>
            <div class="modal fade" id="act-${v.id}-${parts[0]}" tabindex="-1">
                <div class="modal-dialog">
                    <form method="post" action="${parts[2]}" class="modal-content">
                        <input type="hidden" name="versionId" value="${v.id}">
                        <div class="modal-header">
                            <h5 class="modal-title"><c:out value="${parts[1]}"/> — <c:out value="${v.displayName}"/></h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <label>Commentaire (optionnel)</label>
                            <textarea name="comment" class="form-control" rows="3"></textarea>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                            <button type="submit" class="btn btn-${parts[3]}"><c:out value="${parts[1]}"/></button>
                        </div>
                    </form>
                </div>
            </div>
        </c:forTokens>
    </c:forEach>

    <c:if test="${empty versions}">
        <div class="panel"><p class="text-muted mb-0">Aucune version dans la session active.</p></div>
    </c:if>
</c:if>

<%@ include file="_layout_foot.jspf" %>
