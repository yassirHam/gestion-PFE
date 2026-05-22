<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Convocations" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-paper-plane me-2 text-primary"></i> Convocations &amp; rappels</h2>

<div class="panel">
    <div class="row g-3 align-items-stretch">
        <div class="col-md-4">
            <h2 class="mb-2"><i class="fa-solid fa-envelope me-1"></i> Convocations</h2>
            <p class="text-tiny mb-2">Préparer convocations étudiants + jury pour toutes les soutenances.</p>
            <form method="post" action="sendConvocations.do">
                <button class="btn btn-primary w-100"><i class="fa-solid fa-paper-plane me-1"></i> Préparer toutes</button>
            </form>
        </div>
        <div class="col-md-4">
            <h2 class="mb-2"><i class="fa-solid fa-bell me-1"></i> Rappels</h2>
            <p class="text-tiny mb-2">Envoyer un rappel à chaque étudiant dont la soutenance approche.</p>
            <form method="post" action="sendReminders.do">
                <button class="btn btn-warning w-100"><i class="fa-solid fa-bell me-1"></i> Envoyer rappels</button>
            </form>
        </div>
        <div class="col-md-4">
            <h2 class="mb-2"><i class="fa-solid fa-tower-broadcast me-1"></i> Dispatch SMTP</h2>
            <p class="text-tiny mb-2">Tente l'envoi SMTP des notifications en file (${queuedCount}).</p>
            <form method="post" action="dispatchNotifications.do">
                <button class="btn btn-success w-100" ${queuedCount == 0 ? 'disabled' : ''}>
                    <i class="fa-solid fa-paper-plane me-1"></i> Envoyer la file
                </button>
            </form>
        </div>
    </div>
    <c:if test="${empty appSettings.smtpHost or not appSettings.smtpEnabled}">
        <div class="alert alert-warning small mt-3 mb-0">
            <i class="fa-solid fa-circle-exclamation me-1"></i>
            SMTP non activé. Les notifications restent en file et peuvent être consultées ci-dessous.
            <a href="settings.do">Configurer SMTP</a>
        </div>
    </c:if>
</div>

<div class="panel">
    <h2 class="mb-3">Notifications récentes (${fn:length(notifications)})</h2>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr>
                <th>Date</th><th>Type</th><th>Destinataire</th><th>Sujet</th><th>État</th><th>Erreur</th>
            </tr></thead>
            <tbody>
            <c:forEach var="n" items="${notifications}">
                <tr>
                    <td><fmt:formatDate value="${n.queuedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td><span class="badge bg-secondary"><c:out value="${n.kind}"/></span></td>
                    <td>
                        <c:out value="${n.recipientName}"/>
                        <c:if test="${not empty n.recipient}"><span class="text-tiny d-block"><c:out value="${n.recipient}"/></span></c:if>
                    </td>
                    <td><c:out value="${n.subject}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${n.status == 'SENT'}"><span class="badge bg-success">Envoyé</span></c:when>
                            <c:when test="${n.status == 'FAILED'}"><span class="badge bg-danger">Échec</span></c:when>
                            <c:when test="${n.status == 'CANCELLED'}"><span class="badge bg-secondary">Annulé</span></c:when>
                            <c:otherwise><span class="badge bg-warning text-dark">En file</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-tiny text-danger"><c:out value="${n.errorMessage}"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty notifications}">
                <tr><td colspan="6" class="text-center text-muted py-3">Aucune notification.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="_layout_foot.jspf" %>
