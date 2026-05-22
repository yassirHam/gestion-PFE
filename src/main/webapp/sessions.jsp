<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Sessions et versions" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-flag me-2 text-primary"></i> Sessions et versions de planning</h2>

<!-- ─── Active session ──────────────────────────────────── -->
<div class="panel">
    <div class="d-flex justify-content-between align-items-start mb-3 flex-wrap gap-2">
        <div>
            <h2 class="mb-1">Session active</h2>
            <c:choose>
                <c:when test="${not empty activeSession}">
                    <div class="fs-5 fw-semibold"><c:out value="${activeSession.displayName}"/></div>
                    <div class="text-tiny">
                        Code : <code><c:out value="${activeSession.code}"/></code>
                        &middot; Année : <c:out value="${activeSession.academicYear}"/>
                        <c:if test="${not empty activeSession.deadlineDate}">
                            &middot; Date butoir : <fmt:formatDate value="${activeSession.deadlineDate}" pattern="yyyy-MM-dd"/>
                        </c:if>
                    </div>
                </c:when>
                <c:otherwise>
                    <span class="text-muted">Aucune session active.</span>
                </c:otherwise>
            </c:choose>
        </div>
        <button type="button" class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#newSessionModal">
            <i class="fa-solid fa-plus me-1"></i> Nouvelle session
        </button>
    </div>

    <!-- Versions of the active session -->
    <c:if test="${not empty activeSession}">
        <div class="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
            <h2 class="m-0">Versions</h2>
            <form method="post" action="createVersion.do" class="d-flex gap-2">
                <input type="hidden" name="sessionId" value="${activeSession.id}">
                <input type="text" name="label" class="form-control form-control-sm" placeholder="Libellé (ex: Version corrigée)" style="min-width:240px">
                <button class="btn btn-sm btn-outline-primary"><i class="fa-solid fa-code-branch me-1"></i> Nouvelle version</button>
            </form>
        </div>
        <div class="table-responsive">
            <table class="table table-sm align-middle">
                <thead><tr>
                    <th>#</th><th>Libellé</th><th>État</th><th>Courante</th><th>Créée le</th><th>Publication</th><th class="text-end">Actions</th>
                </tr></thead>
                <tbody>
                <c:forEach var="v" items="${versions}">
                    <tr>
                        <td><code>v${v.versionNumber}</code></td>
                        <td><c:out value="${v.label}"/></td>
                        <td><span class="badge bg-${v.state.bootstrap} badge-state"><c:out value="${v.state.label}"/></span></td>
                        <td>
                            <c:choose>
                                <c:when test="${v.current}"><i class="fa-solid fa-circle-check text-success"></i></c:when>
                                <c:otherwise>
                                    <form method="post" action="setCurrentVersion.do" class="d-inline">
                                        <input type="hidden" name="sessionId" value="${activeSession.id}">
                                        <input type="hidden" name="versionId" value="${v.id}">
                                        <button class="btn btn-sm btn-link p-0">Définir comme courante</button>
                                    </form>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td><fmt:formatDate value="${v.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                        <td>
                            <c:if test="${not empty v.publishedAt}">
                                <i class="fa-solid fa-lock text-success me-1"></i>
                                <fmt:formatDate value="${v.publishedAt}" pattern="yyyy-MM-dd HH:mm"/>
                            </c:if>
                            <c:if test="${empty v.publishedAt and not empty v.frozenAt}">
                                <i class="fa-solid fa-snowflake text-info me-1"></i>
                                Gelée le <fmt:formatDate value="${v.frozenAt}" pattern="yyyy-MM-dd HH:mm"/>
                            </c:if>
                        </td>
                        <td class="text-end">
                            <a href="approvals.do" class="btn btn-sm btn-outline-secondary" title="Cycle de validation">
                                <i class="fa-solid fa-circle-check"></i>
                            </a>
                            <c:if test="${v.editable and currentUser.role == 'ADMIN_PEDAGOGIQUE'}">
                                <form method="post" action="deleteVersion.do" class="d-inline" onsubmit="return confirm('Supprimer cette version brouillon ?');">
                                    <input type="hidden" name="id" value="${v.id}">
                                    <button class="btn btn-sm btn-outline-danger"><i class="fa-solid fa-trash"></i></button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty versions}">
                    <tr><td colspan="7" class="text-center text-muted py-3">Aucune version dans cette session.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </c:if>
