<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html>
<head>
    <title>Affectation</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <style>
        .badge-GI   { background-color: #0d6efd; color: white; }
        .badge-ID   { background-color: #ffc107; color: black; }
        .badge-TDIA { background-color: #198754; color: white; }

        .fichier-item {
            border: 1px solid #ddd;
            padding: 8px;
            background: #f8f9fa;
        }
    </style>
</head>

<body class="bg-light">

<!-- Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark bg-primary mb-4 shadow-sm">
    <div class="container-fluid px-4">
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
                    <a class="nav-link" href="dashboard.do"><i class="fa-solid fa-chart-pie me-1"></i> Dashboard</a>
                </li>
            </ul>
        </div>
    </div>
</nav>

<div class="container">

    <h2 class="mb-4 text-center"><i class="fa-solid fa-list-check me-2"></i>Affectation des encadrants</h2>

    <!-- DEBUG -->
    <c:set var="finalDebug" value="${not empty debug ? debug : sessionScope.affectationDebug}" />
    <c:if test="${not empty finalDebug}">
        <div class="alert alert-info">
            <ul class="mb-0">
                <c:forEach var="d" items="${finalDebug}">
                    <li>${d}</li>
                </c:forEach>
            </ul>
        </div>
        <%-- Clear session debug after showing it --%>
        <c:remove var="affectationDebug" scope="session" />
    </c:if>

    <!-- ================= UPLOAD ================= -->
    <div class="card p-3 mb-4">

        <!-- FORMAT PREVIEW SECTION -->
        <div class="mb-4">
            <button class="btn btn-outline-secondary btn-sm mb-3" type="button" data-bs-toggle="collapse" data-bs-target="#formatPreview" aria-expanded="false">
                <i class="fa-solid fa-circle-info me-1"></i> Voir le format Excel attendu
            </button>

            <div class="collapse" id="formatPreview">
                <div class="row g-3">

                    <!-- FORMAT ETUDIANTS -->
                    <div class="col-md-7">
                        <div class="border rounded p-3 bg-white">
                            <div class="d-flex justify-content-between align-items-end mb-2">
                                <div>
                                    <h6 class="fw-bold mb-1"><i class="fa-solid fa-user-graduate text-primary me-1"></i> Format — Fichier Étudiants</h6>
                                </div>
                                <a href="templateEtudiants.do" class="btn btn-sm btn-outline-primary"><i class="fa-solid fa-download me-1"></i>Modèle</a>
                            </div>
                            <div class="table-responsive">
                                <table class="table table-bordered table-sm text-center mb-0" style="font-size:0.82rem;">
                                    <thead class="table-dark">
                                        <tr>
                                            <th>A — Col 1</th>
                                            <th>B — Col 2</th>
                                            <th>C — Col 3</th>
                                            <th>D — Col 4</th>
                                            <th>E — Col 5</th>
                                            <th>F — Col 6</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr class="table-light fw-semibold">
                                            <td><span class="badge bg-secondary">CNE</span></td>
                                            <td><span class="badge bg-secondary">NOM</span></td>
                                            <td><span class="badge bg-secondary">PRÉNOM</span></td>
                                            <td><span class="badge bg-secondary">EMAIL</span></td>
                                            <td><span class="badge bg-secondary">CNE BINÔME</span></td>
                                            <td><span class="badge bg-info text-dark">SUJET PFE</span></td>
                                        </tr>
                                        <tr class="text-muted fst-italic">
                                            <td>R140025687</td>
                                            <td>BENALI</td>
                                            <td>Hamza</td>
                                            <td>h.benali@etu.ma</td>
                                            <td>R140025688</td>
                                            <td>Building a web app</td>
                                        </tr>
                                        <tr class="text-muted fst-italic">
                                            <td>R140025688</td>
                                            <td>EL OUALI</td>
                                            <td>Sara</td>
                                            <td>s.elouali@etu.ma</td>
                                            <td>R140025687</td>
                                            <td>Building a web app</td>
                                        </tr>
                                        <tr class="text-muted fst-italic">
                                            <td>R123456789</td>
                                            <td>SOLO</td>
                                            <td>Han</td>
                                            <td>h.solo@etu.ma</td>
                                            <td><em>(vide)</em></td>
                                            <td>Création d'un Chatbot IA</td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                            <div class="alert alert-warning py-1 px-2 mt-2 mb-0 small">
                                <i class="fa-solid fa-triangle-exclamation me-1"></i>
                                <strong>Attention :</strong> La filière est déduite du nom du fichier (ex: <code>GI.xlsx</code>, <code>ID.xlsx</code>, <code>TDIA.xlsx</code>).
                            </div>
                        </div>
                    </div>

                    <!-- FORMAT PROFESSEURS -->
                    <div class="col-md-5">
                        <div class="border rounded p-3 bg-white">
                            <div class="d-flex justify-content-between align-items-end mb-2">
                                <div>
                                    <h6 class="fw-bold mb-1"><i class="fa-solid fa-chalkboard-user text-success me-1"></i> Format — Fichier Professeurs</h6>
                                </div>
                                <a href="templateProfs.do" class="btn btn-sm btn-outline-success"><i class="fa-solid fa-download me-1"></i>Modèle</a>
                            </div>
                            <div class="table-responsive">
                                <table class="table table-bordered table-sm text-center mb-0" style="font-size:0.82rem;">
                                    <thead class="table-dark">
                                        <tr>
                                            <th>A — Colonne 1</th>
                                            <th>B — Colonne 2</th>
                                            <th>C — Colonne 3</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr class="table-light fw-semibold">
                                            <td><span class="badge bg-secondary">NOM</span></td>
                                            <td><span class="badge bg-secondary">PRÉNOM</span></td>
                                            <td><span class="badge bg-secondary">SPÉCIALITÉ</span></td>
                                        </tr>
                                        <tr class="text-muted fst-italic">
                                            <td>AMRANI</td>
                                            <td>Karim</td>
                                            <td>Informatique</td>
                                        </tr>
                                        <tr class="text-muted fst-italic">
                                            <td>ZOUAK</td>
                                            <td>Fatima</td>
                                            <td>Réseaux</td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>

        <div class="row">

            <!-- ETUDIANTS -->
            <div class="col-md-6">
                <form action="uploadEtudiants.do" method="post" enctype="multipart/form-data">
                    <label>Fichier Etudiants</label>
                    <input type="file" name="files" multiple class="form-control mb-2" required>
                    <button class="btn btn-primary w-100">Upload Etudiants</button>
                </form>
            </div>

            <!-- PROFS -->
            <div class="col-md-6">
                <form action="uploadProfs.do" method="post" enctype="multipart/form-data">
                    <label>Fichier Professeurs</label>
                    <input type="file" name="files" class="form-control mb-2" required>
                    <button class="btn btn-success w-100">Upload Professeurs</button>
                </form>
            </div>

        </div>
    </div>


    <!-- ================= LISTES ================= -->
    <div class="card p-3 mb-4">

        <h5>Listes disponibles</h5>

        <c:choose>
            <c:when test="${empty fichiers}">
                <div class="alert alert-warning">Aucune liste importee</div>
            </c:when>

            <c:otherwise>
                <form id="selectionForm">

                    <div class="d-flex flex-wrap gap-2 mb-3">
                        <c:forEach var="f" items="${fichiers}">
                            <label class="fichier-item">
                                <input type="checkbox"
                                       name="selectedFilieres"
                                       value="${f.filiere}">
                                <span class="
                                    ${f.filiere == 'GI' ? 'badge-GI' :
                                      f.filiere == 'ID' ? 'badge-ID' :
                                      f.filiere == 'TDIA' ? 'badge-TDIA' : 'bg-secondary text-white'} badge">
                                    ${f.filiere}
                                </span>
                                <strong>${f.nomFichier}</strong>
                                <small>(${f.nbEtudiants} etudiants)</small>
                            </label>
                        </c:forEach>
                    </div>

                    <!-- ACTIONS -->
                    <div class="d-flex gap-3">

                        <button type="submit"
                                formaction="lancerAffectation.do"
                                formmethod="post"
                                class="btn btn-success"
                                onclick="return confirm('Confirmer le lancement de l\'affectation (cela écrasera les précédentes) ?')">
                            Lancer Affectation
                        </button>

                        <button type="submit"
                                formaction="supprimerListes.do"
                                formmethod="post"
                                class="btn btn-danger"
                                onclick="return confirm('Supprimer les listes sélectionnées ?')">
                            Supprimer
                        </button>

                    </div>

                </form>
            </c:otherwise>
        </c:choose>

    </div>

    <!-- ================= TELECHARGEMENT APRES AFFECTATION ================= -->
    <c:if test="${affectationDone == true || sessionScope.affectationDone == true}">
        <div class="alert alert-success text-center mt-4">
            <h5>Affectation effectuée avec succès</h5>
            <p>
                Les affectations ont été enregistrées en base. Vous pouvez exporter le résultat
                ou générer le planning des soutenances.
            </p>

            <div class="mt-3 d-flex flex-wrap gap-2 justify-content-center">
                <form action="exportPdf.do" method="post" class="d-inline"
                      onsubmit="return confirm('Voulez-vous télécharger le rapport au format PDF ?')">
                    <button class="btn btn-danger">
                        <i class="fa-solid fa-file-pdf me-1"></i>Télécharger PDF
                    </button>
                </form>

                <form action="exportDocx.do" method="post" class="d-inline"
                      onsubmit="return confirm('Voulez-vous télécharger le rapport au format Word ?')">
                    <button class="btn btn-primary">
                        <i class="fa-solid fa-file-word me-1"></i>Télécharger Word
                    </button>
                </form>

                <a href="planning.do" class="btn btn-success">
                    <i class="fa-solid fa-calendar-check me-1"></i>Voir / Générer le Planning
                </a>
            </div>
        </div>
        <%-- Clear session flag after showing it --%>
        <c:remove var="affectationDone" scope="session" />
    </c:if>

    <!-- Historique des affectations -->
    <div class="card p-4 mt-4">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h5 class="fw-bold mb-0"><i class="fa-solid fa-clock-rotate-left text-primary me-2"></i> Historique des affectations</h5>
            <c:if test="${not empty historyFiles}">
                <a href="clearHistory.do?type=affectation" class="btn btn-sm btn-outline-danger" onclick="return confirm('Supprimer TOUT l\'historique des affectations ?')">
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

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>