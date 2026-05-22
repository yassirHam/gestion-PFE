<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Gouvernance professeurs" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-chalkboard-user me-2 text-primary"></i> Gouvernance des professeurs</h2>

<div class="panel">
    <h2 class="mb-2">Données opérationnelles</h2>
    <p class="text-tiny mb-3">Le grade, l'appartenance, les langues parlées et les capacités définies ici alimentent le moteur de planning.</p>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr>
                <th>Professeur</th><th>Discipline</th><th>Grade</th><th>Interne</th>
                <th>Langues</th><th>Max/jour</th><th>VIP</th><th>Département</th><th>Exclu ?</th><th class="text-end">Actions</th>
            </tr></thead>
            <tbody>
            <c:forEach var="p" items="${profs}">
                <tr>
                    <td><strong><c:out value="${p.nom} ${p.prenom}"/></strong>
                        <div class="text-tiny"><c:out value="${p.email}"/></div>
                    </td>
                    <td class="text-tiny"><c:out value="${p.discipline}"/> / <c:out value="${p.specialite}"/></td>
                    <td><span class="badge bg-secondary"><c:out value="${p.grade}"/></span></td>
                    <td><c:if test="${p.internal}"><i class="fa-solid fa-circle-check text-success"></i></c:if><c:if test="${not p.internal}"><span class="badge bg-info">Externe</span></c:if></td>
                    <td class="text-tiny"><c:out value="${p.languages}"/></td>
                    <td><c:if test="${not empty p.maxSoutenancesPerDay}">${p.maxSoutenancesPerDay}</c:if></td>
                    <td><c:if test="${p.vip}"><i class="fa-solid fa-star text-warning" title="VIP"></i></c:if></td>
                    <td><c:if test="${not empty p.department}"><c:out value="${p.department.name}"/></c:if></td>
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
                <!-- Edit operational fields -->
                <div class="modal fade" id="editProf-${p.idp}" tabindex="-1">
                    <div class="modal-dialog">
                        <form method="post" action="saveProfGovernance.do" class="modal-content">
                            <input type="hidden" name="id" value="${p.idp}">
                            <div class="modal-header"><h5 class="modal-title"><c:out value="${p.nom} ${p.prenom}"/></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                            <div class="modal-body row g-2">
                                <div class="col-md-6"><label>Email</label><input type="email" name="email" class="form-control" value="<c:out value='${p.email}'/>"></div>
                                <div class="col-md-6"><label>Téléphone</label><input type="text" name="phone" class="form-control" value="<c:out value='${p.phone}'/>"></div>
                                <div class="col-md-6"><label>Grade</label>
                                    <select name="grade" class="form-select">
                                        <c:forEach var="g" items="${grades}"><option value="${g}" ${p.grade == g ? 'selected' : ''}><c:out value="${g.label}"/></option></c:forEach>
                                    </select>
                                </div>
                                <div class="col-md-6"><label>Département</label>
                                    <select name="departmentId" class="form-select">
                                        <option value="">—</option>
                                        <c:forEach var="d" items="${departments}"><option value="${d.id}" ${p.department.id == d.id ? 'selected' : ''}><c:out value="${d.name}"/></option></c:forEach>
                                    </select>
                                </div>
                                <div class="col-md-6"><label>Langues (CSV: fr,en,ar)</label><input type="text" name="languages" class="form-control" value="<c:out value='${p.languages}'/>"></div>
                                <div class="col-md-6"><label>Max soutenances/jour</label><input type="number" name="maxSoutenancesPerDay" class="form-control" value="<c:out value='${p.maxSoutenancesPerDay}'/>"></div>
                                <div class="col-md-6"><div class="form-check mt-4"><input type="checkbox" name="internal" class="form-check-input" id="int-${p.idp}" ${p.internal ? 'checked' : ''}><label class="form-check-label" for="int-${p.idp}">Interne à l'établissement</label></div></div>
                                <div class="col-md-6"><div class="form-check mt-4"><input type="checkbox" name="vip" class="form-check-input" id="vip-${p.idp}" ${p.vip ? 'checked' : ''}><label class="form-check-label" for="vip-${p.idp}">VIP / prioritaire</label></div></div>
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
                                <p class="text-tiny text-muted mb-2">Ce professeur sera retiré de toute nouvelle composition de jury jusqu'à sa réintégration.</p>
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
            </tbody>
        </table>
    </div>
</div>

<%@ include file="_layout_foot.jspf" %>
