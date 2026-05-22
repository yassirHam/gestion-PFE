<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Départements" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-building me-2 text-primary"></i> Départements</h2>

<div class="panel">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="m-0">Liste (${fn:length(departments)})</h2>
        <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#newDept">
            <i class="fa-solid fa-plus me-1"></i> Nouveau département
        </button>
    </div>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr><th>Code</th><th>Nom</th><th>Campus</th><th>Filières</th><th>Quotas</th><th>Actif</th><th class="text-end">Actions</th></tr></thead>
            <tbody>
            <c:forEach var="d" items="${departments}">
                <tr>
                    <td><code><c:out value="${d.code}"/></code></td>
                    <td><c:out value="${d.name}"/></td>
                    <td><c:out value="${d.campus}"/></td>
                    <td class="text-tiny"><c:out value="${d.filiereCodes}"/></td>
                    <td class="text-tiny">
                        <c:if test="${not empty d.maxSoutenancesPerDay}">Max sout./jour: ${d.maxSoutenancesPerDay}<br></c:if>
                        <c:if test="${not empty d.maxExternalMembers}">Max externes: ${d.maxExternalMembers}</c:if>
                    </td>
                    <td><c:choose><c:when test="${d.active}"><i class="fa-solid fa-circle-check text-success"></i></c:when><c:otherwise><i class="fa-solid fa-circle-xmark text-secondary"></i></c:otherwise></c:choose></td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#editDept-${d.id}"><i class="fa-solid fa-pen"></i></button>
                        <c:if test="${currentUser.role == 'ADMIN_PEDAGOGIQUE'}">
                            <form method="post" action="deleteDepartment.do" class="d-inline" onsubmit="return confirm('Supprimer ce département ?');">
                                <input type="hidden" name="id" value="${d.id}">
                                <button class="btn btn-sm btn-outline-danger"><i class="fa-solid fa-trash"></i></button>
                            </form>
                        </c:if>
                    </td>
                </tr>
                <!-- Edit modal -->
                <div class="modal fade" id="editDept-${d.id}" tabindex="-1">
                    <div class="modal-dialog">
                        <form method="post" action="saveDepartment.do" class="modal-content">
                            <input type="hidden" name="id" value="${d.id}">
                            <div class="modal-header"><h5 class="modal-title">Modifier <c:out value="${d.name}"/></h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                            <div class="modal-body row g-2">
                                <div class="col-md-4"><label>Code</label><input type="text" name="code" class="form-control" value="<c:out value='${d.code}'/>" required></div>
                                <div class="col-md-8"><label>Nom</label><input type="text" name="name" class="form-control" value="<c:out value='${d.name}'/>"></div>
                                <div class="col-md-6"><label>Campus</label><input type="text" name="campus" class="form-control" value="<c:out value='${d.campus}'/>"></div>
                                <div class="col-md-6"><label>Filières (CSV)</label><input type="text" name="filiereCodes" class="form-control" value="<c:out value='${d.filiereCodes}'/>"></div>
                                <div class="col-md-6"><label>Max soutenances/jour</label><input type="number" name="maxSoutenancesPerDay" class="form-control" value="<c:out value='${d.maxSoutenancesPerDay}'/>"></div>
                                <div class="col-md-6"><label>Max externes / jury</label><input type="number" name="maxExternalMembers" class="form-control" value="<c:out value='${d.maxExternalMembers}'/>"></div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                                <button class="btn btn-primary">Enregistrer</button>
                            </div>
                        </form>
                    </div>
                </div>
            </c:forEach>
            <c:if test="${empty departments}"><tr><td colspan="7" class="text-center text-muted py-3">Aucun département.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</div>

<!-- New department -->
<div class="modal fade" id="newDept" tabindex="-1">
    <div class="modal-dialog">
        <form method="post" action="saveDepartment.do" class="modal-content">
            <div class="modal-header"><h5 class="modal-title">Nouveau département</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
            <div class="modal-body row g-2">
                <div class="col-md-4"><label>Code *</label><input type="text" name="code" class="form-control" required></div>
                <div class="col-md-8"><label>Nom</label><input type="text" name="name" class="form-control"></div>
                <div class="col-md-6"><label>Campus</label><input type="text" name="campus" class="form-control"></div>
                <div class="col-md-6"><label>Filières (CSV)</label><input type="text" name="filiereCodes" class="form-control" placeholder="GI,GINF,GTR"></div>
                <div class="col-md-6"><label>Max soutenances/jour</label><input type="number" name="maxSoutenancesPerDay" class="form-control"></div>
                <div class="col-md-6"><label>Max externes / jury</label><input type="number" name="maxExternalMembers" class="form-control"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                <button class="btn btn-primary">Créer</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="_layout_foot.jspf" %>
