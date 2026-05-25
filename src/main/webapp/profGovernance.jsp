<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Gouvernance professeurs" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-chalkboard-user me-2 text-primary"></i> Gouvernance des professeurs</h2>

<div class="panel">
    <h2 class="mb-2">Données opérationnelles</h2>
    <p class="text-tiny mb-3">Les informations ci-dessous alimentent le moteur de planning et la composition des jurys.</p>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr>
                <th>Professeur</th><th>Email / Tél.</th>
                <th>Langues</th><th>Max/jour</th><th>Interne</th><th>Exclu ?</th><th class="text-end">Actions</th>
            </tr></thead>
            <tbody>
            <c:forEach var="p" items="${profs}">
                <tr>
                    <td><strong><c:out value="${p.nom} ${p.prenom}"/></strong></td>
                    <td class="text-tiny">
                        <div><c:out value="${p.email}"/></div>
                        <div class="text-muted"><c:out value="${p.phone}"/></div>
                    </td>
                    <td class="text-tiny"><c:out value="${p.languages}"/></td>
                    <td><c:if test="${not empty p.maxSoutenancesPerDay}">${p.maxSoutenancesPerDay}</c:if></td>
                    <td><c:if test="${p.internal}"><i class="fa-solid fa-circle-check text-success"></i></c:if><c:if test="${not p.internal}"><span class="badge bg-info">Externe</span></c:if></td>
                    <td>
                        <c:choose>
                            <c:when test="${p.excluded}">
                                <span class="badge bg-danger" title="${p.exclusionReason}"><i class="fa-solid fa-ban"></i> exclu</span>
                            </c:when>
                            <c:otherwise><span class="text-tiny text-muted">non</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#editProf-${p.idp}"><i class="fa-solid fa-pen"></i></button>
                        <c:choose>
                            <c:when test="${p.excluded}">
                                <form method="post" action="reinstateProf.do" class="d-inline">
                                    <input type="hidden" name="id" value="${p.idp}">
                                    <button class="btn btn-sm btn-outline-success" title="Réintégrer"><i class="fa-solid fa-rotate-left"></i></button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <button class="btn btn-sm btn-outline-danger" data-bs-toggle="modal" data-bs-target="#exclProf-${p.idp}" title="Exclure"><i class="fa-solid fa-ban"></i></button>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<c:forEach var="p" items="${profs}">
    <!-- Edit operational fields -->
    <div class="modal fade" id="editProf-${p.idp}" tabindex="-1">
        <div class="modal-dialog">
            <form method="post" action="saveProfGovernance.do" class="modal-content">
                <input type="hidden" name="id" value="${p.idp}">
                <div class="modal-header"><h5 class="modal-title"><c:out value="${p.nom} ${p.prenom}"/></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body row g-2">
                    <div class="col-md-6"><label>Email</label><input type="email" name="email" class="form-control" value="<c:out value='${p.email}'/>"></div>
                    <div class="col-md-6"><label>Téléphone</label><input type="text" name="phone" class="form-control" value="<c:out value='${p.phone}'/>"></div>
                    <div class="col-md-6"><label>Langues (CSV: fr,en,ar)</label><input type="text" name="languages" class="form-control" value="<c:out value='${p.languages}'/>"></div>
                    <div class="col-md-6"><label>Max soutenances/jour</label><input type="number" name="maxSoutenancesPerDay" class="form-control" value="<c:out value='${p.maxSoutenancesPerDay}'/>"></div>
                    <div class="col-md-12"><div class="form-check mt-2"><input type="checkbox" name="internal" class="form-check-input" id="int-${p.idp}" ${p.internal ? 'checked' : ''}><label class="form-check-label" for="int-${p.idp}">Interne à l'établissement</label></div></div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                    <button class="btn btn-primary">Enregistrer</button>
                </div>
            </form>
        </div>
    </div>
    <!-- Exclude prof -->
    <div class="modal fade" id="exclProf-${p.idp}" tabindex="-1">
        <div class="modal-dialog">
            <form method="post" action="excludeProf.do" class="modal-content">
                <input type="hidden" name="id" value="${p.idp}">
                <div class="modal-header"><h5 class="modal-title">Exclure temporairement</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body">
                    <p class="text-tiny text-muted mb-2">Ce professeur sera retiré de toute nouvelle composition de jury et affectation jusqu'à sa réintégration.</p>
                    <label>Motif</label>
                    <textarea name="reason" class="form-control" rows="3"></textarea>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                    <button class="btn btn-danger">Exclure</button>
                </div>
            </form>
        </div>
    </div>
</c:forEach>

<%@ include file="_layout_foot.jspf" %>
