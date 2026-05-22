<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Configuration du Planning – Gestion PFE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <style>
        body {
            background: linear-gradient(135deg, #f0f4ff 0%, #fafbff 100%);
            min-height: 100vh;
            font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .navbar-brand { font-weight: 700; letter-spacing: -0.5px; }
        .card {
            border: none;
            border-radius: 16px;
            box-shadow: 0 8px 24px rgba(0,0,0,0.07);
        }
        .section-title {
            font-size: 0.72rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: .08em;
            color: #64748b;
            margin-bottom: 0.75rem;
        }
        .constraint-row {
            border-bottom: 1px solid #f1f5f9;
            padding: 10px 0;
            transition: background 0.15s;
        }
        .constraint-row:last-child { border-bottom: none; }
        .constraint-row:hover { background: #f8faff; border-radius: 8px; }
        .priority-toggle .btn-check:checked + .btn-outline-danger { background: #ef4444; color: white; border-color: #ef4444; }
        .priority-toggle .btn-check:checked + .btn-outline-warning { background: #f59e0b; color: #1e293b; border-color: #f59e0b; }
        .form-control-sm, .form-select-sm { border-radius: 8px; }
        .badge-hard  { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
        .badge-soft  { background: #fefce8; color: #ca8a04; border: 1px solid #fef08a; }
        .time-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; }

        .reco-info    { border-left: 4px solid #0dcaf0; }
        .reco-warning { border-left: 4px solid #ffc107; }
        .reco-danger  { border-left: 4px solid #dc3545; }

        .page-header {
            background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
            color: white;
            border-radius: 16px;
            padding: 2rem;
            margin-bottom: 2rem;
        }

        .step-badge {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 0.8rem;
            flex-shrink: 0;
        }
        .salle-row td { vertical-align: middle; }

        /* ── Loading Overlay ───────────────────────────────────────────── */
        #loading-overlay {
            display: none;
            position: fixed;
            inset: 0;
            z-index: 9999;
            background: rgba(15, 23, 42, 0.92);
            backdrop-filter: blur(6px);
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 2rem;
        }
        #loading-overlay.active { display: flex; }

        .loading-card {
            background: rgba(255, 255, 255, 0.06);
            border: 1px solid rgba(255,255,255,0.12);
            border-radius: 20px;
            padding: 2.5rem 3rem;
            text-align: center;
            max-width: 460px;
            width: 90%;
            box-shadow: 0 25px 60px rgba(0,0,0,0.4);
        }

        .orbit-spinner {
            width: 80px;
            height: 80px;
            position: relative;
            margin: 0 auto 1.5rem;
        }
        .orbit-spinner .ring {
            position: absolute;
            inset: 0;
            border-radius: 50%;
            border: 3px solid transparent;
            animation: orbit-spin 1.4s linear infinite;
        }
        .orbit-spinner .ring:nth-child(1) {
            border-top-color: #6366f1;
            animation-duration: 1.0s;
        }
        .orbit-spinner .ring:nth-child(2) {
            inset: 10px;
            border-top-color: #8b5cf6;
            animation-duration: 1.4s;
            animation-direction: reverse;
        }
        .orbit-spinner .ring:nth-child(3) {
            inset: 22px;
            border-top-color: #a78bfa;
            animation-duration: 1.8s;
        }
        .orbit-spinner .core {
            position: absolute;
            inset: 32px;
            border-radius: 50%;
            background: radial-gradient(circle, #6366f1 0%, #4f46e5 100%);
            animation: core-pulse 1.4s ease-in-out infinite;
        }
        @keyframes orbit-spin { to { transform: rotate(360deg); } }
        @keyframes core-pulse {
            0%, 100% { transform: scale(1); opacity: 0.8; }
            50%       { transform: scale(1.15); opacity: 1; }
        }

        .loading-title {
            font-size: 1.2rem;
            font-weight: 700;
            color: #f1f5f9;
            margin-bottom: 0.4rem;
        }
        .loading-phase {
            font-size: 0.85rem;
            color: #94a3b8;
            min-height: 1.4em;
            transition: opacity 0.4s;
        }
        .loading-phase.fade-out { opacity: 0; }
        .loading-phase.fade-in  { opacity: 1; }

        .loading-bar-wrap {
            width: 100%;
            height: 4px;
            background: rgba(255,255,255,0.1);
            border-radius: 2px;
            margin-top: 1.5rem;
            overflow: hidden;
        }
        .loading-bar {
            height: 100%;
            width: 0%;
            border-radius: 2px;
            background: linear-gradient(90deg, #6366f1, #a78bfa);
            transition: width 0.8s ease;
        }
        .loading-dots {
            color: #64748b;
            font-size: 0.75rem;
            margin-top: 0.75rem;
        }

        @media (max-width: 576px) {
            .container { padding-left: 12px; padding-right: 12px; }
            .time-grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>

<!-- ── Full-Screen Loading Overlay ────────────────────────────────── -->
<div id="loading-overlay" role="status" aria-live="polite">
    <div class="loading-card">
        <div class="orbit-spinner" aria-hidden="true">
            <div class="ring"></div>
            <div class="ring"></div>
            <div class="ring"></div>
            <div class="core"></div>
        </div>
        <div class="loading-title">Génération du planning en cours…</div>
        <div class="loading-phase fade-in" id="loading-phase">Initialisation du moteur de planification</div>
        <div class="loading-bar-wrap">
            <div class="loading-bar" id="loading-bar"></div>
        </div>
        <div class="loading-dots" id="loading-dots">Veuillez patienter — cela peut prendre quelques secondes</div>
    </div>
</div>

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
                    <a class="nav-link active" href="config.do"><i class="fa-solid fa-sliders me-1"></i>Configuration</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="planning.do"><i class="fa-solid fa-calendar-days me-1"></i>Planning</a>
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

<div class="container py-2 py-md-4" style="max-width: 860px;">

    <c:if test="${not hasAffectations}">
        <div class="alert alert-warning d-flex align-items-center gap-2 mb-4">
            <i class="fa-solid fa-triangle-exclamation fa-lg"></i>
            <div>
                Aucune affectation trouvée. <a href="affectation.do" class="alert-link">Effectuez l'affectation</a> avant de configurer le planning.
            </div>
        </div>
    </c:if>

    <%-- ── Planning failure banner ──────────────────────────────────────────── --%>
    <c:if test="${planningFailed == true}">
        <div class="card mb-4" style="border: 2px solid #dc3545; border-radius: 16px; overflow: hidden;">
            <%-- Red header --%>
            <div style="background: linear-gradient(135deg, #dc3545 0%, #b91c1c 100%); padding: 1.25rem 1.5rem; color: white;">
                <div class="d-flex align-items-center gap-3">
                    <div style="width:48px;height:48px;border-radius:50%;background:rgba(255,255,255,0.15);display:flex;align-items:center;justify-content:center;flex-shrink:0;">
                        <i class="fa-solid fa-circle-xmark fa-xl"></i>
                    </div>
                    <div>
                        <h5 class="fw-bold mb-0">Le planning n'a pas pu être généré</h5>
                        <p class="small mb-0" style="opacity:0.85;">Des contraintes <strong>dures</strong> ne peuvent pas être satisfaites avec les paramètres actuels. Modifiez la configuration ci-dessous, puis relancez.</p>
                    </div>
                </div>
            </div>

            <div class="p-4">

                <%-- Violated hard constraints --%>
                <c:if test="${not empty planningHardViolations}">
                    <h6 class="fw-bold mb-3"><i class="fa-solid fa-ban text-danger me-2"></i>Contraintes violées</h6>
                    <div class="list-group mb-4">
                        <c:forEach var="v" items="${planningHardViolations}">
                            <div class="list-group-item list-group-item-danger border-danger-subtle">
                                <div class="d-flex align-items-start gap-2">
                                    <i class="fa-solid fa-xmark-circle text-danger mt-1"></i>
                                    <div>
                                        <strong>${v.constraintLabel}</strong>
                                        <div class="small text-muted mt-1">${v.detail}</div>
                                        <c:if test="${not empty v.suggestion}">
                                            <div class="small text-success mt-1 fw-semibold">
                                                <i class="fa-solid fa-lightbulb me-1"></i>${v.suggestion}
                                            </div>
                                        </c:if>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>

                <%-- Unscheduled projects --%>
                <c:if test="${not empty planningUnscheduled}">
                    <h6 class="fw-bold mb-2"><i class="fa-solid fa-calendar-xmark text-warning me-2"></i>${planningUnscheduled.size()} projet(s) non planifiable(s)</h6>
                    <div class="alert alert-warning small mb-4 py-2">
                        <c:forEach var="p" items="${planningUnscheduled}" varStatus="st">
                            <span class="badge bg-warning text-dark me-1 mb-1">${p}</span>
                        </c:forEach>
                    </div>
                </c:if>

                <%-- Evaluate which generic actions are relevant --%>
                <c:set var="hasCapacityIssue" value="${not empty planningUnscheduled}" />
                <c:set var="hasConstraintIssue" value="${not empty planningHardViolations}" />
                <c:forEach var="v" items="${planningHardViolations}">
                    <c:if test="${v.constraintId == 'MAX_SOUTENANCES_PER_PROF_PER_DAY' or v.constraintId == 'MAX_SOUTENANCES_PER_ROOM_PER_DAY'}">
                        <c:set var="hasCapacityIssue" value="true" />
                    </c:if>
                </c:forEach>

                <%-- Generic action checklist --%>
                <h6 class="fw-bold mb-3"><i class="fa-solid fa-list-check text-primary me-2"></i>Ce que vous pouvez faire</h6>
                <div class="row g-2 mb-3">
                    <c:if test="${hasCapacityIssue}">
                        <div class="col-md-6">
                            <div class="d-flex align-items-start gap-2 p-3 rounded" style="background:#f0fdf4;border:1px solid #bbf7d0;">
                                <i class="fa-regular fa-calendar-plus text-success mt-1 fa-lg"></i>
                                <div>
                                    <div class="small fw-semibold text-success">Augmenter le nombre de jours</div>
                                    <div class="small text-muted">Donnez plus de jours dans <strong>Étape 1</strong> pour avoir plus de créneaux disponibles.</div>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="d-flex align-items-start gap-2 p-3 rounded" style="background:#eff6ff;border:1px solid #bfdbfe;">
                                <i class="fa-solid fa-door-open text-primary mt-1 fa-lg"></i>
                                <div>
                                    <div class="small fw-semibold text-primary">Ajouter ou cocher plus de salles</div>
                                    <div class="small text-muted">Plus de salles = plus de soutenances en parallèle. Voir <strong>Étape 3</strong>.</div>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="d-flex align-items-start gap-2 p-3 rounded" style="background:#fdf4ff;border:1px solid #e9d5ff;">
                                <i class="fa-solid fa-clock text-purple mt-1 fa-lg" style="color:#7c3aed;"></i>
                                <div>
                                    <div class="small fw-semibold" style="color:#6d28d9;">Élargir les plages horaires</div>
                                    <div class="small text-muted">Avancez l'heure de début ou reculez la fin pour plus de créneaux par jour.</div>
                                </div>
                            </div>
                        </div>
                    </c:if>
                    <c:if test="${hasConstraintIssue}">
                        <div class="col-md-6">
                            <div class="d-flex align-items-start gap-2 p-3 rounded" style="background:#fefce8;border:1px solid #fef08a;">
                                <i class="fa-solid fa-sliders text-warning mt-1 fa-lg"></i>
                                <div>
                                    <div class="small fw-semibold" style="color:#92400e;">Assouplir les contraintes dures</div>
                                    <div class="small text-muted">Passez certaines contraintes de <strong>Dure → Souple</strong> dans <strong>Étape 2</strong>.</div>
                                </div>
                            </div>
                        </div>
                    </c:if>
                </div>

            </div>
        </div>
    </c:if>

    <!-- Recommendations panel -->
    <div id="recommendations-panel" class="mb-3"></div>

    <form action="lancerPlanning.do" method="post" id="planningForm">

        <!-- ── STEP 1 : Time slots ── -->
        <div class="card p-4 mb-4">
            <div class="d-flex align-items-center gap-2 mb-3">
                <span class="step-badge bg-primary text-white">1</span>
                <h5 class="fw-bold mb-0">Plage horaire & durée</h5>
            </div>
            <p class="text-muted small mb-3">
                Configurez le nombre de jours, la date de départ, et les créneaux matin / après-midi.
            </p>

            <div class="row g-3 mb-3">
                <div class="col-sm-6">
                    <c:set var="needsMoreCapacity" value="${not empty planningUnscheduled}" />
                    <label class="form-label small fw-semibold ${needsMoreCapacity ? 'text-danger' : ''}">Nombre de jours</label>
                    <input type="number" min="1" max="60" class="form-control form-control-sm cfg-input ${needsMoreCapacity ? 'is-invalid border-danger border-2 shadow-sm' : ''}"
                           name="numberOfDays" value="${planningConfig.numberOfDays}" required>
                </div>
                <div class="col-sm-6">
                    <label class="form-label small fw-semibold">Date de début</label>
                    <input type="date" class="form-control form-control-sm cfg-input"
                           name="startDate" value="${planningConfig.startDate}" required>
                </div>
            </div>

            <div class="row g-3 mb-3">
                <div class="col-sm-3">
                    <label class="form-label small fw-semibold">Matin – début (h)</label>
                    <input type="number" min="6" max="23" class="form-control form-control-sm cfg-input"
                           name="startHourMorning" value="${planningConfig.startHourMorning}">
                </div>
                <div class="col-sm-3">
                    <label class="form-label small fw-semibold">Matin – fin (h)</label>
                    <input type="number" min="6" max="23" class="form-control form-control-sm cfg-input"
                           name="endHourMorning" value="${planningConfig.endHourMorning}">
                </div>
                <div class="col-sm-3">
                    <label class="form-label small fw-semibold">Après-midi – début (h)</label>
                    <input type="number" min="6" max="23" class="form-control form-control-sm cfg-input"
                           name="startHourAfternoon" value="${planningConfig.startHourAfternoon}">
                </div>
                <div class="col-sm-3">
                    <label class="form-label small fw-semibold">Après-midi – fin (h)</label>
                    <input type="number" min="6" max="23" class="form-control form-control-sm cfg-input"
                           name="endHourAfternoon" value="${planningConfig.endHourAfternoon}">
                </div>
            </div>

            <div class="row g-3">
                <div class="col-sm-6">
                    <label class="form-label small fw-semibold">Durée d'une soutenance (min)</label>
                    <input type="number" min="15" step="5" class="form-control form-control-sm cfg-input"
                           name="soutenanceDurationMinutes" value="${planningConfig.soutenanceDurationMinutes}">
                </div>
                <div class="col-sm-6">
                    <label class="form-label small fw-semibold">Pause entre deux soutenances (min)</label>
                    <input type="number" min="0" step="5" class="form-control form-control-sm cfg-input"
                           name="breakBetweenMinutes" value="${planningConfig.breakBetweenMinutes}">
                </div>
            </div>

            <div class="alert alert-info small mt-3 mb-0 py-2">
                <i class="fa-solid fa-circle-info me-1"></i>
                Créneaux / jour estimés : <strong id="slots-per-day-display">${planningConfig.slotsPerDay}</strong>
            </div>
        </div>

        <!-- ── STEP 2 : Constraints ── -->
        <div class="card p-4 mb-4">
            <div class="d-flex align-items-center gap-2 mb-3">
                <span class="step-badge bg-warning text-dark">2</span>
                <h5 class="fw-bold mb-0">Contraintes</h5>
            </div>
            <p class="text-muted small mb-3">
                Pour chaque contrainte, définissez la valeur et choisissez si elle est <span class="badge badge-hard">Dure</span> (bloquante) ou <span class="badge badge-soft">Souple</span> (avertissement).
            </p>

            <c:forEach var="c" items="${constraints}">
                <c:set var="hasViolation" value="false" />
                <c:if test="${not empty planningHardViolations}">
                    <c:forEach var="v" items="${planningHardViolations}">
                        <c:if test="${v.constraintId == c.id}">
                            <c:set var="hasViolation" value="true" />
                        </c:if>
                    </c:forEach>
                </c:if>
                <div class="constraint-row px-2">
                    <div class="d-flex justify-content-between align-items-start gap-3 flex-wrap">
                        <div class="flex-grow-1" style="min-width: 55%;">
                            <label class="small fw-semibold mb-1 d-block ${hasViolation ? 'text-danger' : ''}">${c.label}</label>
                            <input type="text"
                                   name="constraint_value_${c.id}"
                                   value="${c.value}"
                                   class="form-control form-control-sm cfg-input ${hasViolation ? 'is-invalid border-danger border-2 shadow-sm' : ''}"
                                   style="max-width: 180px;">
                        </div>
                        <div class="priority-toggle btn-group btn-group-sm align-self-end" role="group">
                            <c:choose>
                                <c:when test="${c.priorityLocked}">
                                    <span class="badge bg-${c.priority == 'HARD' ? 'danger' : 'warning text-dark'} align-self-center px-2">
                                        <i class="fa-solid fa-lock me-1"></i>${c.priority}
                                    </span>
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

        <!-- ── STEP 3 : Salles ── -->
        <div class="card p-4 mb-4">
            <div class="d-flex align-items-center gap-2 mb-3">
                <span class="step-badge bg-success text-white">3</span>
                <h5 class="fw-bold mb-0">Salles de soutenance</h5>
            </div>
            <p class="text-muted small mb-3">
                Cochez les salles à inclure dans le planning. Ajoutez-en de nouvelles si besoin.
            </p>

            <div class="d-flex gap-2 mb-2 flex-wrap">
                <button type="button" class="btn btn-sm btn-outline-primary" id="selectAllSalles">
                    <i class="fa-solid fa-check-double me-1"></i>Tout cocher
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary" id="unselectAllSalles">
                    <i class="fa-regular fa-square me-1"></i>Tout décocher
                </button>
            </div>

            <div class="border rounded mb-3" style="max-height: 240px; overflow-y: auto;">
                <c:choose>
                    <c:when test="${empty salles}">
                        <p class="text-muted small p-3 mb-0">
                            <i class="fa-solid fa-circle-exclamation me-1"></i>Aucune salle disponible. Ajoutez-en ci-dessous.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <table class="table table-sm align-middle mb-0">
                            <tbody>
                                <c:forEach var="salle" items="${salles}">
                                    <tr class="salle-row">
                                        <td style="width: 40px;">
                                            <input class="form-check-input" type="checkbox"
                                                   name="selectedSalles" value="${salle.id_salle}"
                                                   id="salle_${salle.id_salle}" checked>
                                        </td>
                                        <td>
                                            <label class="form-check-label small mb-0" for="salle_${salle.id_salle}">
                                                <strong>${salle.num_salle}</strong>
                                                <span class="text-muted d-block" style="font-size: 0.75rem;">${salle.block}</span>
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

            <!-- Salle CRUD (outside the main generate form) -->
            <input type="hidden" name="numberOfRooms" id="numberOfRooms" value="${salles.size()}">

            <hr class="my-3">
            <h6 class="section-title">Ajouter des salles</h6>

            <c:if test="${not empty salleFlash}">
                <div class="alert alert-${salleFlashIsError == true ? 'danger' : 'success'} small alert-dismissible fade show">
                    ${salleFlash}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </c:if>
        </div>

        <!-- Generate button -->
        <div class="d-grid mb-4">
            <button type="submit" class="btn btn-primary btn-lg shadow-sm" ${!hasAffectations ? 'disabled' : ''}>
                <i class="fa-solid fa-rocket me-2"></i>
                <c:choose>
                    <c:when test="${not empty soutenances}">Régénérer le planning</c:when>
                    <c:otherwise>Générer le planning</c:otherwise>
                </c:choose>
            </button>
            <a href="planning.do" class="btn btn-outline-secondary mt-2">
                <i class="fa-solid fa-calendar-check me-1"></i>Voir le planning actuel
            </a>
        </div>

    </form>

    <!-- Salle CRUD forms (separate from planningForm) -->
    <div class="card p-4 mb-4">
        <h6 class="section-title">Gérer les salles</h6>

        <form action="addSalle.do" method="post" class="mb-2">
            <div class="input-group input-group-sm">
                <input type="text" class="form-control" name="numSalle" placeholder="Ex: S5 NB" required>
                <button class="btn btn-outline-success" type="submit">
                    <i class="fa-solid fa-plus me-1"></i>Ajouter
                </button>
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
              onsubmit="return confirm('Supprimer TOUTES les salles non utilisées ?');">
            <button class="btn btn-outline-danger btn-sm w-100" type="submit">
                <i class="fa-solid fa-trash me-1"></i>Supprimer toutes les salles
            </button>
        </form>
    </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
document.addEventListener('DOMContentLoaded', function () {
    // ── Loading overlay logic ─────────────────────────────────────────────
    var form = document.getElementById('planningForm');
    if (form) {
        form.addEventListener('submit', function (e) {
            if (!confirm("Générer un nouveau planning ? L'ancien sera écrasé.")) {
                e.preventDefault();
                return;
            }
            showLoadingOverlay();
        });
    }

    var phases = [
        "Initialisation du moteur de planification…",
        "Chargement des affectations encadrants…",
        "Analyse des contraintes dures et souples…",
        "Regroupement des binômes…",
        "Attribution des créneaux horaires…",
        "Vérification des conflits de jury…",
        "Optimisation de la répartition par filière…",
        "Calcul des scores de satisfaction…",
        "Génération des salles et des horaires…",
        "Finalisation et sauvegarde du planning…",
    ];

    function showLoadingOverlay() {
        var overlay = document.getElementById('loading-overlay');
        var phaseEl = document.getElementById('loading-phase');
        var barEl   = document.getElementById('loading-bar');
        if (!overlay) return;
        overlay.classList.add('active');

        var idx = 0;
        var progress = 5;
        barEl.style.width = progress + '%';

        var interval = setInterval(function () {
            idx = (idx + 1) % phases.length;
            progress = Math.min(92, progress + (95 / phases.length));

            // Fade out → update text → fade in
            phaseEl.classList.remove('fade-in');
            phaseEl.classList.add('fade-out');
            setTimeout(function () {
                phaseEl.textContent = phases[idx];
                phaseEl.classList.remove('fade-out');
                phaseEl.classList.add('fade-in');
            }, 350);

            barEl.style.width = progress + '%';

            if (idx === phases.length - 1) clearInterval(interval);
        }, 1800);
    }

    // ── Salle select/unselect ─────────────────────────────────────────────
    var selectAllBtn    = document.getElementById('selectAllSalles');
    var unselectAllBtn  = document.getElementById('unselectAllSalles');
    var salleCheckboxes = document.querySelectorAll('input[name="selectedSalles"]');

    if (selectAllBtn) {
        selectAllBtn.addEventListener('click', function () {
            salleCheckboxes.forEach(function (cb) { cb.checked = true; });
            debouncedRefresh();
        });
    }
    if (unselectAllBtn) {
        unselectAllBtn.addEventListener('click', function () {
            salleCheckboxes.forEach(function (cb) { cb.checked = false; });
            debouncedRefresh();
        });
    }

    // ── Recommendations ───────────────────────────────────────────────────
    var panel      = document.getElementById('recommendations-panel');
    var inputs     = document.querySelectorAll('.cfg-input');
    var slotsDisp  = document.getElementById('slots-per-day-display');
    var roomsField = document.getElementById('numberOfRooms');

    function refreshRecommendations() {
        if (!panel) return;
        var fd = new URLSearchParams();
        inputs.forEach(function (inp) { if (inp.name) fd.append(inp.name, inp.value); });
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
            if (slotsDisp) slotsDisp.textContent = data.slotsPerDay;
            if (!data.recommendations || data.recommendations.length === 0) { panel.innerHTML = ''; return; }
            var html = '';
            data.recommendations.forEach(function (r) {
                html += '<div class="alert alert-' + r.bootstrap + ' small reco-' + r.bootstrap + ' mb-2">'
                      + '<i class="fa-solid ' + r.icon + ' me-1"></i>'
                      + '<strong>' + r.title + '</strong> &mdash; ' + r.message;
                if (r.suggestion) html += '<div class="mt-1"><i class="fa-solid fa-lightbulb me-1"></i>' + r.suggestion + '</div>';
                html += '</div>';
            });
            panel.innerHTML = html;
        })
        .catch(function () {});
    }

    var debounceTimer = null;
    function debouncedRefresh() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(refreshRecommendations, 250);
    }

    inputs.forEach(function (inp) {
        inp.addEventListener('input', debouncedRefresh);
        inp.addEventListener('change', debouncedRefresh);
    });
    salleCheckboxes.forEach(function (cb) { cb.addEventListener('change', debouncedRefresh); });

    refreshRecommendations();
});
</script>

</body>
</html>
