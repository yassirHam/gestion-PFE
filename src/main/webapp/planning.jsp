<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt" %>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Planning des Soutenances PFE</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <style>
        body { background: #f0f2f5; font-family: 'Segoe UI', sans-serif; }

        /* ── Navbar ── */
        .navbar-brand { font-weight: 800; letter-spacing: .5px; }

        /* ── Page header ── */
        .page-header {
            background: linear-gradient(135deg, #1A56DB 0%, #0a3abf 100%);
            color: white;
            padding: 28px 32px 20px;
            border-radius: 14px;
            margin-bottom: 24px;
            box-shadow: 0 4px 20px rgba(26,86,219,.35);
        }
        .page-header h1 { font-size: 1.5rem; font-weight: 700; margin: 0; }
        .page-header .sub { font-size: .88rem; opacity: .85; margin-top: 4px; }

        /* ── Cards ── */
        .card-action {
            border: none;
            border-radius: 12px;
            box-shadow: 0 2px 12px rgba(0,0,0,.08);
        }

        /* ── Planning table ── */
        .planning-wrapper {
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 2px 16px rgba(0,0,0,.1);
        }
        #planningTable {
            font-size: .78rem;
            border-collapse: collapse;
            width: 100%;
        }
        #planningTable thead th {
            background: #1A56DB;
            color: white;
            font-weight: 600;
            padding: 10px 7px;
            text-align: center;
            white-space: nowrap;
            border: 1px solid #144bb5;
        }
        #planningTable tbody td {
            padding: 6px 7px;
            border: 1px solid #dee2e6;
            text-align: center;
            vertical-align: middle;
        }
        /* Prof colored cells */
        .cell-prof {
            font-weight: 700;
            color: white;
            border-radius: 3px;
        }
        /* Filière colored cells */
        .fil-GI   { background: #CFE2FF; }
        .fil-ID   { background: #FFF3CD; }
        .fil-TDIA { background: #D9EAD3; }
        .fil-other{ background: #f8f9fa; }

        /* Alternating date groups */
        .date-separator td {
            background: #1A56DB !important;
            color: white !important;
            font-weight: 700;
            font-size: .82rem;
            text-align: left;
            padding: 5px 10px;
        }

        /* ── Legend ── */
        .legend-badge {
            display: inline-block;
            padding: 3px 10px;
            border-radius: 4px;
            font-size: .78rem;
            font-weight: 600;
            margin: 3px;
        }

        /* ── Buttons ── */
        .btn-export {
            font-weight: 600;
            border-radius: 8px;
            padding: 9px 20px;
            transition: transform .15s, box-shadow .15s;
        }
        .btn-export:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,.18); }

        /* ── Stats bar ── */
        .stat-pill {
            background: white;
            border-radius: 10px;
            padding: 10px 18px;
            box-shadow: 0 1px 8px rgba(0,0,0,.08);
            text-align: center;
        }
        .stat-pill .num { font-size: 1.6rem; font-weight: 800; color: #1A56DB; line-height: 1; }
        .stat-pill .lbl { font-size: .75rem; color: #6c757d; margin-top: 2px; }
    </style>
</head>
<body>

<!-- ═══ Navbar ═══════════════════════════════════════════════════════════ -->
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

<div class="container-fluid px-4">

    <!-- ═══ Page header ═══════════════════════════════════════════════════ -->
    <div class="page-header">
        <h1><i class="fa-solid fa-calendar-check me-2"></i>Planning des Soutenances PFE</h1>
        <div class="sub">
            École Nationale des Sciences Appliquées – Al Hoceima &nbsp;|&nbsp;
            Département Mathématiques et Informatique &nbsp;|&nbsp;
            Première Session — 2024/2025
        </div>
    </div>

    <!-- ═══ Action card ════════════════════════════════════════════════════ -->
    <div class="card card-action p-3 mb-4">
        <div class="d-flex flex-wrap align-items-center gap-3">

            <!-- Generate button -->
            <form action="lancerPlanning.do" method="post"
                  onsubmit="return confirm('Générer le planning ? Cela remplacera le planning existant.')">
                <button id="btnGenerer" class="btn btn-success btn-export">
                    <i class="fa-solid fa-wand-magic-sparkles me-2"></i>Générer le Planning
                </button>
            </form>

            <!-- Export buttons (only when planning exists) -->
            <c:if test="${not empty soutenances}">
                <form action="planningPdf.do" method="post" class="d-inline">
                    <button id="btnPdf" class="btn btn-danger btn-export">
                        <i class="fa-solid fa-file-pdf me-2"></i>Télécharger PDF
                    </button>
                </form>
                <form action="planningDocx.do" method="post" class="d-inline">
                    <button id="btnWord" class="btn btn-primary btn-export">
                        <i class="fa-solid fa-file-word me-2"></i>Télécharger Word
                    </button>
                </form>
            </c:if>

            <!-- Stats pills -->
            <c:if test="${not empty soutenances}">
                <div class="ms-auto d-flex gap-3">
                    <div class="stat-pill">
                        <div class="num">${soutenances.size()}</div>
                        <div class="lbl">Soutenances</div>
                    </div>
                </div>
            </c:if>
        </div>
    </div>

    <!-- ═══ Debug log (collapsed) ══════════════════════════════════════════ -->
    <c:if test="${not empty debug}">
        <div class="mb-3">
            <button class="btn btn-sm btn-outline-secondary" type="button"
                    data-bs-toggle="collapse" data-bs-target="#debugLog">
                <i class="fa-solid fa-terminal me-1"></i>Voir le journal de génération
            </button>
            <div class="collapse mt-2" id="debugLog">
                <div class="card card-body bg-dark text-light" style="font-size:.8rem;max-height:220px;overflow-y:auto">
                    <c:forEach var="d" items="${debug}">
                        <div>${d}</div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </c:if>

    <!-- ═══ No planning yet ════════════════════════════════════════════════ -->
    <c:if test="${empty soutenances}">
        <div class="card card-action p-5 text-center">
            <i class="fa-regular fa-calendar-xmark fa-3x text-muted mb-3"></i>
            <h5 class="text-muted">Aucun planning généré</h5>
            <p class="text-muted small">
                Cliquez sur <strong>Générer le Planning</strong> pour lancer l'algorithme
                d'affectation des jurys, des salles et des créneaux horaires.
            </p>
        </div>
    </c:if>

    <!-- ═══ Planning table ═════════════════════════════════════════════════ -->
    <c:if test="${not empty soutenances}">

        <!-- Legend: professor colors -->
        <div class="card card-action p-3 mb-3">
            <div class="fw-semibold mb-2 text-muted small">
                <i class="fa-solid fa-palette me-1"></i>Légende des encadrants
            </div>
            <div id="legendContainer">
                <%-- built by JS below --%>
            </div>
            <div class="mt-2">
                <span class="legend-badge fil-GI">Filière GI</span>
                <span class="legend-badge fil-ID">Filière ID</span>
                <span class="legend-badge fil-TDIA">Filière TDIA</span>
            </div>
        </div>

        <!-- Table -->
        <div class="planning-wrapper mb-5">
            <table id="planningTable">
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Encadrant</th>
                        <th>Membre de jury 1</th>
                        <th>Membre de jury 2</th>
                        <th>Date</th>
                        <th>Heure</th>
                        <th>Salle</th>
                        <th>Nom d'étudiant</th>
                        <th>Prénom d'étudiant</th>
                        <th>Filière</th>
                    </tr>
                </thead>
                <tbody>
                    <%-- We'll group by date via JS, but render raw rows here --%>
                    <c:set var="prevDate" value=""/>
                    <c:forEach var="s" items="${soutenances}" varStatus="st">
                        <fmt:formatDate var="dateStr" value="${s.date}" pattern="dd/MM/yyyy"/>
                        <fmt:formatDate var="dateKey" value="${s.date}" pattern="yyyy-MM-dd"/>

                        <%-- Date separator row --%>
                        <c:if test="${dateKey != prevDate}">
                            <tr class="date-separator">
                                <td colspan="10">
                                    <i class="fa-regular fa-calendar me-2"></i>
                                    <fmt:formatDate value="${s.date}" pattern="EEEE dd MMMM yyyy" />
                                </td>
                            </tr>
                            <c:set var="prevDate" value="${dateKey}"/>
                        </c:if>
                        <c:set var="filiere" value="${s.etudiant.filiere}"/>
                        <c:set var="filClass"
                               value="${filiere == 'GI' ? 'fil-GI' : filiere == 'ID' ? 'fil-ID' : filiere == 'TDIA' ? 'fil-TDIA' : 'fil-other'}"/>

                        <tr>
                            <td>${st.count}</td>

                            <!-- Encadrant — colored by professor -->
                            <td class="cell-prof"
                                data-prof-id="${s.jury.president.idp}"
                                style="background-color:#${profColors[s.jury.president.idp]}">
                                ${s.jury.president.nom} ${s.jury.president.prenom}
                            </td>

                            <!-- Jury member 1 -->
                            <td class="cell-prof"
                                data-prof-id="${s.jury.rapporteur1.idp}"
                                style="background-color:#${profColors[s.jury.rapporteur1.idp]}">
                                ${s.jury.rapporteur1.nom} ${s.jury.rapporteur1.prenom}
                            </td>

                            <!-- Jury member 2 -->
                            <td class="cell-prof"
                                data-prof-id="${s.jury.rapporteur2.idp}"
                                style="background-color:#${profColors[s.jury.rapporteur2.idp]}">
                                ${s.jury.rapporteur2.nom} ${s.jury.rapporteur2.prenom}
                            </td>

                            <!-- Date / Heure / Salle — filière colored -->
                            <td class="${filClass}">${dateStr}</td>
                            <td class="${filClass}">${s.heure}</td>
                            <td class="${filClass}">${s.salle.num_salle}</td>

                            <!-- Student -->
                            <td>${s.etudiant.nomE}</td>
                            <td>${s.etudiant.prenomE}</td>
                            <td class="${filClass}"><strong>${filiere}</strong></td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

</div><!-- /container-fluid -->

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
/* Build professor color legend dynamically */
(function() {
    const cells = document.querySelectorAll('.cell-prof');
    const seen  = {};
    cells.forEach(td => {
        const profId = td.dataset.profId;
        const color  = td.style.backgroundColor;
        const name   = td.textContent.trim();
        if (!seen[profId]) {
            seen[profId] = { color, name };
        }
    });
    const container = document.getElementById('legendContainer');
    if (!container) return;
    Object.values(seen).forEach(({ color, name }) => {
        const span = document.createElement('span');
        span.className = 'legend-badge';
        span.style.backgroundColor = color;
        span.style.color = 'white';
        span.textContent = name;
        container.appendChild(span);
    });
})();
</script>
</body>
</html>
