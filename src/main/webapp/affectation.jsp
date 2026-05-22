<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Affectation - Gestion PFE</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <style>
        body {
            background-color: #f8f9fa;
            font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .badge-GI   { background-color: #0d6efd; color: white; }
        .badge-ID   { background-color: #ffc107; color: black; }
        .badge-TDIA { background-color: #198754; color: white; }

        .fichier-item {
            border: 1px solid #ddd;
            padding: 8px 12px;
            background: #f8f9fa;
            border-radius: 6px;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        .card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.04);
        }

        .upload-zone {
            border: 2px dashed #cbd5e1;
            border-radius: 12px;
            padding: 24px;
            text-align: center;
            background: #f8fafc;
            transition: border-color 0.2s, background 0.2s;
        }
        .upload-zone:hover { border-color: #6366f1; background: #f1f5f9; }

        @media (max-width: 576px) {
            .container { padding-left: 12px; padding-right: 12px; }
            h2 { font-size: 1.4rem; }
        }
    </style>
</head>

<body>

<!-- Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark bg-primary mb-4 shadow-sm">
    <div class="container-fluid px-3 px-lg-4">
        <a class="navbar-brand fw-bold" href="index.jsp">
            <i class="fa-solid fa-graduation-cap me-2"></i>Gestion PFE
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link active" href="affectation.do"><i class="fa-solid fa-users me-1"></i> Affectation</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="planning.do"><i class="fa-solid fa-calendar-days me-1"></i> Planning</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="pv.do"><i class="fa-solid fa-file-lines me-1"></i> PVs</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="dashboard.do"><i class="fa-solid fa-chart-pie me-1"></i> Dashboard</a>
                </li>
            </ul>
        </div>
    </div>
</nav>

<div class="container">

    <h2 class="mb-4 text-center"><i class="fa-solid fa-list-check me-2"></i>Affectation des encadrants</h2>

    <!-- DEBUG / Flash messages -->
    <c:set var="finalDebug" value="${not empty debug ? debug : sessionScope.affectationDebug}" />
    <c:if test="${not empty finalDebug}">
        <div class="alert alert-info">
            <ul class="mb-0">
                <c:forEach var="d" items="${finalDebug}">
                    <li>${d}</li>
                </c:forEach>
            </ul>
        </div>
        <c:remove var="affectationDebug" scope="session" />
    </c:if>

    <!-- ================= UNIFIED EXCEL IMPORT ================= -->
    <div class="card p-3 p-md-4 mb-4">

        <h5 class="fw-bold mb-3"><i class="fa-solid fa-file-excel text-success me-2"></i> Import unifié des données</h5>
        <p class="text-muted small mb-3">
            Téléversez <strong>un seul fichier Excel</strong> contenant plusieurs feuilles (une par filière, plus une feuille
            <code>Professeurs</code> et, optionnellement, une feuille <code>Salles</code>).
        </p>

        <!-- Format preview -->
        <button class="btn btn-outline-secondary btn-sm mb-3" type="button" data-bs-toggle="collapse" data-bs-target="#formatPreview" aria-expanded="false">
            <i class="fa-solid fa-circle-info me-1"></i> Voir le format Excel attendu
        </button>

        <div class="collapse" id="formatPreview">
            <div class="row g-3 mb-3">
                <div class="col-12 col-lg-6">
                    <div class="border rounded p-3 bg-white h-100">
                        <h6 class="fw-bold"><i class="fa-solid fa-user-graduate text-primary me-1"></i>
                            Feuille par filière (ex: <code>GI</code>, <code>ID</code>, <code>TDIA</code>, ...)</h6>
                        <p class="small text-muted mb-2">Le <strong>nom de la feuille</strong> est utilisé comme code filière.</p>
                        <div class="table-responsive">
                            <table class="table table-bordered table-sm text-center mb-0" style="font-size:0.78rem;">
                                <thead class="table-dark">
                                    <tr>
                                        <th>A</th><th>B</th><th>C</th><th>D</th><th>E</th><th>F</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr class="table-light fw-semibold">
                                        <td>CNE</td><td>NOM</td><td>PRÉNOM</td><td>EMAIL</td>
                                        <td>CNE BINÔME</td><td>SUJET PFE</td>
                                    </tr>
                                    <tr class="text-muted fst-italic">
                                        <td>R140025687</td><td>BENALI</td><td>Hamza</td>
                                        <td>h.benali@etu.ma</td><td>R140025688</td><td>App web</td>
                                    </tr>
                                    <tr class="text-muted fst-italic">
                                        <td>R140025688</td><td>EL OUALI</td><td>Sara</td>
                                        <td>s.elouali@etu.ma</td><td>R140025687</td><td>App web</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="col-12 col-lg-6">
                    <div class="border rounded p-3 bg-white h-100">
                        <h6 class="fw-bold"><i class="fa-solid fa-chalkboard-user text-success me-1"></i>
                            Feuille <code>Professeurs</code></h6>
                        <p class="small text-muted mb-2">Une seule feuille pour tous les professeurs.</p>
                        <div class="table-responsive">
                            <table class="table table-bordered table-sm text-center mb-0" style="font-size:0.78rem;">
                                <thead class="table-dark">
                                    <tr>
                                        <th>A</th><th>B</th><th>C</th><th>D</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr class="table-light fw-semibold">
                                        <td>NOM</td><td>PRÉNOM</td><td>DISCIPLINE</td><td>SPÉCIALITÉ</td>
                                    </tr>
                                    <tr class="text-muted fst-italic">
                                        <td>AMRANI</td><td>Karim</td><td>Informatique</td><td>IA</td>
                                    </tr>
                                    <tr class="text-muted fst-italic">
                                        <td>SMITH</td><td>John</td><td>Anglais</td><td>Anglais</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="col-12">
                    <div class="border rounded p-3 bg-white">
                        <h6 class="fw-bold"><i class="fa-solid fa-door-open text-warning me-1"></i>
                            Feuille <code>Salles</code> <span class="badge bg-secondary">optionnelle</span></h6>
                        <p class="small text-muted mb-2">Si présente, les salles non existantes sont créées automatiquement.</p>
                        <div class="table-responsive">
                            <table class="table table-bordered table-sm text-center mb-0" style="font-size:0.78rem;">
                                <thead class="table-dark">
                                    <tr><th>A</th><th>B</th><th>C</th></tr>
                                </thead>
                                <tbody>
                                    <tr class="table-light fw-semibold">
                                        <td>NUM_SALLE</td><td>BLOCK</td><td>STATUS</td>
                                    </tr>
                                    <tr class="text-muted fst-italic">
                                        <td>S3 AB</td><td>Ancien Bloc</td><td>Libre</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <a href="templateData.do" class="btn btn-sm btn-outline-primary">
                <i class="fa-solid fa-download me-1"></i>Télécharger le modèle complet
            </a>
        </div>

        <!-- Upload form -->
        <form action="uploadData.do" method="post" enctype="multipart/form-data" class="mt-3">
            <div class="upload-zone mb-3">
                <i class="fa-solid fa-cloud-arrow-up fa-2x text-primary mb-2"></i>
                <div>
                    <input type="file" name="file" accept=".xlsx" class="form-control" required>
                </div>
            </div>
            <button class="btn btn-primary w-100">
                <i class="fa-solid fa-upload me-1"></i>Importer les données
            </button>
        </form>
    </div>

    <!-- ================= LISTES ================= -->
    <div class="card p-3 p-md-4 mb-4">

        <h5 class="fw-bold mb-3"><i class="fa-solid fa-folder-open me-2"></i>Listes disponibles</h5>

        <c:choose>
            <c:when test="${empty fichiers}">
                <div class="alert alert-warning mb-0">Aucune liste importée pour le moment.</div>
            </c:when>

            <c:otherwise>
                <form id="selectionForm">
                    <div class="d-flex flex-wrap gap-2 mb-3">
                        <c:forEach var="f" items="${fichiers}">
                            <label class="fichier-item">
                                <input type="checkbox" name="selectedFilieres" value="${f.filiere}">
                                <span class="
                                    ${f.filiere == 'GI' ? 'badge-GI' :
                                      f.filiere == 'ID' ? 'badge-ID' :
                                      f.filiere == 'TDIA' ? 'badge-TDIA' : 'bg-secondary text-white'} badge">
                                    ${f.filiere}
                                </span>
                                <strong class="text-truncate" style="max-width: 200px;">${f.nomFichier}</strong>
                                <small class="text-muted">(${f.nbEtudiants} étudiants)</small>
                            </label>
                        </c:forEach>
                    </div>

                    <div class="d-flex gap-2 flex-wrap">
                        <button type="submit"
                                formaction="lancerAffectation.do"
                                formmethod="post"
                                class="btn btn-success"
                                onclick="return confirm('Confirmer le lancement de l\'affectation ? (cela écrasera la précédente)')">
                            <i class="fa-solid fa-rocket me-1"></i>Lancer Affectation
                        </button>

                        <button type="submit"
                                formaction="supprimerListes.do"
                                formmethod="post"
                                class="btn btn-danger"
                                onclick="return confirm('Supprimer les listes sélectionnées ?')">
                            <i class="fa-solid fa-trash me-1"></i>Supprimer
                        </button>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- ================= TELECHARGEMENT APRES AFFECTATION ================= -->
    <c:if test="${affectationDone == true || sessionScope.affectationDone == true}">
        <div class="alert alert-success text-center mt-4">
            <h5 class="fw-bold mb-2"><i class="fa-solid fa-circle-check me-2"></i>Affectation effectuée avec succès</h5>
            <p class="mb-3">
                Les affectations ont été enregistrées en base. Vous pouvez exporter le résultat
                ou générer le planning des soutenances.
            </p>

            <div class="d-flex flex-wrap gap-2 justify-content-center">
                <form action="exportPdf.do" method="post" class="d-inline">
                    <button class="btn btn-danger">
                        <i class="fa-solid fa-file-pdf me-1"></i>Télécharger PDF
                    </button>
                </form>

                <form action="exportDocx.do" method="post" class="d-inline">
                    <button class="btn btn-primary">
                        <i class="fa-solid fa-file-word me-1"></i>Télécharger Word
                    </button>
                </form>

                <a href="planning.do" class="btn btn-success">
                    <i class="fa-solid fa-calendar-check me-1"></i>Voir / Générer le Planning
                </a>
            </div>
        </div>
        <c:remove var="affectationDone" scope="session" />
    </c:if>

    <!-- Historique des affectations -->
    <div class="card p-3 p-md-4 mt-4">
        <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
            <h5 class="fw-bold mb-0"><i class="fa-solid fa-clock-rotate-left text-primary me-2"></i> Historique des affectations</h5>
            <c:if test="${not empty historyTimestamps}">
                <a href="clearHistory.do?type=affectation" class="btn btn-sm btn-outline-danger" onclick="return confirm('Supprimer TOUT l\'historique des affectations ?')">
                    <i class="fa-solid fa-trash-can me-1"></i> Vider l'historique
                </a>
            </c:if>
        </div>

        <c:if test="${not empty sessionScope.restoreError}">
            <div class="alert alert-danger">
                <i class="fa-solid fa-triangle-exclamation me-2"></i> ${sessionScope.restoreError}
            </div>
            <c:remove var="restoreError" scope="session"/>
        </c:if>

        <c:choose>
            <c:when test="${not empty historyTimestamps}">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <tbody>
                            <c:forEach var="ts" items="${historyTimestamps}">
                                <tr>
                                    <td>
                                        <i class="fa-solid fa-clock-rotate-left text-muted me-2"></i>
                                        <strong>Le ${ts.substring(0,10).replace('-', '/')} à ${ts.substring(11).replace('-', ':')}</strong>
                                    </td>
                                    <td class="text-end">
                                        <a href="restoreAffectation.do?timestamp=${ts}" class="btn btn-sm btn-outline-success me-1" onclick="return confirm('Restaurer cette affectation va écraser l\'affectation actuelle. Continuer ?')">
                                            <i class="fa-solid fa-clock-rotate-left"></i> Restaurer
                                        </a>
                                        <a href="downloadHistory.do?file=Affectation_${ts}.pdf" class="btn btn-sm btn-outline-danger" title="PDF">
                                            <i class="fa-solid fa-file-pdf"></i>
                                        </a>
                                        <a href="downloadHistory.do?file=Affectation_${ts}.docx" class="btn btn-sm btn-outline-primary" title="Word">
                                            <i class="fa-solid fa-file-word"></i>
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

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
