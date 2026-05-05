<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html>
<head>
    <title>Affectation</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
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

<div class="container mt-5">

    <h2 class="mb-4 text-center">Affectation des encadrants</h2>

    <!-- DEBUG -->
    <c:if test="${not empty debug}">
        <div class="alert alert-info">
            <ul class="mb-0">
                <c:forEach var="d" items="${debug}">
                    <li>${d}</li>
                </c:forEach>
            </ul>
        </div>
    </c:if>

    <!-- ================= UPLOAD ================= -->
    <div class="card p-3 mb-4">
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
    <c:if test="${affectationDone == true}">
        <div class="alert alert-success text-center mt-4">
            <h5>Affectation effectuée avec succès</h5>
            <p>
                Les affectations ont été enregistrées en base. Vous pouvez exporter le résultat.
            </p>

            <div class="mt-3">
                <form action="exportPdf.do" method="post" class="d-inline"
                      onsubmit="return confirm('Voulez-vous télécharger le rapport au format PDF ?')">
                    <button class="btn btn-danger">
                        Télécharger PDF
                    </button>
                </form>

                <form action="exportDocx.do" method="post" class="d-inline"
                      onsubmit="return confirm('Voulez-vous télécharger le rapport au format Word ?')">
                    <button class="btn btn-primary">
                        Télécharger Word
                    </button>
                </form>
            </div>
        </div>
    </c:if>

</div>

</body>
</html>