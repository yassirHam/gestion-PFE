<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestion des PFE | ENSAH</title>
    
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css">
    <link href="https://fonts.googleapis.com/css2?family=Segoe+UI:wght@300;400;600&display=swap" rel="stylesheet">
    
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f4f7f6;
        }
        .navbar {
            background: linear-gradient(90deg, #0d6efd 0%, #003d99 100%);
        }
        .card-custom {
            border: none;
            border-radius: 15px;
            transition: all 0.3s ease;
            background: white;
        }
        .card-custom:hover {
            transform: translateY(-10px);
            box-shadow: 0 15px 30px rgba(0,0,0,0.1);
        }
        .icon-box {
            width: 70px;
            height: 70px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 20px;
        }
        .stat-card {
            border-left: 5px solid #0d6efd;
        }
    </style>
</head>
<body>

    <nav class="navbar navbar-dark shadow-sm mb-5">
        <div class="container">
            <a class="navbar-brand fw-bold" href="#">
                <i class="bi bi-mortarboard-fill me-2"></i> GESTION DES PFE
            </a>
            <span class="navbar-text text-white d-none d-md-block">
                Année Universitaire 2023-2024
            </span>
        </div>
    </nav>

    <div class="container">
        <div class="row mb-5">
            <div class="col-12 text-center">
                <h1 class="display-5 fw-bold text-dark">Tableau de Bord</h1>
                <p class="lead text-muted">Pilotez les affectations, les plannings et la génération des documents officiels.</p>
            </div>
        </div>

        <div class="row g-4 mb-5">
            <div class="col-md-4">
                <div class="card card-custom text-center p-4 h-100">
                    <div class="icon-box bg-primary bg-opacity-10 text-primary">
                        <i class="bi bi-person-plus-fill fs-1"></i>
                    </div>
                    <h3>Affectation</h3>
                    <p class="text-muted small">Distribution automatique des étudiants aux encadrants (Quota: 3-4).</p>
                    <a href="AffectationServlet" class="btn btn-primary mt-auto rounded-pill">
                        Lancer l'affectation
                    </a>
                </div>
            </div>

            <div class="col-md-4">
                <div class="card card-custom text-center p-4 h-100">
                    <div class="icon-box bg-success bg-opacity-10 text-success">
                        <i class="bi bi-calendar-event fs-1"></i>
                    </div>
                    <h3>Planning</h3>
                    <p class="text-muted small">Génération des soutenances et gestion des salles/horaires.</p>
                    <a href="PlanningServlet" class="btn btn-success mt-auto rounded-pill">
                        Gérer le planning
                    </a>
                </div>
            </div>

            <div class="col-md-4">
                <div class="card card-custom text-center p-4 h-100">
                    <div class="icon-box bg-danger bg-opacity-10 text-danger">
                        <i class="bi bi-file-earmark-word-fill fs-1"></i>
                    </div>
                    <h3>Générer PVs</h3>
                    <p class="text-muted small">Exportation massive des fiches d'évaluation (Word/PDF).</p>
                    <a href="PVServlet" class="btn btn-danger mt-auto rounded-pill">
                        Exporter les PVs
                    </a>
                </div>
            </div>
        </div>

        <div class="card card-custom shadow-sm p-4 mb-5">
            <h4 class="mb-4 fw-bold"><i class="bi bi-bar-chart-fill me-2"></i> Statistiques Clés</h4>
            <div class="row g-3">
                <div class="col-md-3">
                    <div class="p-3 border rounded bg-light stat-card">
                        <small class="text-muted d-block text-uppercase fw-bold">Étudiants</small>
                        <span class="h3 fw-bold">${not empty totalEtudiants ? totalEtudiants : '0'}</span>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="p-3 border rounded bg-light stat-card" style="border-left-color: #198754;">
                        <small class="text-muted d-block text-uppercase fw-bold">Professeurs</small>
                        <span class="h3 fw-bold">${not empty totalProfs ? totalProfs : '0'}</span>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="p-3 border rounded bg-light stat-card" style="border-left-color: #ffc107;">
                        <small class="text-muted d-block text-uppercase fw-bold">Soutenances</small>
                        <span class="h3 fw-bold">${not empty totalSoutenances ? totalSoutenances : '0'}</span>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="p-3 border rounded bg-light stat-card" style="border-left-color: #dc3545;">
                        <small class="text-muted d-block text-uppercase fw-bold">Conformité</small>
                        <div>
                            <c:choose>
                                <c:when test="${hasAlerts}">
                                    <span class="badge bg-danger"><i class="bi bi-exclamation-triangle-fill"></i> Anomalie</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge bg-success"><i class="bi bi-check-circle-fill"></i> Conforme</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <footer class="text-center text-muted py-4">
            <small>&copy; 2024 ENSAH - Projet JEE Gestion des PFE - Réalisé avec Hibernate & MVC2</small>
        </footer>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/bootstrap.bundle.min.js"></script>
</body>
</html>