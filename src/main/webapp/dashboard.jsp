<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Dashboard - Gestion PFE</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- FontAwesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <!-- Chart.js -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

    <style>
        body {
            background-color: #f8f9fa;
            font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        
        .navbar-brand {
            font-weight: 700;
            letter-spacing: -0.5px;
        }
        
        .stat-card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.04);
            transition: transform 0.2s;
        }
        
        .stat-card:hover {
            transform: translateY(-5px);
        }
        
        .icon-box {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
        }
        
        .bg-primary-light { background-color: #e0e7ff; color: #4338ca; }
        .bg-success-light { background-color: #dcfce7; color: #15803d; }
        .bg-warning-light { background-color: #fef3c7; color: #b45309; }

        .chart-container {
            background: white;
            border-radius: 12px;
            padding: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.04);
            margin-bottom: 24px;
        }
    </style>
</head>
<body>

<!-- Navbar -->
<nav class="navbar navbar-expand-lg navbar-dark bg-primary mb-4 shadow-sm">
    <div class="container-fluid px-4">
        <a class="navbar-brand" href="index.jsp">
            <i class="fa-solid fa-graduation-cap me-2"></i>Gestion PFE
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link" href="affectation.do"><i class="fa-solid fa-users me-1"></i> Affectation</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="planning.do"><i class="fa-solid fa-calendar-days me-1"></i> Planning</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link active" href="dashboard.do"><i class="fa-solid fa-chart-pie me-1"></i> Dashboard</a>
                </li>
            </ul>
        </div>
    </div>
</nav>

<div class="container mb-5">
    
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold text-dark"><i class="fa-solid fa-chart-line me-2 text-primary"></i> Dashboard Analytique</h2>
        <div class="d-flex align-items-center">
            <form action="dashboard.do" method="get" class="d-flex me-3">
                <input type="text" name="q" class="form-control form-control-sm me-2" placeholder="Chercher un étudiant ou un prof..." value="${searchQuery}" required>
                <button type="submit" class="btn btn-sm btn-primary"><i class="fa-solid fa-search"></i></button>
            </form>
            <span class="badge bg-secondary p-2">Année 2025/2026</span>
        </div>
    </div>

    <!-- ===== RECHERCHE RESULTATS ===== -->
    <c:if test="${not empty searchResult}">
        <div class="card mb-4 border-primary shadow-sm">
            <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
                <h5 class="mb-0"><i class="fa-solid fa-magnifying-glass me-2"></i> Résultat de la recherche</h5>
                <a href="dashboard.do" class="text-white"><i class="fa-solid fa-xmark"></i></a>
            </div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${searchResult.searchType == 'NONE'}">
                        <div class="alert alert-warning mb-0"><i class="fa-solid fa-circle-exclamation me-2"></i> Aucun étudiant ou professeur trouvé pour "<strong>${searchQuery}</strong>".</div>
                    </c:when>

                    <c:when test="${searchResult.searchType == 'ETUDIANT'}">
                        <h5 class="text-primary fw-bold"><i class="fa-solid fa-user-graduate me-2"></i> Étudiant: ${searchResult.etu.nomE} ${searchResult.etu.prenomE}</h5>
                        <ul class="list-group list-group-flush">
                            <li class="list-group-item"><strong>Filière :</strong> ${searchResult.etu.filiere}</li>
                            <li class="list-group-item"><strong>Encadrant :</strong> ${searchResult.affectation.encadrant.nom} ${searchResult.affectation.encadrant.prenom}</li>
                            <c:if test="${not empty searchResult.soutenance}">
                                <li class="list-group-item list-group-item-success">
                                    <strong><i class="fa-solid fa-calendar-check text-success me-1"></i> Soutenance planifiée :</strong> 
                                    Le ${searchResult.soutenance.date} à ${searchResult.soutenance.heure} — Salle : ${searchResult.soutenance.salle.num_salle}
                                </li>
                            </c:if>
                            <c:if test="${empty searchResult.soutenance}">
                                <li class="list-group-item list-group-item-warning"><i class="fa-solid fa-clock text-warning me-1"></i> Soutenance non encore planifiée.</li>
                            </c:if>
                        </ul>
                    </c:when>

                    <c:when test="${searchResult.searchType == 'PROFESSEUR'}">
                        <h5 class="text-success fw-bold"><i class="fa-solid fa-chalkboard-user me-2"></i> Professeur: ${searchResult.prof.nom} ${searchResult.prof.prenom}</h5>
                        <div class="row mt-3">
                            <div class="col-md-6">
                                <h6 class="fw-bold border-bottom pb-2">Étudiants Encadrés (${searchResult.encadrements.size()})</h6>
                                <c:choose>
                                    <c:when test="${empty searchResult.encadrements}"><p class="text-muted small">Aucun étudiant encadré.</p></c:when>
                                    <c:otherwise>
                                        <ul class="list-group list-group-flush small">
                                            <c:forEach var="aff" items="${searchResult.encadrements}">
                                                <li class="list-group-item"><i class="fa-solid fa-user-graduate me-1 text-primary"></i> ${aff.etudiant.nomE} ${aff.etudiant.prenomE} (${aff.etudiant.filiere})</li>
                                            </c:forEach>
                                        </ul>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <div class="col-md-6">
                                <h6 class="fw-bold border-bottom pb-2">Soutenances Prévues (${searchResult.soutenances.size()})</h6>
                                <c:choose>
                                    <c:when test="${empty searchResult.soutenances}"><p class="text-muted small">Aucune soutenance prévue (ou planning non généré).</p></c:when>
                                    <c:otherwise>
                                        <ul class="list-group list-group-flush small">
                                            <c:forEach var="sout" items="${searchResult.soutenances}">
                                                <li class="list-group-item">
                                                    <strong>${sout.date} à ${sout.heure}</strong> (Salle ${sout.salle.num_salle})
                                                    <br><span class="text-muted">Étudiant: ${sout.etudiant.nomE} ${sout.etudiant.prenomE}</span>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:when>
                </c:choose>
            </div>
        </div>
    </c:if>

    <c:if test="${empty totalEtudiants || totalEtudiants == 0}">
        <div class="alert alert-warning shadow-sm border-0 rounded-3">
            <i class="fa-solid fa-circle-exclamation me-2"></i> Aucune donnée d'affectation disponible pour générer les statistiques. Veuillez d'abord <a href="affectation.do" class="alert-link">lancer une affectation</a>.
        </div>
    </c:if>

    <c:if test="${not empty totalEtudiants && totalEtudiants > 0}">
        
        <!-- Key Metrics -->
        <div class="row g-4 mb-4">
            <div class="col-md-4">
                <div class="card stat-card h-100 p-3">
                    <div class="d-flex align-items-center">
                        <div class="icon-box bg-primary-light me-3">
                            <i class="fa-solid fa-user-graduate"></i>
                        </div>
                        <div>
                            <h6 class="text-muted mb-1">Total Étudiants Affectés</h6>
                            <h3 class="mb-0 fw-bold">${totalEtudiants}</h3>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="col-md-4">
                <div class="card stat-card h-100 p-3">
                    <div class="d-flex align-items-center">
                        <div class="icon-box bg-success-light me-3">
                            <i class="fa-solid fa-chalkboard-user"></i>
                        </div>
                        <div>
                            <h6 class="text-muted mb-1">Professeurs Encadrants</h6>
                            <h3 class="mb-0 fw-bold">${totalProfs}</h3>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="col-md-4">
                <div class="card stat-card h-100 p-3">
                    <div class="d-flex align-items-center">
                        <div class="icon-box bg-warning-light me-3">
                            <i class="fa-solid fa-calendar-check"></i>
                        </div>
                        <div>
                            <h6 class="text-muted mb-1">Soutenances Planifiées</h6>
                            <h3 class="mb-0 fw-bold">${totalSoutenances}</h3>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Charts Row 1 -->
        <div class="row">
            <!-- Bar Chart: Etudiants par Professeur -->
            <div class="col-lg-8">
                <div class="chart-container">
                    <h5 class="mb-4 text-secondary"><i class="fa-solid fa-chart-column me-2"></i> Étudiants encadrés par Professeur</h5>
                    <canvas id="profChart" height="100"></canvas>
                </div>
            </div>
            
            <!-- Pie Chart: Etudiants/Soutenances par Filière -->
            <div class="col-lg-4">
                <div class="chart-container">
                    <h5 class="mb-4 text-secondary"><i class="fa-solid fa-chart-pie me-2"></i> Répartition par Filière</h5>
                    <canvas id="filiereChart" height="200"></canvas>
                </div>
            </div>
        </div>
        
        <!-- Charts Row 2 -->
        <div class="row mt-4">
            <!-- Bar Chart: Participations Jury -->
            <div class="col-lg-12">
                <div class="chart-container">
                    <h5 class="mb-4 text-secondary"><i class="fa-solid fa-users me-2"></i> Participations aux Jurys par Professeur</h5>
                    <canvas id="juryChart" height="80"></canvas>
                </div>
            </div>
        </div>
        
    </c:if>

</div>

<!-- Bootstrap JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

<!-- Chart.js Init -->
<c:if test="${not empty labelsProf}">
<script>
    document.addEventListener("DOMContentLoaded", function() {
        
        // Données injectées depuis le backend
        const labelsProf = ${labelsProf};
        const dataProf = ${dataProf};
        
        const labelsFil = ${labelsFil};
        const dataFil = ${dataFil};

        const labelsSoutProf = ${labelsSoutProf};
        const dataSoutProf = ${dataSoutProf};

        // --- Graphique Bar (Profs) ---
        const ctxProf = document.getElementById('profChart').getContext('2d');
        new Chart(ctxProf, {
            type: 'bar',
            data: {
                labels: labelsProf,
                datasets: [{
                    label: 'Nombre d\'étudiants',
                    data: dataProf,
                    backgroundColor: 'rgba(67, 56, 202, 0.7)',
                    borderColor: 'rgba(67, 56, 202, 1)',
                    borderWidth: 1,
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1 }
                    },
                    x: {
                        ticks: {
                            autoSkip: false,
                            maxRotation: 45,
                            minRotation: 45
                        }
                    }
                }
            }
        });

        // --- Graphique Pie (Filières) ---
        const ctxFil = document.getElementById('filiereChart').getContext('2d');
        new Chart(ctxFil, {
            type: 'doughnut',
            data: {
                labels: labelsFil,
                datasets: [{
                    data: dataFil,
                    backgroundColor: [
                        '#3b82f6', // bleu
                        '#10b981', // vert
                        '#f59e0b', // jaune
                        '#6366f1', // indigo
                        '#ef4444'  // rouge
                    ],
                    borderWidth: 0,
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                },
                cutout: '60%'
            }
        });

        // --- Graphique Bar (Jurys par Prof) ---
        const ctxJury = document.getElementById('juryChart').getContext('2d');
        new Chart(ctxJury, {
            type: 'bar',
            data: {
                labels: labelsSoutProf,
                datasets: [{
                    label: 'Nombre de participations aux jurys',
                    data: dataSoutProf,
                    backgroundColor: 'rgba(16, 185, 129, 0.7)', // vert
                    borderColor: 'rgba(16, 185, 129, 1)',
                    borderWidth: 1,
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1 }
                    },
                    x: {
                        ticks: {
                            autoSkip: false,
                            maxRotation: 45,
                            minRotation: 45
                        }
                    }
                }
            }
        });
        
    });
</script>
</c:if>

</body>
</html>
