<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt" %>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Planning des Soutenances PFE</title>
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">

    <style>
        body {
            background-color: #f8f9fa;
            font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .navbar-brand { font-weight: 700; letter-spacing: -0.5px; }
        .card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.04);
            margin-bottom: 1.5rem;
        }
        .config-section h6 { font-size: 0.85rem; text-transform: uppercase; letter-spacing: .04em; color: #475569; }
        .constraint-row { border-bottom: 1px solid #f1f5f9; padding: 8px 0; }
        .constraint-row:last-child { border-bottom: none; }
        .priority-toggle .btn-check:checked + .btn-outline-danger { background: #dc3545; color: white; }
        .priority-toggle .btn-check:checked + .btn-outline-warning { background: #ffc107; color: #0f172a; }

        .reco-info    { border-left: 4px solid #0dcaf0; }
        .reco-warning { border-left: 4px solid #ffc107; }
        .reco-danger  { border-left: 4px solid #dc3545; }

        .salle-row td { vertical-align: middle; }

        @media (max-width: 992px) {
            .sticky-config { position: static !important; top: auto !important; }
        }
        @media (max-width: 576px) {
            .container { padding-left: 12px; padding-right: 12px; }
            h2 { font-size: 1.4rem; }
        }
    </style>
</head>
<body>

<!-- Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm mb-4">
    <div class="container-fluid px-3 px-lg-4">
        <a class="navbar-brand d-flex align-items-center" href="index.jsp">
            <c:choose>
                <c:when test="${not empty appSettings and appSettings.hasLogo()}">
                    <img src="logo.do" alt="Logo" style="height: 38px; margin-right: 12px; object-fit: contain;">
                </c:when>
                <c:otherwise>
                    <i class="fa-solid fa-graduation-cap me-2 text-primary" style="font-size: 28px;"></i>
                </c:otherwise>
            </c:choose>
            <c:out value="${empty appSettings.institutionName ? 'Gestion PFE' : appSettings.institutionName}"/>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="nav">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link" href="affectation.do"><i class="fa-solid fa-users me-1"></i>Affectation</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="config.do"><i class="fa-solid fa-sliders me-1"></i>Configuration</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link active" href="planning.do"><i class="fa-solid fa-calendar-days me-1"></i>Planning</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="pv.do"><i class="fa-solid fa-file-lines me-1"></i>PVs</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="dashboard.do"><i class="fa-solid fa-chart-pie me-1"></i>Dashboard</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="settings.do"><i class="fa-solid fa-gear me-1"></i>Paramètres</a>
                </li>
            </ul>
        </div>
    </div>
</nav>

<div class="container py-3 py-md-4">

    <!-- Header -->
    <div class="row mb-4 align-items-center">
        <div class="col">
            <h2 class="fw-bold mb-1"><i class="fa-solid fa-calendar-check text-primary me-2"></i> Planning des Soutenances</h2>
            <p class="text-muted mb-0 d-none d-md-block">Configurez, générez et téléchargez le planning des soutenances PFE.</p>
        </div>
    </div>

    <!-- ============================================================
         FAILED PLANNING ALERT (HARD constraint violations)
         ============================================================ -->
    <c:if test="${planningFailed == true}">
        <div class="card border-danger mb-4 reco-danger">
            <div class="card-body">
                <h5 class="card-title text-danger fw-bold mb-3">
                    <i class="fa-solid fa-circle-xmark me-2"></i>Le planning n'a pas pu être généré
                </h5>
                <p class="text-muted">
                    Certaines contraintes <strong>dures</strong> ne peuvent pas être respectées avec la configuration actuelle.
                    Aucune donnée n'a été enregistrée. Modifiez les paramètres ci-dessous puis relancez.
                </p>

                <c:if test="${not empty planningHardViolations}">
                    <h6 class="fw-bold mt-3">Contraintes violées</h6>
                    <ul class="list-group list-group-flush mb-3">
                        <c:forEach var="v" items="${planningHardViolations}">
                            <li class="list-group-item">
                                <span class="badge bg-${v.bootstrapClass} me-2"><i class="fa-solid ${v.iconClass}"></i> ${v.severity}</span>
                                <strong>${v.constraintLabel}</strong>
                                <div class="small text-muted mt-1">${v.detail}</div>
                                <c:if test="${not empty v.suggestion}">
                                    <div class="small text-success mt-1"><i class="fa-solid fa-lightbulb me-1"></i>${v.suggestion}</div>
                                </c:if>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>

                <c:if test="${not empty planningUnscheduled}">
                    <h6 class="fw-bold mt-3">Projets non planifiables</h6>
                    <div class="alert alert-warning small mb-3">
                        ${planningUnscheduled.size()} projet(s) n'ont pas pu trouver de créneau valide :
                        <c:forEach var="p" items="${planningUnscheduled}" varStatus="st">
                            ${p}<c:if test="${!st.last}">, </c:if>
                        </c:forEach>
                    </div>
                </c:if>

                <c:if test="${not empty planningSuggestions}">
                    <h6 class="fw-bold mt-3">Suggestions</h6>
                    <c:forEach var="s" items="${planningSuggestions}">
                        <div class="alert alert-${s.bootstrapClass} small mb-2">
                            <i class="fa-solid ${s.iconClass} me-1"></i>
                            <strong>${s.title}</strong> — ${s.message}
                            <c:if test="${not empty s.suggestion}">
                                <div class="mt-1"><i class="fa-solid fa-lightbulb me-1"></i>${s.suggestion}</div>
                            </c:if>
                        </div>
                    </c:forEach>
                </c:if>
            </div>
        </div>
    </c:if>

    <!-- ============================================================
         SUCCESSFUL PLANNING — soft warnings if any
         ============================================================ -->
    <c:if test="${planningFailed != true && not empty planningSoftViolations}">
        <div class="card border-warning mb-4 reco-warning">
            <div class="card-body">
                <h5 class="card-title text-warning fw-bold mb-3">
                    <i class="fa-solid fa-triangle-exclamation me-2"></i>Avertissements (contraintes souples)
                </h5>
                <ul class="list-group list-group-flush">
                    <c:forEach var="v" items="${planningSoftViolations}">
                        <li class="list-group-item">
                            <span class="badge bg-${v.bootstrapClass} me-2">${v.severity}</span>
                            <strong>${v.constraintLabel}</strong>
                            <div class="small text-muted mt-1">${v.detail}</div>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </c:if>

    <div class="row g-4">

        <!-- ============================================================
             LEFT COLUMN — Generated planning + history
             ============================================================ -->
        <div class="col-12 col-lg-7">
            <div class="card p-3 p-md-4">
                <c:choose>
                    <c:when test="${not empty soutenances}">
                        <div class="row mb-3">
                            <div class="col-md-7 mb-3 mb-md-0">
                                <h6 class="text-muted mb-2"><i class="fa-solid fa-chalkboard-user me-2"></i>Encadrants / jurys</h6>
                                <div class="d-flex flex-wrap gap-1">
                                    <c:forEach var="entry" items="${profLegend}">
                                        <span class="badge fw-normal px-2 py-1" style="background-color:#${entry.value}; color:white; font-size:0.75rem;">${entry.key}</span>
                                    </c:forEach>
                                </div>
                            </div>
                            <div class="col-md-5">
                                <h6 class="text-muted mb-2"><i class="fa-solid fa-graduation-cap me-2"></i>Filières</h6>
                                <div class="d-flex flex-wrap gap-1">
                                    <c:forEach var="entry" items="${filiereLegend}">
                                        <span class="badge fw-normal px-2 py-1 text-white" style="background-color:#${entry.value};">${entry.key}</span>
                                    </c:forEach>
                                </div>
                            </div>
                        </div>

                        <p class="text-muted small mb-3 border-top pt-3">
                            <i class="fa-solid fa-circle-check text-success me-1"></i>
                            <strong>${soutenances.size()}</strong> soutenances planifiées (planning actif).
                        </p>

                        <div class="d-flex gap-2 flex-wrap">
                            <form action="planningPdf.do" method="post" class="d-inline">
                                <button type="submit" class="btn btn-danger">
                                    <i class="fa-solid fa-file-pdf me-1"></i>PDF
                                </button>
                            </form>
                            <form action="planningJurySujetPdf.do" method="post" class="d-inline">
                                <button type="submit" class="btn btn-outline-danger">
                                    <i class="fa-solid fa-brain me-1"></i>Jury + Sujet
                                </button>
                            </form>
                            <form action="planningDocx.do" method="post" class="d-inline">
                                <button type="submit" class="btn btn-outline-primary">
                                    <i class="fa-solid fa-file-word me-1"></i>Word
                                </button>
                            </form>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="text-center py-5">
                            <i class="fa-regular fa-calendar-xmark fa-3x text-muted mb-3"></i>
                            <h5 class="mb-2">Aucun planning généré</h5>
                            <p class="text-muted mb-0 small">Configurez les paramètres à droite puis cliquez sur <em>Générer</em>.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Historique -->
            <div class="card p-3 p-md-4">
                <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
                    <h5 class="fw-bold mb-0"><i class="fa-solid fa-clock-rotate-left text-primary me-2"></i>Historique</h5>
                    <c:if test="${not empty historyFiles}">
                        <a href="clearHistory.do?type=planning" class="btn btn-sm btn-outline-danger" onclick="return confirm('Supprimer TOUT l\'historique ?')">
                            <i class="fa-solid fa-trash-can me-1"></i>Vider
                        </a>
                    </c:if>
                </div>
                <c:choose>
                    <c:when test="${not empty historyFiles}">
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <tbody>
                                    <c:forEach var="file" items="${historyFiles}">
                                        <tr>
                                            <td class="small">
                                                <i class="fa-solid ${file.endsWith('.pdf') ? 'fa-file-pdf text-danger' : 'fa-file-word text-primary'} me-2"></i>
                                                ${file}
                                            </td>
                                            <td class="text-end">
                                                <a href="downloadHistory.do?file=${file}" class="btn btn-sm btn-outline-secondary">
                                                    <i class="fa-solid fa-download"></i>
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

           <!-- ============================================================
             RIGHT COLUMN — Configuration Summary + Salles
             ============================================================ -->
        <div class="col-12 col-lg-5">
            <div class="card border-0 shadow-sm overflow-hidden mb-4 sticky-config" style="position: sticky; top: 16px;">
                <!-- Card Header -->
                <div class="bg-dark text-white p-4">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h5 class="fw-bold mb-0"><i class="fa-solid fa-sliders text-white-50 me-2"></i>Configuration Active</h5>
                    </div>
                    <p class="small text-white-50 mb-0">Résumé des paramètres appliqués pour le planning actuel.</p>
                </div>

                <!-- Card Body -->
                <div class="card-body p-4 bg-white">
                    <div class="row g-3 mb-4">
                        <div class="col-6">
                            <div class="d-flex align-items-center mb-1">
                                <i class="fa-regular fa-calendar text-primary me-2"></i>
                                <span class="small text-muted fw-semibold text-uppercase">Dates</span>
                            </div>
                            <div class="fw-bold text-dark fs-6">Du ${planningConfig.startDate}</div>
                            <div class="small text-muted">Durée : ${planningConfig.numberOfDays} jours</div>
                        </div>
                        <div class="col-6">
                            <div class="d-flex align-items-center mb-1">
                                <i class="fa-regular fa-clock text-primary me-2"></i>
                                <span class="small text-muted fw-semibold text-uppercase">Créneaux</span>
                            </div>
                            <div class="fw-bold text-dark fs-6">${planningConfig.slotsPerDay} par jour</div>
                            <div class="small text-muted">${planningConfig.soutenanceDurationMinutes} min / soutenance</div>
                        </div>
                    </div>

                    <div class="mb-4">
                        <div class="d-flex align-items-center mb-2">
                            <i class="fa-solid fa-shield-halved text-primary me-2"></i>
                            <span class="small text-muted fw-semibold text-uppercase">Contraintes & Règles</span>
                        </div>
                        <ul class="list-unstyled mb-0 small text-dark">
                            <li class="mb-2"><i class="fa-solid fa-check text-success me-2"></i>${constraints.size()} règles actives</li>
                            <li class="mb-2"><i class="fa-solid fa-check text-success me-2"></i>Équilibre des jurys</li>
                            <li class="mb-1"><i class="fa-solid fa-check text-success me-2"></i>Disponibilités respectées</li>
                        </ul>
                    </div>

                    <div class="mb-4">
                        <div class="d-flex justify-content-between align-items-end mb-2">
                            <div class="d-flex align-items-center">
                                <i class="fa-solid fa-door-open text-primary me-2"></i>
                                <span class="small text-muted fw-semibold text-uppercase">Salles autorisées</span>
                            </div>
                            <span class="badge bg-primary rounded-pill">${salles.size()}</span>
                        </div>
                        <div class="d-flex flex-wrap gap-1">
                            <c:forEach var="salle" items="${salles}" varStatus="status">
                                <c:if test="${status.index < 8}">
                                    <span class="badge bg-light text-dark border">${salle.num_salle}</span>
                                </c:if>
                            </c:forEach>
                            <c:if test="${salles.size() > 8}">
                                <span class="badge bg-light text-secondary border">+${salles.size() - 8}</span>
                            </c:if>
                        </div>
                    </div>

                    <hr class="my-4" style="border-color: #f1f5f9;">

                    <!-- Action Button -->
                    <div class="d-grid gap-2">
                        <a href="config.do" class="btn btn-dark btn-lg">
                            <i class="fa-solid fa-pen-to-square me-2"></i>Modifier la configuration
                        </a>
                        <form action="lancerPlanning.do" method="post" id="planningForm" onsubmit="return confirm('Générer un nouveau planning avec cette configuration ? L\'ancien sera écrasé.');">
                            <!-- Hidden inputs to submit the generation with all existing salles -->
                            <div class="d-none">
                                <c:forEach var="salle" items="${salles}">
                                    <input type="checkbox" name="selectedSalles" value="${salle.id_salle}" checked>
                                </c:forEach>
                                <input type="hidden" name="numberOfRooms" value="${salles.size()}">
                            </div>
                            <button type="submit" class="btn btn-outline-primary btn-lg w-100 mt-2" ${!hasAffectations ? 'disabled' : ''}>
                                <i class="fa-solid fa-rocket me-2"></i>
                                <c:choose>
                                    <c:when test="${not empty soutenances}">Régénérer le planning</c:when>
                                    <c:otherwise>Générer le planning</c:otherwise>
                                </c:choose>
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    document.addEventListener('DOMContentLoaded', function () {
        // ── Salle select all / unselect all ──────────────────────────────────
        var selectAllBtn = document.getElementById('selectAllSalles');
        var unselectAllBtn = document.getElementById('unselectAllSalles');
        var salleCheckboxes = document.querySelectorAll('input[name="selectedSalles"]');

        if (selectAllBtn) {
            selectAllBtn.addEventListener('click', function () {
                salleCheckboxes.forEach(function (cb) { cb.checked = true; });
            });
        }
        if (unselectAllBtn) {
            unselectAllBtn.addEventListener('click', function () {
                salleCheckboxes.forEach(function (cb) { cb.checked = false; });
            });
        }

        // ── Recommendations: refresh on input change ─────────────────────────
        var panel = document.getElementById('recommendations-panel');
        var inputs = document.querySelectorAll('.cfg-input');
        var slotsDisplay = document.getElementById('slots-per-day-display');
        var roomsField = document.getElementById('numberOfRooms');

        function refreshRecommendations() {
            if (!panel) return;
            var form = document.getElementById('planningForm');
            if (!form) return;

            // Build payload from cfg inputs
            var fd = new URLSearchParams();
            inputs.forEach(function (input) {
                if (input.name) fd.append(input.name, input.value);
            });
            var checkedSalles = document.querySelectorAll('input[name="selectedSalles"]:checked').length;
            if (roomsField) roomsField.value = checkedSalles;
            fd.append('numberOfRooms', checkedSalles);

            fetch('recommendations.do', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: fd.toString()
            })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (data) {
                if (!data) return;
                if (slotsDisplay) slotsDisplay.textContent = data.slotsPerDay;

                if (!data.recommendations || data.recommendations.length === 0) {
                    panel.innerHTML = '';
                    return;
                }
                var html = '';
                data.recommendations.forEach(function (r) {
                    html += '<div class="alert alert-' + r.bootstrap + ' small reco-' + r.bootstrap + ' mb-2">'
                          + '<i class="fa-solid ' + r.icon + ' me-1"></i>'
                          + '<strong>' + r.title + '</strong> &mdash; ' + r.message;
                    if (r.suggestion) {
                        html += '<div class="mt-1"><i class="fa-solid fa-lightbulb me-1"></i>' + r.suggestion + '</div>';
                    }
                    html += '</div>';
                });
                panel.innerHTML = html;
            })
            .catch(function () { /* ignore */ });
        }

        var debounceTimer = null;
        function debouncedRefresh() {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(refreshRecommendations, 250);
        }

        inputs.forEach(function (input) {
            input.addEventListener('input', debouncedRefresh);
            input.addEventListener('change', debouncedRefresh);
        });
        salleCheckboxes.forEach(function (cb) { cb.addEventListener('change', debouncedRefresh); });
        if (selectAllBtn) selectAllBtn.addEventListener('click', debouncedRefresh);
        if (unselectAllBtn) unselectAllBtn.addEventListener('click', debouncedRefresh);

        // Initial load
        refreshRecommendations();
    });
</script>

</body>
</html>
