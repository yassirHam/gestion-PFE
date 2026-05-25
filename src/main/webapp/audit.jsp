<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Journal d'audit" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-clipboard-list me-2 text-primary"></i> Journal d'audit</h2>

<div class="panel">
    <form method="get" action="audit.do" class="row g-2 align-items-end">
        <div class="col-md-4">
            <label class="form-label small text-uppercase fw-semibold text-muted">Acteur</label>
            <select name="actorId" class="form-select form-select-sm">
                <option value="">— Tous les acteurs —</option>
                <c:forEach var="u" items="${users}">
                    <option value="${u.id}" ${filterActorId == u.id ? 'selected' : ''}>
                        <c:out value="${u.username}"/> (<c:out value="${u.role}"/>)
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="col-md-3">
            <label class="form-label small text-uppercase fw-semibold text-muted">Limite</label>
            <input type="number" name="limit" class="form-control form-control-sm" value="${limit}" min="10" max="1000">
        </div>
        <div class="col-md-2">
            <button class="btn btn-primary btn-sm w-100"><i class="fa-solid fa-magnifying-glass me-1"></i> Filtrer</button>
        </div>
    </form>
</div>

<div class="panel">
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead>
                <tr>
                    <th>Quand</th>
                    <th>Acteur</th>
                    <th>Action</th>
                    <th>Cible</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
            <c:forEach var="l" items="${logs}">
                <tr>
                    <td class="text-tiny"><fmt:formatDate value="${l.occurredAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${not empty l.actorUsername}">
                                <strong><c:out value="${l.actorUsername}"/></strong>
                            </c:when>
                            <c:otherwise><span class="text-muted">système</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td><span class="badge bg-secondary"><c:out value="${l.action}"/></span></td>
                    <td class="text-tiny">
                        <c:if test="${not empty l.targetType}">
                            <c:out value="${l.targetType}"/><c:if test="${not empty l.targetId}">#${l.targetId}</c:if>
                        </c:if>
                    </td>
                    <td><c:out value="${l.summary}"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty logs}">
                <tr><td colspan="5" class="text-center text-muted py-3">Aucune entrée.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="_layout_foot.jspf" %>
