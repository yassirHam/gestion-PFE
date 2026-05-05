<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt" %>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Planning des Soutenances PFE</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    
    <style>
        body {
            background-color: #f8f9fa;
            font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        
        .navbar-brand {
            font-weight: 700;
            letter-spacing: -0.5px;
        }
        
        .card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.04);
            margin-bottom: 1.5rem;
        }
    </style>
</head>
<body>

<!-- Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm mb-4">
    <div class="container-fluid px-4">
        <a class="navbar-brand" href="index.jsp">
            <i class="fa-solid fa-graduation-cap me-2"></i>Gestion PFE
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="nav">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link" href="affectation.do">
                        <i class="fa-solid fa-users me-1"></i>Affectation
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link active" href="planning.do">
                        <i class="fa-solid fa-calendar-days me-1"></i>Planning
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="dashboard.do">
                        <i class="fa-solid fa-chart-pie me-1"></i>Dashboard
                    </a>
                </li>
            </ul>
        </div>
    </div>
</nav>

<div class="container py-4">

    <!-- Header -->
    <div class="row mb-4 align-items-center">
        <div class="col">
            <h2 class="fw-bold mb-1"><i class="fa-solid fa-calendar-check text-primary me-2"></i> Planning des Soutenances</h2>
            <p class="text-muted mb-0">Générer, configurer et télécharger le planning des soutenances PFE.</p>
        </div>
    </div>

    <div class="row">
        <div class="col-lg-8">
            <!-- Main Status Card -->
            <div class="card p-4">
                <c:choose>
                    <c:when test="${not empty soutenances}">
                        <div class="row mb-4">
                            <!-- Légende des Professeurs -->
                            <div class="col-md-7 mb-3 mb-md-0">
                                <h6 class="text-muted mb-3"><i class="fa-solid fa-chalkboard-user me-2"></i>Légende des encadrants / jurys</h6>
                                <div class="d-flex flex-wrap gap-2">
                                    <c:forEach var="entry" items="${profLegend}">
                                        <span class="badge fw-normal px-2 py-1" style="background-color:#${entry.value}; color:white; font-size:0.8rem;">${entry.key}</span>
                                    </c:forEach>
                                </div>
                            </div>
                            
                            <!-- Légende des Filières & Créneaux -->
                            <div class="col-md-5">
                                <h6 class="text-muted mb-3"><i class="fa-solid fa-graduation-cap me-2"></i>Légende des filières</h6>
                                <div class="d-flex flex-wrap gap-2 mb-3">
                                    <c:forEach var="entry" items="${filiereLegend}">
                                        <span class="badge fw-normal px-2 py-1 text-white" style="background-color:#${entry.value};">${entry.key}</span>
                                    </c:forEach>
                                </div>
                                
                                <h6 class="text-muted mb-2"><i class="fa-solid fa-clock me-2"></i>Couleurs des heures</h6>
                                <div class="d-flex flex-wrap gap-2">
                                    <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#CFE2FF;">09h00</span>
                                    <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#D9EAD3;">10h00</span>
                                    <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#FFF3CD;">11h00</span>
                                    <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#F8D7DA;">14h00</span>
                                    <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#E2D9F3;">15h00</span>
                                    <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#FFE5CC;">16h00</span>
                                </div>
                            </div>
                        </div>

                        <p class="text-muted small mb-4 border-top pt-3">
                            <i class="fa-solid fa-circle-check text-success me-1"></i>
                            <strong>${soutenances.size()}</strong> soutenances planifiées (Planning actif).
                        </p>

                        <!-- Boutons télécharger actuels -->
                        <div class="d-flex gap-3 flex-wrap">
                            <form action="planningPdf.do" method="post">
                                <button type="submit" class="btn btn-danger px-4 py-2">
                                    <i class="fa-solid fa-file-pdf me-2"></i>Télécharger le PDF Actuel
                                </button>
                            </form>
                            <form action="planningDocx.do" method="post">
                                <button type="submit" class="btn btn-outline-primary px-4 py-2">
                                    <i class="fa-solid fa-file-word me-2"></i>Télécharger le Word Actuel
                                </button>
                            </form>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <!-- Aucun planning -->
                        <div class="text-center py-5">
                            <i class="fa-regular fa-calendar-xmark fa-4x text-muted mb-3"></i>
                            <h5 class="mb-2">Aucun planning généré</h5>
                            <p class="text-muted mb-4">
                                Utilisez la configuration à droite pour lancer l'algorithme d'affectation.
                            </p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            
            <!-- Historique des plannings -->
            <div class="card p-4">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h5 class="fw-bold mb-0"><i class="fa-solid fa-clock-rotate-left text-primary me-2"></i> Historique des plannings</h5>
                    <c:if test="${not empty historyFiles}">
                        <a href="clearHistory.do?type=planning" class="btn btn-sm btn-outline-danger" onclick="return confirm('Supprimer TOUT l\'historique des plannings ?')">
                            <i class="fa-solid fa-trash-can me-1"></i> Vider l'historique
                        </a>
                    </c:if>
                </div>
                <c:choose>
                    <c:when test="${not empty historyFiles}">
                        <div class="table-responsive">
                            <table class="table table-hover align-middle">
                                <tbody>
                                    <c:forEach var="file" items="${historyFiles}">
                                        <tr>
                                            <td>
                                                <i class="fa-solid ${file.endsWith('.pdf') ? 'fa-file-pdf text-danger' : 'fa-file-word text-primary'} me-2"></i>
                                                ${file}
                                            </td>
                                            <td class="text-end">
                                                <a href="downloadHistory.do?file=${file}" class="btn btn-sm btn-outline-secondary">
                                                    <i class="fa-solid fa-download"></i> Télécharger
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <p class="text-muted small mb-0">Aucun historique disponible.</p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <div class="col-lg-4">
            <!-- Configuration & Génération -->
            <div class="card p-4 sticky-top" style="top: 20px;">
                <h5 class="fw-bold mb-3"> Configuration</h5>
                
                <c:if test="${not hasAffectations}">
                    <div class="alert alert-warning small">
                        <i class="fa-solid fa-triangle-exclamation me-1"></i>
                        Veuillez réaliser l'affectation avant de générer le planning.
                    </div>
                </c:if>

                <form action="addSalle.do" method="post" class="mb-4">
                    <label class="form-label small fw-semibold text-muted">Ajouter une nouvelle salle</label>
                    <div class="input-group">
                        <input type="text" class="form-control form-control-sm" name="numSalle" placeholder="Ex: S12A" required>
                        <button class="btn btn-outline-success btn-sm" type="submit"><i class="fa-solid fa-plus"></i></button>
                    </div>
                </form>

                <form action="lancerPlanning.do" method="post" onsubmit="return confirm('Générer un nouveau planning ? L\'ancien sera écrasé.');">
                    <label class="form-label small fw-semibold text-muted">Salles à inclure</label>
                    <div class="border rounded p-3 mb-4" style="max-height: 200px; overflow-y: auto;">
                        <c:forEach var="salle" items="${salles}">
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" name="selectedSalles" value="${salle.id_salle}" id="salle_${salle.id_salle}" checked>
                                <label class="form-check-label small" for="salle_${salle.id_salle}">
                                    ${salle.num_salle}
                                </label>
                            </div>
                        </c:forEach>
                        <c:if test="${empty salles}">
                            <p class="text-muted small mb-0">Aucune salle disponible.</p>
                        </c:if>
                    </div>

                    <button type="submit" class="btn btn-primary w-100" ${!hasAffectations ? 'disabled' : ''}>
                        <c:choose>
                            <c:when test="${not empty soutenances}">Régénérer le Planning</c:when>
                            <c:otherwise>Générer le Planning</c:otherwise>
                        </c:choose>
                    </button>
                </form>
            </div>
        </div>
    </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

</body>
</html>
