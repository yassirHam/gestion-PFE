<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Gouvernance salles" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-door-open me-2 text-primary"></i> Gouvernance des salles</h2>

<div class="panel">
    <h2 class="mb-3">Salles (${fn:length(salles)})</h2>
    <p class="text-tiny mb-3">La priorité, la disponibilité et le rattachement à un département pilotent l'attribution des salles par le moteur de planning.</p>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr>
                <th>N°</th><th>Block</th><th>Campus</th><th>Capacité</th><th>Équipement</th>
                <th>Priorité</th><th>Disponible</th><th>Département</th><th class="text-end">Actions</th>
            </tr></thead>
            <tbody>
            <c:forEach var="s" items="${salles}">
                <tr>
                    <td><strong><c:out value="${s.num_salle}"/></strong></td>
                    <td><c:out value="${s.block}"/></td>
                    <td class="text-tiny"><c:out value="${s.campus}"/></td>
                    <td><c:out value="${s.capacity}"/></td>
                    <td class="text-tiny"><c:out value="${s.equipment}"/></td>
                    <td><span class="badge bg-secondary">${s.priority}</span></td>
                    <td><c:choose><c:when test="${s.available}"><i class="fa-solid fa-circle-check text-success"></i></c:when><c:otherwise><i class="fa-solid fa-circle-xmark text-danger"></i></c:otherwise></c:choose></td>
                    <td><c:if test="${not empty s.department}"><c:out value="${s.department.name}"/></c:if></td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#editSalle-${s.id_salle}">
                            <i class="fa-solid fa-pen"></i>
                        </button>
                    </td>
                </tr>
                <div class="modal fade" id="editSalle-${s.id_salle}" tabindex="-1">
                    <div class="modal-dialog">
                        <form method="post" action="saveSalleGovernance.do" class="modal-content">
                            <input type="hidden" name="id" value="${s.id_salle}">
                            <div class="modal-header"><h5 class="modal-title">Salle <c:out value="${s.num_salle}"/></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                            <div class="modal-body row g-2">
                                <div class="col-md-6"><label>Block</label><input type="text" name="block" class="form-control" value="<c:out value='${s.block}'/>"></div>
                                <div class="col-md-6"><label>Campus</label><input type="text" name="campus" class="form-control" value="<c:out value='${s.campus}'/>"></div>
                                <div class="col-md-4"><label>Capacité</label><input type="number" name="capacity" class="form-control" value="<c:out value='${s.capacity}'/>"></div>
                                <div class="col-md-4"><label>Priorité</label><input type="number" name="priority" class="form-control" value="${s.priority}"></div>
                                <div class="col-md-4"><label>Département</label>
                                    <select name="departmentId" class="form-select">
                                        <option value="">—</option>
                                        <c:forEach var="d" items="${departments}"><option value="${d.id}" ${s.department.id == d.id ? 'selected' : ''}><c:out value="${d.name}"/></option></c:forEach>
                                    </select>
                                </div>
                                <div class="col-md-12"><label>Équipement</label><input type="text" name="equipment" class="form-control" value="<c:out value='${s.equipment}'/>" placeholder="Vidéoprojecteur, tableau, ..."></div>
                                <div class="col-md-12"><div class="form-check mt-2"><input type="checkbox" name="available" class="form-check-input" id="av-${s.id_salle}" ${s.available ? 'checked' : ''}><label class="form-check-label" for="av-${s.id_salle}">Salle disponible pour le planning</label></div></div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                                <button class="btn btn-primary">Enregistrer</button>
                            </div>
                        </form>
                    </div>
                </div>
            </c:forEach>
            <c:if test="${empty salles}"><tr><td colspan="9" class="text-center text-muted py-3">Aucune salle.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="_layout_foot.jspf" %>
