<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt" %>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Planning des Soutenances PFE</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- FontAwesome -->
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
            <p class="text-muted mb-0">Générer et télécharger le planning des soutenances PFE.</p>
        </div>
    </div>

    <!-- Main Card -->
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
                            <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#CFE2FF;">GI</span>
                            <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#FFF3CD;">ID</span>
                            <span class="badge fw-normal px-2 py-1 text-dark" style="background-color:#D9EAD3;">TDIA</span>
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
                    <i class="fa-solid fa-info-circle me-1"></i>
                    <strong>${soutenances.size()}</strong> soutenances planifiées. Téléchargez le fichier PDF ou Word pour voir le détail complet du planning.
                </p>

                <!-- Boutons télécharger -->
                <div class="d-flex gap-3 flex-wrap">
                    <form action="planningPdf.do" method="post">
                        <button type="submit" class="btn btn-danger px-4 py-2">
                            <i class="fa-solid fa-file-pdf me-2"></i>Télécharger PDF
                        </button>
                    </form>
                    <form action="planningDocx.do" method="post">
                        <button type="submit" class="btn btn-outline-primary px-4 py-2">
                            <i class="fa-solid fa-file-word me-2"></i>Télécharger Word
                        </button>
                    </form>
                    <form action="lancerPlanning.do" method="post" class="ms-auto"
                          onsubmit="return confirm('Régénérer le planning ? L\'ancien sera remplacé.')">
                        <button type="submit" class="btn btn-outline-secondary px-4 py-2">
                            <i class="fa-solid fa-rotate me-2"></i>Régénérer
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
                        Cliquez sur le bouton ci-dessous pour lancer l'algorithme d'affectation des jurys, salles et créneaux horaires.
                    </p>
                    <form action="lancerPlanning.do" method="post"
                          onsubmit="return confirm('Générer le planning ?')">
                        <button type="submit" class="btn btn-primary px-4 py-2" ${!hasAffectations ? 'disabled' : ''}>
                            <i class="fa-solid fa-wand-magic-sparkles me-2"></i>Générer le Planning
                        </button>
                    </form>
                    <c:if test="${not hasAffectations}">
                        <div class="alert alert-warning mt-4 d-inline-block text-start">
                            <i class="fa-solid fa-triangle-exclamation me-2"></i>
                            <strong>Attention :</strong> Veuillez d'abord réaliser l'affectation avant de générer le planning.
                        </div>
                    </c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- Log de génération -->
    <c:if test="${not empty debug}">
        <div class="card p-3 bg-light">
            <h6 class="text-muted mb-3"><i class="fa-solid fa-terminal me-2"></i>Journal de génération</h6>
            <div style="max-height: 200px; overflow-y: auto; font-family: monospace; font-size: 0.85rem;" class="text-secondary">
                <c:forEach var="d" items="${debug}">
                    <div>${d}</div>
                </c:forEach>
            </div>
        </div>
    </c:if>

</div>

<!-- Bootstrap JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

</body>
</html>
