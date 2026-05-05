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
            border-radius: 8px;
            padding: 8px 12px;
            background: #f8f9fa;
        }

        .download-section {
            background: #f0fff4;
            border: 2px solid #198754;
            border-radius: 12px;
            padding: 24px;
            text-align: center;
        }

        .download-section h5 {
            color: #198754;
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
    <div class="card p-4 mb-4 shadow-sm">
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
    <div class="card p-4 mb-4 shadow-sm">

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
                                onclick="return confirm('⚠️ Lancer l\'affectation va écraser les affectations existantes pour les filières sélectionnées. Continuer ?')">
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
        <div class="download-section shadow-sm">
            <h5>✅ Affectation effectuée avec succès !</h5>
            <p class="text-muted mb-4">
                Les affectations ont été enregistrées. Choisissez le format pour télécharger le rapport.
            </p>

            <div class="d-flex justify-content-center gap-3">

                <form action="exportPdf.do" method="post" class="d-inline"
                      onsubmit="return confirm('📄 Vous allez télécharger le rapport PDF des affectations. Continuer ?')">
                    <button class="btn btn-danger btn-lg">
                        📄 Télécharger PDF
                    </button>
                </form>

                <form action="exportDocx.do" method="post" class="d-inline"
                      onsubmit="return confirm('📝 Vous allez télécharger le rapport Word (DOCX) des affectations. Continuer ?')">
                    <button class="btn btn-primary btn-lg">
                        📝 Télécharger Word
                    </button>
                </form>

            </div>
        </div>
    </c:if>

</div>

</body>
</html>