</div>

<!-- ─── All sessions ────────────────────────────────────── -->
<div class="panel">
    <h2 class="mb-3">Toutes les sessions</h2>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr>
                <th>Code</th><th>Libellé</th><th>Année</th><th>Période</th><th>Date butoir</th><th>État</th><th class="text-end">Actions</th>
            </tr></thead>
            <tbody>
            <c:forEach var="s" items="${sessions}">
                <tr>
                    <td><code><c:out value="${s.code}"/></code></td>
                    <td><c:out value="${s.label}"/></td>
                    <td><c:out value="${s.academicYear}"/></td>
                    <td>
                        <fmt:formatDate value="${s.startDate}" pattern="yyyy-MM-dd"/>
                        &rarr;
                        <fmt:formatDate value="${s.endDate}" pattern="yyyy-MM-dd"/>
                    </td>
                    <td><fmt:formatDate value="${s.deadlineDate}" pattern="yyyy-MM-dd"/></td>
                    <td>
                        <c:if test="${s.active}"><span class="badge bg-success">Active</span></c:if>
                        <c:if test="${s.closed}"><span class="badge bg-secondary">Clôturée</span></c:if>
                        <c:if test="${not s.active and not s.closed}"><span class="badge bg-light text-dark">En attente</span></c:if>
                    </td>
                    <td class="text-end">
                        <c:if test="${not s.active and not s.closed}">
                            <form method="post" action="activateSession.do" class="d-inline">
                                <input type="hidden" name="id" value="${s.id}">
                                <button class="btn btn-sm btn-outline-success"><i class="fa-solid fa-play"></i> Activer</button>
                            </form>
                        </c:if>
                        <c:if test="${not s.closed}">
                            <form method="post" action="closeSession.do" class="d-inline" onsubmit="return confirm('Clôturer la session ?');">
                                <input type="hidden" name="id" value="${s.id}">
                                <button class="btn btn-sm btn-outline-secondary"><i class="fa-solid fa-flag-checkered"></i> Clôturer</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty sessions}">
                <tr><td colspan="7" class="text-center text-muted py-3">Aucune session enregistrée.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<!-- ─── New session modal ─────────────────────────────────── -->
<div class="modal fade" id="newSessionModal" tabindex="-1">
    <div class="modal-dialog">
        <form method="post" action="createSession.do" class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Nouvelle session</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label>Code *</label>
                        <input type="text" name="code" class="form-control" required placeholder="JUIN-2026">
                    </div>
                    <div class="col-md-6">
                        <label>Libellé</label>
                        <input type="text" name="label" class="form-control" placeholder="Session de juin 2026">
                    </div>
                    <div class="col-md-6">
                        <label>Année universitaire</label>
                        <input type="text" name="academicYear" class="form-control" placeholder="2025-2026">
                    </div>
                    <div class="col-md-6">
                        <label>Département (optionnel)</label>
                        <select name="departmentId" class="form-select">
                            <option value="">— Tous les départements —</option>
                            <c:forEach var="d" items="${departments}">
                                <option value="${d.id}"><c:out value="${d.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-4"><label>Début</label><input type="date" name="startDate" class="form-control"></div>
                    <div class="col-md-4"><label>Fin</label><input type="date" name="endDate" class="form-control"></div>
                    <div class="col-md-4"><label>Date butoir</label><input type="date" name="deadlineDate" class="form-control"></div>
                    <div class="col-12">
                        <div class="form-check">
                            <input type="checkbox" name="activate" class="form-check-input" id="activateNow" checked>
                            <label class="form-check-label" for="activateNow">Activer immédiatement</label>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                <button type="submit" class="btn btn-primary">Créer</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="_layout_foot.jspf" %>
