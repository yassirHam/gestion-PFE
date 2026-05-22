<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Gestion PFE - PVs</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
</head>
<body class="bg-light">

<!-- Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm mb-4">
    <div class="container-fluid px-3 px-lg-4">
        <a class="navbar-brand d-flex align-items-center" href="index.jsp">
            <img src="images/logo.png" alt="Logo" style="height: 38px; margin-right: 12px; object-fit: contain;">
            Gestion PFE
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link" href="affectation.do"><i class="fa-solid fa-users me-1"></i> Affectation</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="config.do"><i class="fa-solid fa-sliders me-1"></i> Configuration</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="planning.do"><i class="fa-solid fa-calendar-days me-1"></i> Planning</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link active" href="pv.do"><i class="fa-solid fa-file-lines me-1"></i> PVs</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="dashboard.do"><i class="fa-solid fa-chart-pie me-1"></i> Dashboard</a>
                </li>
            </ul>
        </div>
    </div>
</nav>

<div class="container mb-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2 class="fw-bold mb-1"><i class="fa-solid fa-file-lines text-primary me-2"></i>Proces-Verbaux de Soutenance</h2>
            <p class="text-muted mb-0">Telecharger les PVs generes depuis le template Word officiel.</p>
        </div>
        <c:if test="${not empty pvItems}">
            <a href="downloadPvZip.do" class="btn btn-success"
               onclick="return confirm('Voulez-vous telecharger tous les PVs au format ZIP ?')">
                <i class="fa-solid fa-file-zipper me-1"></i> Telecharger tous les PVs (ZIP)
            </a>
        </c:if>
    </div>

    <div class="row g-2 mb-4">
        <div class="col-md-4">
            <div class="card shadow-sm p-3 text-center">
                <small class="text-muted text-uppercase fw-bold">PVs disponibles</small>
                <span class="h2 fw-bold text-primary">${not empty totalPVs ? totalPVs : '0'}</span>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card shadow-sm p-3 text-center">
                <small class="text-muted text-uppercase fw-bold">Professeurs</small>
                <span class="h2 fw-bold text-success">${professorGroups.size()}</span>
            </div>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty pvItems}">
            <div class="alert alert-warning text-center">
                <i class="fa-solid fa-triangle-exclamation me-2"></i>
                <strong>Aucune soutenance planifiee.</strong>
                Veuillez d'abord generer le planning.
                <br><br>
                <a href="planning.do" class="btn btn-warning">
                    <i class="fa-solid fa-calendar-days me-1"></i> Aller au Planning
                </a>
            </div>
        </c:when>

        <c:otherwise>
            <div class="row g-4">
                <div class="col-lg-4">
                    <div class="card shadow-sm">
                        <div class="card-header bg-white">
                            <h5 class="mb-0 fw-bold">Professeurs</h5>
                            <div class="input-group input-group-sm mt-2">
                                <span class="input-group-text bg-light border-end-0"><i class="fa-solid fa-search text-muted"></i></span>
                                <input type="text" id="searchProf" class="form-control border-start-0 ps-0" placeholder="Chercher un professeur...">
                            </div>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-hover mb-0 align-middle" id="profTable">
                                <thead class="table-light">
                                    <tr>
                                        <th>Professeur</th>
                                        <th class="text-end">PVs</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="group" items="${professorGroups}">
                                        <tr class="${selectedProfessorId == group.professorId ? 'table-primary' : ''}">
                                            <td>
                                                <a href="pv.do?profId=${group.professorId}" class="text-decoration-none fw-semibold">
                                                    ${group.professorName}
                                                </a>
                                            </td>
                                            <td class="text-end">
                                                <span class="badge bg-primary">${group.pvs.size()}</span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="col-lg-8">
                    <div class="card shadow-sm">
                        <div class="card-header bg-white d-flex justify-content-between align-items-center">
                            <div>
                                <h5 class="mb-0 fw-bold">
                                    PVs de ${selectedProfessorGroup.professorName}
                                </h5>
                            </div>
                            <c:if test="${not empty selectedProfessorGroup}">
                                <a href="downloadPvZip.do?profId=${selectedProfessorGroup.professorId}" class="btn btn-sm btn-outline-danger"
                                   onclick="return confirm('Voulez-vous telecharger les PVs de ce professeur au format ZIP ?')">
                                    <i class="fa-solid fa-file-zipper me-1"></i> ZIP de ce professeur
                                </a>
                            </c:if>
                        </div>

                        <div class="card-body">
                            <c:choose>
                                <c:when test="${empty selectedProfessorGroup}">
                                    <div class="alert alert-info mb-0">Selectionnez un professeur pour afficher ses PVs.</div>
                                </c:when>
                                <c:otherwise>
                                    <div class="table-responsive">
                                        <table class="table table-hover align-middle">
                                            <thead class="table-light">
                                                <tr>
                                                    <th>Etudiant</th>
                                                    <th>Filiere</th>
                                                    <th>Date</th>
                                                    <th>Heure</th>
                                                    <th>Salle</th>
                                                    <th class="text-end">Action</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <c:forEach var="pv" items="${selectedProfessorGroup.pvs}">
                                                    <tr>
                                                        <td>
                                                            <strong>${pv.studentName}</strong>
                                                            <div class="small text-muted">${pv.fileName}</div>
                                                        </td>
                                                        <td>${pv.filiere}</td>
                                                        <td>${pv.date}</td>
                                                        <td>${pv.heure}</td>
                                                        <td>${pv.salle}</td>
                                                        <td class="text-end">
                                                            <a href="downloadPvDocx.do?pvId=${pv.id}" class="btn btn-sm btn-outline-primary"
                                                               onclick="return confirm('Voulez-vous telecharger ce PV au format Word ?')">
                                                                <i class="fa-solid fa-download me-1"></i> DOCX
                                                            </a>
                                                        </td>
                                                    </tr>
                                                </c:forEach>
                                            </tbody>
                                        </table>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const searchInput = document.getElementById('searchProf');
        if (searchInput) {
            searchInput.addEventListener('keyup', function() {
                let filter = this.value.toLowerCase();
                let rows = document.querySelectorAll('#profTable tbody tr');
                
                rows.forEach(row => {
                    let text = row.querySelector('td:first-child').textContent.toLowerCase();
                    row.style.display = text.includes(filter) ? '' : 'none';
                });
            });
        }
    });
</script>
</body>
</html>
