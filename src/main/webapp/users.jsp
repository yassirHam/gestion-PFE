<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="pageTitle" value="Utilisateurs" scope="request"/>
<%@ include file="_layout_head.jspf" %>

<h2 class="mb-4 fw-bold"><i class="fa-solid fa-users-gear me-2 text-primary"></i> Utilisateurs et rôles</h2>

<div class="panel">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h2 class="m-0">Comptes (${fn:length(users)})</h2>
        <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#newUserModal">
            <i class="fa-solid fa-user-plus me-1"></i> Nouvel utilisateur
        </button>
    </div>
    <div class="table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr><th>Identifiant</th><th>Nom</th><th>Email</th><th>Rôle</th><th>Département</th><th>Filières</th><th>Actif</th><th>Dernière connexion</th><th class="text-end">Actions</th></tr></thead>
            <tbody>
            <c:forEach var="u" items="${users}">
                <tr>
                    <td><strong><c:out value="${u.username}"/></strong></td>
                    <td><c:out value="${u.fullName}"/></td>
                    <td class="text-tiny"><c:out value="${u.email}"/></td>
                    <td><span class="badge bg-secondary"><c:out value="${u.role.label}"/></span></td>
                    <td><c:if test="${not empty u.department}"><c:out value="${u.department.name}"/></c:if></td>
                    <td class="text-tiny"><c:out value="${u.scopedFilieres}"/></td>
                    <td><c:choose><c:when test="${u.active}"><i class="fa-solid fa-circle-check text-success"></i></c:when><c:otherwise><i class="fa-solid fa-circle-xmark text-secondary"></i></c:otherwise></c:choose></td>
                    <td class="text-tiny"><fmt:formatDate value="${u.lastLoginAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary" data-bs-toggle="modal" data-bs-target="#editUser-${u.id}"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn btn-sm btn-outline-warning" data-bs-toggle="modal" data-bs-target="#pwdUser-${u.id}"><i class="fa-solid fa-key"></i></button>
                        <c:if test="${u.id != currentUser.id}">
                            <form method="post" action="deleteUser.do" class="d-inline" onsubmit="return confirm('Désactiver cet utilisateur ?');">
                                <input type="hidden" name="id" value="${u.id}">
                                <button class="btn btn-sm btn-outline-danger"><i class="fa-solid fa-user-slash"></i></button>
                            </form>
                        </c:if>
                    </td>
                </tr>
                <!-- Edit modal -->
                <div class="modal fade" id="editUser-${u.id}" tabindex="-1">
                    <div class="modal-dialog">
                        <form method="post" action="saveUser.do" class="modal-content">
                            <input type="hidden" name="id" value="${u.id}">
                            <div class="modal-header">
                                <h5 class="modal-title">Modifier <c:out value="${u.username}"/></h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body row g-2">
                                <div class="col-md-6"><label>Nom complet</label><input type="text" name="fullName" class="form-control" value="<c:out value='${u.fullName}'/>"></div>
                                <div class="col-md-6"><label>Email</label><input type="email" name="email" class="form-control" value="<c:out value='${u.email}'/>"></div>
                                <div class="col-md-6"><label>Rôle</label>
                                    <select name="role" class="form-select">
                                        <c:forEach var="r" items="${roles}"><option value="${r}" ${u.role == r ? 'selected' : ''}><c:out value="${r.label}"/></option></c:forEach>
                                    </select>
                                </div>
                                <div class="col-md-6"><label>Département</label>
                                    <select name="departmentId" class="form-select">
                                        <option value="">—</option>
                                        <c:forEach var="d" items="${departments}"><option value="${d.id}" ${u.department.id == d.id ? 'selected' : ''}><c:out value="${d.name}"/></option></c:forEach>
                                    </select>
                                </div>
                                <div class="col-md-6"><label>Lien Professeur</label>
                                    <select name="professeurId" class="form-select">
                                        <option value="">—</option>
                                        <c:forEach var="p" items="${professeurs}"><option value="${p.idp}" ${u.professeur.idp == p.idp ? 'selected' : ''}><c:out value="${p.nom} ${p.prenom}"/></option></c:forEach>
                                    </select>
                                </div>
                                <div class="col-md-6"><label>Filières scopées (séparées par virgule)</label><input type="text" name="scopedFilieres" class="form-control" value="<c:out value='${u.scopedFilieres}'/>"></div>
                                <div class="col-md-6"><label>Mot de passe (laisser vide pour ne pas changer)</label><input type="password" name="password" class="form-control"></div>
                                <div class="col-md-6"><div class="form-check mt-4"><input type="checkbox" name="active" class="form-check-input" id="active-${u.id}" ${u.active ? 'checked' : ''}><label class="form-check-label" for="active-${u.id}">Actif</label></div></div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                                <button class="btn btn-primary">Enregistrer</button>
                            </div>
                        </form>
                    </div>
                </div>
                <!-- Password reset modal -->
                <div class="modal fade" id="pwdUser-${u.id}" tabindex="-1">
                    <div class="modal-dialog">
                        <form method="post" action="changeUserPassword.do" class="modal-content">
                            <input type="hidden" name="id" value="${u.id}">
                            <div class="modal-header"><h5 class="modal-title">Changer le mot de passe</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                            <div class="modal-body">
                                <label>Nouveau mot de passe</label>
                                <input type="password" name="password" class="form-control" required>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                                <button class="btn btn-warning">Mettre à jour</button>
                            </div>
                        </form>
                    </div>
                </div>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<!-- New user modal -->
<div class="modal fade" id="newUserModal" tabindex="-1">
    <div class="modal-dialog">
        <form method="post" action="saveUser.do" class="modal-content">
            <div class="modal-header"><h5 class="modal-title">Nouvel utilisateur</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
            <div class="modal-body row g-2">
                <div class="col-md-6"><label>Identifiant *</label><input type="text" name="username" class="form-control" required></div>
                <div class="col-md-6"><label>Mot de passe *</label><input type="password" name="password" class="form-control" required></div>
                <div class="col-md-6"><label>Nom complet</label><input type="text" name="fullName" class="form-control"></div>
                <div class="col-md-6"><label>Email</label><input type="email" name="email" class="form-control"></div>
                <div class="col-md-6"><label>Rôle *</label>
                    <select name="role" class="form-select" required>
                        <c:forEach var="r" items="${roles}"><option value="${r}"><c:out value="${r.label}"/></option></c:forEach>
                    </select>
                </div>
                <div class="col-md-6"><label>Département</label>
                    <select name="departmentId" class="form-select">
                        <option value="">—</option>
                        <c:forEach var="d" items="${departments}"><option value="${d.id}"><c:out value="${d.name}"/></option></c:forEach>
                    </select>
                </div>
                <div class="col-md-6"><label>Filières scopées</label><input type="text" name="scopedFilieres" class="form-control" placeholder="GI,GINF"></div>
                <div class="col-md-6"><div class="form-check mt-4"><input type="checkbox" name="active" class="form-check-input" id="newActive" checked><label class="form-check-label" for="newActive">Actif</label></div></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Annuler</button>
                <button class="btn btn-primary">Créer</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="_layout_foot.jspf" %>
