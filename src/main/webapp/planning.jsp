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
<nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm mb-4">
    <div class="container-fluid px-3 px-lg-4">
        <a class="navbar-brand" href="index.jsp">
            <i class="fa-solid fa-graduation-cap me-2"></i>Gestion PFE
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
                    <a class="nav-link active" href="planning.do"><i class="fa-solid fa-calendar-days me-1"></i>Planning</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="pv.do"><i class="fa-solid fa-file-lines me-1"></i>PVs</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="dashboard.do"><i class="fa-solid fa-chart-pie me-1"></i>Dashboard</a>
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
             RIGHT COLUMN — Configuration + recommendations + salles
             ============================================================ -->
        <div class="col-12 col-lg-5">
            <div class="card p-3 p-md-4 sticky-config" style="position: sticky; top: 16px;">
                <h5 class="fw-bold mb-3"><i class="fa-solid fa-sliders me-2"></i>Configuration</h5>

                <c:if test="${not hasAffectations}">
                    <div class="alert alert-warning small">
                        <i class="fa-solid fa-triangle-exclamation me-1"></i>
                        Réalisez d'abord l'affectation des encadrants avant de générer le planning.
                    </div>
                </c:if>

                <!-- Recommendations panel -->
                <div id="recommendations-panel" class="mb-3"></div>

                <form action="lancerPlanning.do" method="post" id="planningForm"
                      onsubmit="return confirm('Générer un nouveau planning ? L\'ancien sera écrasé.');">

                    <!-- ── Time configuration ── -->
                    <div class="config-section mb-3">
                        <h6>Plage et durée</h6>

                        <div class="row g-2 mb-2">
                            <div class="col-6">
                                <label class="form-label small">Nombre de jours</label>
                                <input type="number" min="1" max="60" class="form-control form-control-sm cfg-input"
                                       name="numberOfDays" value="${planningConfig.numberOfDays}" required>
                            </div>
                            <div class="col-6">
                                <label class="form-label small">Date de début</label>
                                <input type="date" class="form-control form-control-sm cfg-input"
                                       name="startDate"
                                       value="${planningConfig.startDate}" required>
                            </div>
                        </div>

                        <div class="row g-2 mb-2">
                            <div class="col-6">
                                <label class="form-label small">Matin: début (h)</label>
                                <input type="number" min="6" max="23" class="form-control form-control-sm cfg-input"
                                       name="startHourMorning" value="${planningConfig.startHourMorning}">
                            </div>
                            <div class="col-6">
                                <label class="form-label small">Matin: fin (h)</label>
                                <input type="number" min="6" max="23" class="form-control form-control-sm cfg-input"
                                       name="endHourMorning" value="${planningConfig.endHourMorning}">
                            </div>
                        </div>

                        <div class="row g-2 mb-2">
                            <div class="col-6">
                                <label class="form-label small">Aprem: début (h)</label>
                                <input type="number" min="6" max="23" class="form-control form-control-sm cfg-input"
                                       name="startHourAfternoon" value="${planningConfig.startHourAfternoon}">
                            </div>
                            <div class="col-6">
                                <label class="form-label small">Aprem: fin (h)</label>
                                <input type="number" min="6" max="23" class="form-control form-control-sm cfg-input"
                                       name="endHourAfternoon" value="${planningConfig.endHourAfternoon}">
                            </div>
                        </div>

                        <div class="row g-2 mb-2">
                            <div class="col-6">
                                <label class="form-label small">Durée d'une soutenance (min)</label>
                                <input type="number" min="15" step="5" class="form-control form-control-sm cfg-input"
                                       name="soutenanceDurationMinutes" value="${planningConfig.soutenanceDurationMinutes}">
                            </div>
                            <div class="col-6">
                                <label class="form-label small">Pause entre 2 (min)</label>
                                <input type="number" min="0" step="5" class="form-control form-control-sm cfg-input"
                                       name="breakBetweenMinutes" value="${planningConfig.breakBetweenMinutes}">
                            </div>
                        </div>
                        <div class="small text-muted">
                            Créneaux/jour estimés: <strong id="slots-per-day-display">${planningConfig.slotsPerDay}</strong>
                        </div>
                    </div>

                    <!-- ── Constraints ── -->
                    <div class="config-section mb-3">
                        <h6 class="d-flex justify-content-between align-items-center">
                            <span>Contraintes</span>
                            <button type="button" class="btn btn-sm btn-link p-0" data-bs-toggle="collapse" data-bs-target="#constraintsBody">Afficher/Masquer</button>
                        </h6>
                        <div class="collapse show" id="constraintsBody">
                            <c:forEach var="c" items="${constraints}">
                                <div class="constraint-row">
                                    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
                                        <div class="flex-grow-1" style="min-width:60%;">
                                            <label class="small fw-semibold mb-1">${c.label}</label>
                                            <input type="text"
                                                   name="constraint_value_${c.id}"
                                                   value="${c.value}"
                                                   class="form-control form-control-sm cfg-input">
                                        </div>
                                        <div class="priority-toggle btn-group btn-group-sm" role="group" aria-label="Priorité">
                                            <c:choose>
                                                <c:when test="${c.priorityLocked}">
                                                    <span class="badge bg-${c.priority == 'HARD' ? 'danger' : 'warning text-dark'} align-self-center">${c.priority}</span>
                                                    <input type="hidden" name="constraint_priority_${c.id}" value="${c.priority}">
                                                </c:when>
                                                <c:otherwise>
                                                    <input type="radio" class="btn-check" id="hard_${c.id}" name="constraint_priority_${c.id}" value="HARD" ${c.priority == 'HARD' ? 'checked' : ''}>
                                                    <label class="btn btn-outline-danger" for="hard_${c.id}">Dure</label>
                                                    <input type="radio" class="btn-check" id="soft_${c.id}" name="constraint_priority_${c.id}" value="SOFT" ${c.priority == 'SOFT' ? 'checked' : ''}>
                                                    <label class="btn btn-outline-warning" for="soft_${c.id}">Souple</label>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>

                    <!-- ── Salles selection (will be populated below the salle CRUD) ── -->
                    <div class="config-section mb-3">
                        <h6>Salles</h6>
                        <div class="d-flex gap-1 mb-2 flex-wrap">
                            <button type="button" class="btn btn-sm btn-outline-primary" id="selectAllSalles">Tout cocher</button>
                            <button type="button" class="btn btn-sm btn-outline-secondary" id="unselectAllSalles">Tout décocher</button>
                        </div>
                        <div class="border rounded" style="max-height: 280px; overflow-y: auto;">
                            <c:choose>
                                <c:when test="${empty salles}">
                                    <p class="text-muted small p-3 mb-0">Aucune salle. Ajoutez-en ci-dessous.</p>
                                </c:when>
                                <c:otherwise>
                                    <table class="table table-sm align-middle mb-0">
                                        <tbody>
                                            <c:forEach var="salle" items="${salles}">
                                                <tr class="salle-row">
                                                    <td style="width: 36px;">
                                                        <input class="form-check-input" type="checkbox"
                                                               name="selectedSalles" value="${salle.id_salle}"
                                                               id="salle_${salle.id_salle}" checked>
                                                    </td>
                                                    <td>
                                                        <label class="form-check-label small mb-0" for="salle_${salle.id_salle}">
                                                            <strong>${salle.num_salle}</strong>
                                                            <span class="text-muted small d-block">${salle.block}</span>
                                                        </label>
                                                    </td>
                                                    <td class="text-end" style="width: 50px;">
                                                        <a href="deleteSalle.do?id=${salle.id_salle}"
                                                           class="btn btn-sm btn-link text-danger p-1"
                                                           onclick="return confirm('Supprimer la salle ${salle.num_salle} ?')"
                                                           title="Supprimer">
                                                            <i class="fa-solid fa-trash"></i>
                                                        </a>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <input type="hidden" name="numberOfRooms" id="numberOfRooms" value="${salles.size()}">

                    <button type="submit" class="btn btn-primary w-100" ${!hasAffectations ? 'disabled' : ''}>
                        <i class="fa-solid fa-rocket me-1"></i>
                        <c:choose>
                            <c:when test="${not empty soutenances}">Régénérer le planning</c:when>
                            <c:otherwise>Générer le planning</c:otherwise>
                        </c:choose>
                    </button>
                </form>

                <!-- ── Salle CRUD (out of the planning form so they can be saved independently) ── -->
                <hr class="my-3">

                <c:if test="${not empty salleFlash}">
                    <div class="alert alert-${salleFlashIsError == true ? 'danger' : 'success'} small alert-dismissible fade show">
                        ${salleFlash}
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                </c:if>

                <h6 class="fw-bold small text-uppercase text-muted mb-2">Gérer les salles</h6>

                <form action="addSalle.do" method="post" class="mb-2">
                    <div class="input-group input-group-sm">
                        <input type="text" class="form-control" name="numSalle" placeholder="Ex: S5 NB" required>
                        <button class="btn btn-outline-success" type="submit"><i class="fa-solid fa-plus"></i></button>
                    </div>
                </form>

                <form action="addSalleBulk.do" method="post" class="mb-2">
                    <textarea class="form-control form-control-sm mb-2" name="salleNames" rows="2"
                              placeholder="Une salle par ligne, ou séparées par , ou ;"></textarea>
                    <button class="btn btn-outline-primary btn-sm w-100" type="submit">
                        <i class="fa-solid fa-list-check me-1"></i>Ajouter en lot
                    </button>
                </form>

                <form action="deleteAllSalles.do" method="post"
                      onsubmit="return confirm('Supprimer TOUTES les salles non utilisées ? Les salles référencées par un planning resteront.');">
                    <button class="btn btn-outline-danger btn-sm w-100" type="submit">
                        <i class="fa-solid fa-trash me-1"></i>Supprimer toutes les salles
                    </button>
                </form>
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
