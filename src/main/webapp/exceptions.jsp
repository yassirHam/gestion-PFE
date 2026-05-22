<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Incidents" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-triangle-exclamation me-2 text-danger"></i> Gestion des incidents</h2>

<div class="panel">
    <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <h2 class="m-0">Signaler un incident</h2>
    </div>
    <form method="post" action="reportException.do" class="row g-2 align-items-end">
        <div class="col-md-4">
            <label class="form-label small text-uppercase fw-semibold text-muted">Soutenance</label>
            <select name="soutenanceId" class="form-select form-select-sm" required>
                <option value="">— Sélectionner —</option>
                <c:forEach var="s" items="${soutenances}">
                    <option value="${s.ids}">
                        <fmt:formatDate value="${s.date}" pattern="yyyy-MM-dd"/> ${s.heure} — <c:out value="${s.etudiant.nomE} ${s.etudiant.prenomE}"/>
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="col-md-3">
            <label class="form-label small text-uppercase fw-semibold text-muted">Type</label>
            <select name="type" class="form-select form-select-sm" required>
                <c:forEach var="t" items="${exceptionTypes}">
                    <option value="${t}"><c:out value="${t.label}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="col-md-4">
            <label class="form-label small text-uppercase fw-semibold text-muted">Description</label>
            <input type="text" name="description" class="form-control form-control-sm" placeholder="Détail de l'incident...">
        </div>
        <div class="col-md-1">
            <button class="btn btn-danger btn-sm w-100"><i class="fa-solid fa-paper-plane"></i></button>
        </div>
    </form>
</div>

<div class="panel">
    <h2 class="mb-3">Incidents ouverts (${fn:length(openExceptions)})</h2>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr>
                <th>Date</th><th>Type</th><th>Étudiant</th><th>Soutenance</th><th>Description</th><th>État</th><th class="text-end">Action</th>
            </tr></thead>
            <tbody>
            <c:forEach var="e" items="${openExceptions}">
                <tr>
                    <td><fmt:formatDate value="${e.reportedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td><span class="badge bg-warning text-dark"><c:out value="${e.type.label}"/></span></td>
                    <td>
                        <c:if test="${not empty e.soutenance.etudiant}">
                            <c:out value="${e.soutenance.etudiant.nomE} ${e.soutenance.etudiant.prenomE}"/>
                        </c:if>
                    </td>
                    <td>
                        <fmt:formatDate value="${e.soutenance.date}" pattern="yyyy-MM-dd"/> ${e.soutenance.heure}
                        <c:if test="${not empty e.soutenance.salle}">— <c:out value="${e.soutenance.salle.num_salle}"/></c:if>
                    </td>
                    <td><span class="text-tiny"><c:out value="${e.description}"/></span></td>
                    <td><span class="badge bg-secondary"><c:out value="${e.status}"/></span></td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-success" data-bs-toggle="modal" data-bs-target="#resolve-${e.id}">
                            <i class="fa-solid fa-circle-check"></i> Résoudre
                        </button>
                    </td>
                </tr>
                <!-- Resolve modal -->
                <div class="modal fade" id="resolve-${e.id}" tabindex="-1">
                    <div class="modal-dialog">
                        <form method="post" action="resolveException.do" class="modal-content">
                            <input type="hidden" name="id" value="${e.id}">
                            <div class="modal-header">
                                <h5 class="modal-title">Résoudre l'incident</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <p class="text-tiny text-muted mb-2"><strong><c:out value="${e.type.label}"/></strong> — <c:out value="${e.description}"/></p>
                                <label>Action opérationnelle</label>
                                <select name="action" class="form-select form-select-sm mb-2">
                                    <c:forEach var="a" items="${resolutionActions}">
                                        <option value="${a}"><c:out value="${a.label}"/></option>
                                    </c:forEach>
                                </select>
                                <label>Notes de résolution</label>
                                <textarea name="notes" class="form-control" rows="3"></textarea>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                                <button type="submit" class="btn btn-success">Résoudre</button>
                            </div>
                        </form>
                    </div>
                </div>
            </c:forEach>
            <c:if test="${empty openExceptions}">
                <tr><td colspan="7" class="text-center text-muted py-3">Aucun incident ouvert.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<details class="panel">
    <summary class="fw-semibold">Historique récent (${fn:length(recentExceptions)})</summary>
    <table class="table table-sm mt-2 mb-0">
        <thead><tr><th>Date</th><th>Type</th><th>Étudiant</th><th>État</th><th>Notes</th></tr></thead>
        <tbody>
        <c:forEach var="e" items="${recentExceptions}">
            <tr>
                <td><fmt:formatDate value="${e.reportedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                <td><c:out value="${e.type.label}"/></td>
                <td><c:if test="${not empty e.soutenance.etudiant}"><c:out value="${e.soutenance.etudiant.nomE}"/></c:if></td>
                <td><span class="badge bg-light text-dark"><c:out value="${e.status}"/></span></td>
                <td class="text-tiny"><c:out value="${e.resolutionNotes}"/></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</details>

<%@ include file="_layout_foot.jspf" %>
