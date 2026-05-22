<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Indisponibilités professeurs" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-calendar-xmark me-2 text-primary"></i> Indisponibilités &amp; préférences des professeurs</h2>

<div class="panel">
    <h2 class="mb-3">Déclarer une indisponibilité</h2>
    <form method="post" action="saveProfAvailability.do" class="row g-2 align-items-end">
        <div class="col-md-3">
            <label class="form-label small text-uppercase fw-semibold text-muted">Professeur *</label>
            <select name="professeurId" class="form-select form-select-sm" required>
                <option value="">— Sélectionner —</option>
                <c:forEach var="p" items="${profs}"><option value="${p.idp}"><c:out value="${p.nom} ${p.prenom}"/></option></c:forEach>
            </select>
        </div>
        <div class="col-md-2">
            <label class="form-label small text-uppercase fw-semibold text-muted">Date *</label>
            <input type="date" name="theDate" class="form-control form-control-sm" required>
        </div>
        <div class="col-md-2">
            <label class="form-label small text-uppercase fw-semibold text-muted">Période</label>
            <select name="period" class="form-select form-select-sm">
                <c:forEach var="x" items="${periods}"><option value="${x}"><c:out value="${x}"/></option></c:forEach>
            </select>
        </div>
        <div class="col-md-2">
            <label class="form-label small text-uppercase fw-semibold text-muted">Type</label>
            <select name="kind" class="form-select form-select-sm">
                <c:forEach var="x" items="${kinds}"><option value="${x}"><c:out value="${x}"/></option></c:forEach>
            </select>
        </div>
        <div class="col-md-2">
            <label class="form-label small text-uppercase fw-semibold text-muted">Motif</label>
            <input type="text" name="reason" class="form-control form-control-sm">
        </div>
        <div class="col-md-1">
            <button class="btn btn-primary btn-sm w-100"><i class="fa-solid fa-plus"></i></button>
        </div>
    </form>
</div>

<div class="panel">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="m-0">Déclarations actuelles</h2>
        <span class="text-tiny text-muted">
            <c:if test="${not empty activeSession}">Session : <c:out value="${activeSession.displayName}"/></c:if>
        </span>
    </div>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr><th>Professeur</th><th>Date</th><th>Période</th><th>Type</th><th>Motif</th><th>Déclarée le</th><th class="text-end">Action</th></tr></thead>
            <tbody>
            <c:forEach var="a" items="${availabilities}">
                <tr>
                    <td><strong><c:out value="${a.professeur.nom} ${a.professeur.prenom}"/></strong></td>
                    <td><fmt:formatDate value="${a.theDate}" pattern="yyyy-MM-dd"/></td>
                    <td><span class="badge bg-light text-dark"><c:out value="${a.period}"/></span></td>
                    <td><span class="badge bg-${a.kind == 'PREFERRED' ? 'success' : 'warning'} text-${a.kind == 'PREFERRED' ? 'white' : 'dark'}"><c:out value="${a.kind}"/></span></td>
                    <td class="text-tiny"><c:out value="${a.reason}"/></td>
                    <td class="text-tiny"><fmt:formatDate value="${a.declaredAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td class="text-end">
                        <form method="post" action="deleteProfAvailability.do" class="d-inline" onsubmit="return confirm('Supprimer cette déclaration ?');">
                            <input type="hidden" name="id" value="${a.id}">
                            <button class="btn btn-sm btn-outline-danger"><i class="fa-solid fa-trash"></i></button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty availabilities}">
                <tr><td colspan="7" class="text-center text-muted py-3">Aucune indisponibilité déclarée.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="_layout_foot.jspf" %>
