<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html>
<head>
    <title>Affectation</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">

    <style>
        .badge-GI { background-color: #0d6efd; color: white; }
        .badge-ID { background-color: #ffc107; color: black; }
        .badge-TDIA { background-color: #198754; color: white; }

        .fichier-item {
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 8px 12px;
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
            <ul>
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
                                class="btn btn-danger">
                            Lancer Affectation
                        </button>

                        <button type="submit"
                                formaction="supprimerListes.do"
                                formmethod="post"
                                class="btn btn-dark"
                                onclick="return confirm('Supprimer les listes ?')">
                            Supprimer
                        </button>

                    </div>

                </form>

            </c:otherwise>
        </c:choose>

    </div>

    <!-- ================= RESULTAT ================= -->

    <c:if test="${not empty grouped}">

        <div class="card p-4 shadow-sm">

            <h4>Resultat de l'affectation</h4>

            <c:forEach var="entry" items="${grouped}">

                <div class="mb-4 p-3 border rounded">

                    <!-- ENCadrant -->
                    <h5 class="text-primary">
                        ${entry.key.nom} ${entry.key.prenom}
                    </h5>

                    <!-- TABLE -->
                    <table class="table table-bordered mt-2">

                        <thead class="table-dark">
                            <tr>
                                <th>Filiere</th>
                                <th>Etudiant</th>
                            </tr>
                        </thead>

                        <tbody>
                        <c:forEach var="a" items="${entry.value}">
                            <tr class="
                                ${a.etudiant.filiere == 'GI' ? 'table-primary' :
                                  a.etudiant.filiere == 'ID' ? 'table-warning' :
                                  a.etudiant.filiere == 'TDIA' ? 'table-success' : ''}">
                                <td>${a.etudiant.filiere}</td>
                                <td>${a.etudiant.nomE} ${a.etudiant.prenomE}</td>
                            </tr>
                        </c:forEach>
                        </tbody>

                    </table>

                </div>

            </c:forEach>

            <!-- EXPORT -->
            <div class="text-center mt-3">

                <form action="exportPdf.do" method="post" class="d-inline">
                    <button class="btn btn-outline-danger">
                        Telecharger PDF
                    </button>
                </form>

                <form action="exportDocx.do" method="post" class="d-inline">
                    <button class="btn btn-outline-primary">
                        Telecharger Word
                    </button>
                </form>

            </div>

        </div>

    </c:if>

</div>

</body>
</html>