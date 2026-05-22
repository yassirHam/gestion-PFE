<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    // Make sure branding is available even when index.jsp is opened directly (no servlet hit)
    if (request.getAttribute("appSettings") == null) {
        try {
            request.setAttribute("appSettings", services.AppSettingsService.getInstance().get());
        } catch (Exception ignored) {}
    }
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Gestion PFE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <style>
        body {
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            min-height: 100vh;
            font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .home-card {
            border: none;
            border-radius: 16px;
            box-shadow: 0 6px 14px rgba(0,0,0,0.08);
            transition: transform 0.2s, box-shadow 0.2s;
            overflow: hidden;
        }
        .home-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 22px rgba(0,0,0,0.12);
        }
        .home-card .card-body {
            padding: 1.5rem;
        }
        .home-card .icon {
            font-size: 2rem;
            margin-bottom: 0.5rem;
        }
        @media (max-width: 576px) {
            h1 { font-size: 1.6rem; }
        }
    </style>
</head>
<body>
<c:set var="activeTab" value="index" scope="request" />
<div class="d-flex flex-nowrap" style="min-height: 100vh;">
    <jsp:include page="sidebar.jsp" />
    <div class="main-content-wrapper flex-grow-1 p-4">

    <div class="text-center mb-4 mb-md-5">
        <h1 class="fw-bold mb-2">Gestion des PFE</h1>
        <p class="text-muted mb-0">
            Affectation, planning, PVs et tableau de bord
            <c:if test="${not empty appSettings.institutionName}">
                &mdash; <c:out value="${appSettings.institutionName}"/>
            </c:if>
        </p>
    </div>

    <div class="row g-3 g-md-4 justify-content-center">

        <div class="col-12 col-sm-6 col-lg-3">
            <a href="affectation.do" class="text-decoration-none">
                <div class="home-card text-center bg-white">
                    <div class="card-body">
                        <div class="icon text-primary"><i class="fa-solid fa-users"></i></div>
                        <h5 class="fw-bold mb-1">Affectation</h5>
                        <p class="small text-muted mb-0">Importer les listes et lancer l'affectation des encadrants.</p>
                    </div>
                </div>
            </a>
        </div>

        <div class="col-12 col-sm-6 col-lg-3">
            <a href="planning.do" class="text-decoration-none">
                <div class="home-card text-center bg-white">
                    <div class="card-body">
                        <div class="icon text-success"><i class="fa-solid fa-calendar-days"></i></div>
                        <h5 class="fw-bold mb-1">Planning</h5>
                        <p class="small text-muted mb-0">Configurer et générer le planning des soutenances.</p>
                    </div>
                </div>
            </a>
        </div>

        <div class="col-12 col-sm-6 col-lg-3">
            <a href="pv.do" class="text-decoration-none">
                <div class="home-card text-center bg-white">
                    <div class="card-body">
                        <div class="icon text-warning"><i class="fa-solid fa-file-lines"></i></div>
                        <h5 class="fw-bold mb-1">PVs</h5>
                        <p class="small text-muted mb-0">Générer les procès-verbaux des soutenances.</p>
                    </div>
                </div>
            </a>
        </div>

        <div class="col-12 col-sm-6 col-lg-3">
            <a href="dashboard.do" class="text-decoration-none">
                <div class="home-card text-center bg-white">
                    <div class="card-body">
                        <div class="icon text-info"><i class="fa-solid fa-chart-pie"></i></div>
                        <h5 class="fw-bold mb-1">Dashboard</h5>
                        <p class="small text-muted mb-0">Statistiques et vérifications.</p>
                    </div>
                </div>
            </a>
        </div>

        <div class="col-12 col-sm-6 col-lg-3">
            <a href="settings.do" class="text-decoration-none">
                <div class="home-card text-center bg-white">
                    <div class="card-body">
                        <div class="icon text-secondary"><i class="fa-solid fa-gear"></i></div>
                        <h5 class="fw-bold mb-1">Paramètres</h5>
                        <p class="small text-muted mb-0">Identité, stockage de l'historique, NLP optionnel.</p>
                    </div>
                </div>
            </a>
        </div>

    </div>

</div>    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
