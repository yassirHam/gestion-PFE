<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Paramètres - Gestion PFE</title>
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
        .section-title {
            font-size: 0.78rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: .08em;
            color: #64748b;
            margin-bottom: 0.75rem;
        }
        .logo-preview {
            border: 1px dashed #cbd5e1;
            border-radius: 12px;
            padding: 12px;
            background: #f8fafc;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 120px;
            min-height: 90px;
        }
        .logo-preview img { max-height: 80px; max-width: 200px; object-fit: contain; }
        .form-section { padding: 1rem 0; border-bottom: 1px solid #f1f5f9; }
        .form-section:last-child { border-bottom: none; }
    </style>
</head>
<body class="bg-light">
<c:set var="activeTab" value="settings" scope="request" />
<div class="d-flex flex-nowrap" style="min-height: 100vh;">
    <jsp:include page="sidebar.jsp" />
    <div class="main-content-wrapper flex-grow-1 p-4">

    <h2 class="fw-bold mb-4"><i class="fa-solid fa-gear me-2 text-primary"></i>Paramètres de l'application</h2>

    <c:if test="${not empty settingsFlash}">
        <div class="alert alert-${settingsFlashIsError ? 'danger' : 'success'} d-flex align-items-center">
            <i class="fa-solid ${settingsFlashIsError ? 'fa-triangle-exclamation' : 'fa-circle-check'} me-2"></i>
            <c:out value="${settingsFlash}"/>
        </div>
    </c:if>

    <!-- ───── Branding ─────────────────────────────────────────────────── -->
    <div class="card p-3 p-md-4">
        <h5 class="fw-bold mb-3"><i class="fa-solid fa-building text-primary me-2"></i>Identité de l'établissement</h5>
        <p class="text-muted small mb-3">Personnalisez le nom, la signature et le logo affichés dans l'application et sur les documents PDF / Word générés.</p>

        <form action="saveBranding.do" method="post" enctype="multipart/form-data">
            <div class="row g-3">
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">Nom de l'établissement</label>
                    <input type="text" name="institutionName" class="form-control"
                           value="${fn:escapeXml(appSettings.institutionName)}"
                           placeholder="Ex: Université de Casablanca">
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">Sous-titre / Département</label>
                    <input type="text" name="institutionSubtitle" class="form-control"
                           value="${fn:escapeXml(appSettings.institutionSubtitle)}"
                           placeholder="Ex: Faculté des Sciences - Département Informatique">
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">Année universitaire</label>
                    <input type="text" name="academicYear" class="form-control"
                           value="${fn:escapeXml(appSettings.academicYear)}"
                           placeholder="Ex: 2026/2027">
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">Titre des documents (PDF/DOCX)</label>
                    <input type="text" name="documentTitle" class="form-control"
                           value="${fn:escapeXml(appSettings.documentTitle)}"
                           placeholder="Ex: Affectation des encadrants de PFE">
                </div>

                <div class="col-12">
                    <label class="form-label small fw-semibold">Logo</label>
                    <div class="d-flex flex-wrap gap-3 align-items-center">
                        <div class="logo-preview">
                            <c:choose>
                                <c:when test="${appSettings.hasLogo()}">
                                    <img src="logo.do" alt="Logo actuel">
                                </c:when>
                                <c:otherwise>
                                    <span class="text-muted small"><i class="fa-regular fa-image me-1"></i>Aucun logo</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="flex-grow-1">
                            <input type="file" name="logo" accept="image/png,image/jpeg" class="form-control">
                            <div class="form-text">PNG ou JPEG, max ~2 Mo. Sera intégré aux exports PDF / Word.</div>
                            <c:if test="${appSettings.hasLogo()}">
                                <div class="form-check form-switch mt-2">
                                    <input class="form-check-input" type="checkbox" id="removeLogo" name="removeLogo" value="true">
                                    <label class="form-check-label small" for="removeLogo">Supprimer le logo actuel</label>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>

            <div class="text-end mt-3">
                <button class="btn btn-primary"><i class="fa-solid fa-floppy-disk me-1"></i>Enregistrer l'identité</button>
            </div>
        </form>
    </div>

    <!-- ───── Storage ──────────────────────────────────────────────────── -->
    <div class="card p-3 p-md-4">
        <h5 class="fw-bold mb-3"><i class="fa-solid fa-folder-tree text-primary me-2"></i>Stockage de l'historique</h5>
        <p class="text-muted small mb-3">
            L'historique stocke les fichiers PDF / Word des affectations et plannings générés.
            Choisissez un dossier local ou un stockage cloud compatible S3
            (AWS&nbsp;S3, MinIO, Wasabi, Cloudflare&nbsp;R2, Backblaze&nbsp;B2...).
        </p>

        <c:set var="status" value="${storageStatus}"/>
        <c:if test="${not empty status}">
            <div class="alert alert-${storageStatusOk ? 'success' : 'warning'} small">
                <i class="fa-solid ${storageStatusOk ? 'fa-circle-check' : 'fa-circle-exclamation'} me-2"></i>
                <strong>État:</strong> <c:out value="${status}"/>
            </div>
        </c:if>

        <form action="saveStorage.do" method="post">
            <div class="form-section">
                <div class="section-title">Type de stockage</div>
                <div class="form-check form-check-inline">
                    <input class="form-check-input" type="radio" name="storageMode" id="modeLocal" value="LOCAL"
                           ${appSettings.storageMode == 'LOCAL' ? 'checked' : ''} onclick="toggleStorageMode('LOCAL')">
                    <label class="form-check-label" for="modeLocal"><i class="fa-solid fa-hard-drive me-1"></i>Local</label>
                </div>
                <div class="form-check form-check-inline">
                    <input class="form-check-input" type="radio" name="storageMode" id="modeS3" value="S3"
                           ${appSettings.storageMode == 'S3' ? 'checked' : ''} onclick="toggleStorageMode('S3')">
                    <label class="form-check-label" for="modeS3"><i class="fa-solid fa-cloud me-1"></i>Cloud (S3 compatible)</label>
                </div>
            </div>

            <div id="localPanel" class="form-section">
                <div class="section-title">Dossier local</div>
                <label class="form-label small fw-semibold">Chemin absolu du dossier</label>
                <input type="text" name="localStoragePath" class="form-control"
                       value="${fn:escapeXml(appSettings.localStoragePath)}"
                       placeholder="Ex: /var/lib/gestion-pfe/historique ou C:\\plannings_history">
                <div class="form-text">Le dossier sera créé automatiquement s'il n'existe pas. Doit être lisible et inscriptible par le serveur Tomcat.</div>
            </div>

            <div id="s3Panel" class="form-section">
                <div class="section-title">Configuration S3 / Cloud</div>
                <div class="row g-3">
                    <div class="col-12 col-md-6">
                        <label class="form-label small fw-semibold">Endpoint</label>
                        <input type="text" name="s3Endpoint" class="form-control"
                               value="${fn:escapeXml(appSettings.s3Endpoint)}"
                               placeholder="Ex: https://s3.amazonaws.com ou https://minio.exemple.com">
                    </div>
                    <div class="col-12 col-md-3">
                        <label class="form-label small fw-semibold">Région</label>
                        <input type="text" name="s3Region" class="form-control"
                               value="${fn:escapeXml(appSettings.s3Region)}"
                               placeholder="us-east-1">
                    </div>
                    <div class="col-12 col-md-3">
                        <label class="form-label small fw-semibold">Bucket</label>
                        <input type="text" name="s3Bucket" class="form-control"
                               value="${fn:escapeXml(appSettings.s3Bucket)}"
                               placeholder="gestion-pfe-history">
                    </div>
                    <div class="col-12 col-md-6">
                        <label class="form-label small fw-semibold">Access Key</label>
                        <input type="text" name="s3AccessKey" class="form-control"
                               value="${fn:escapeXml(appSettings.s3AccessKey)}" autocomplete="off">
                    </div>
                    <div class="col-12 col-md-6">
                        <label class="form-label small fw-semibold">Secret Key</label>
                        <input type="password" name="s3SecretKey" class="form-control"
                               value="${fn:escapeXml(appSettings.s3SecretKey)}" autocomplete="new-password">
                        <div class="form-text">Laissez vide pour conserver la clé existante.</div>
                    </div>
                    <div class="col-12 col-md-9">
                        <label class="form-label small fw-semibold">Préfixe (optionnel)</label>
                        <input type="text" name="s3Prefix" class="form-control"
                               value="${fn:escapeXml(appSettings.s3Prefix)}"
                               placeholder="Ex: prod/historique">
                    </div>
                    <div class="col-12 col-md-3 d-flex align-items-end">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="s3PathStyle" name="s3PathStyleAccess" value="true"
                                   ${appSettings.s3PathStyleAccess ? 'checked' : ''}>
                            <label class="form-check-label small" for="s3PathStyle">Path-style access</label>
                        </div>
                    </div>
                </div>
                <div class="form-text mt-2">
                    <i class="fa-solid fa-circle-info me-1"></i>
                    Le mode <em>path-style</em> est conseillé pour MinIO et la plupart des services compatibles S3.
                    Pour AWS&nbsp;S3 récent, désactivez-le pour utiliser le DNS virtuel.
                </div>
            </div>

            <div class="d-flex flex-wrap gap-2 justify-content-end mt-3">
                <button type="submit" formaction="testStorage.do" formmethod="post" class="btn btn-outline-secondary">
                    <i class="fa-solid fa-plug-circle-check me-1"></i>Tester la connexion
                </button>
                <button class="btn btn-primary"><i class="fa-solid fa-floppy-disk me-1"></i>Enregistrer le stockage</button>
            </div>
        </form>
    </div>

    <!-- ───── NLP / AI ─────────────────────────────────────────────────── -->
    <div class="card p-3 p-md-4">
        <h5 class="fw-bold mb-3"><i class="fa-solid fa-wand-magic-sparkles text-primary me-2"></i>Service NLP (optionnel)</h5>
        <p class="text-muted small mb-3">
            Le service NLP analyse le sujet de chaque PFE pour rapprocher l'étudiant d'un professeur dont la spécialité correspond.
            Cette fonctionnalité est <strong>optionnelle</strong> : si elle est désactivée, le jury est constitué uniquement
            à partir de la discipline déclarée du professeur.
        </p>

        <form action="saveNlp.do" method="post">
            <div class="form-check form-switch mb-3">
                <input class="form-check-input" type="checkbox" id="nlpEnabled" name="nlpEnabled" value="true"
                       ${appSettings.nlpEnabled ? 'checked' : ''}>
                <label class="form-check-label fw-semibold" for="nlpEnabled">Activer le service NLP</label>
            </div>

            <div class="row g-3">
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">URL de base</label>
                    <input type="text" name="nlpBaseUrl" class="form-control"
                           value="${fn:escapeXml(appSettings.nlpBaseUrl)}"
                           placeholder="https://integrate.api.nvidia.com/v1">
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">Modèle</label>
                    <input type="text" name="nlpModel" class="form-control"
                           value="${fn:escapeXml(appSettings.nlpModel)}"
                           placeholder="meta/llama-3.3-70b-instruct">
                </div>
                <div class="col-12">
                    <label class="form-label small fw-semibold">Clé API</label>
                    <input type="password" name="nlpApiKey" class="form-control"
                           value="${fn:escapeXml(appSettings.nlpApiKey)}" autocomplete="new-password">
                    <div class="form-text">
                        Compatible avec toute API au format OpenAI (NVIDIA NIM, OpenAI, OpenRouter, ...).
                        Laissez vide si vous n'avez pas de clé : le service restera désactivé.
                    </div>
                </div>
            </div>

            <div class="text-end mt-3">
                <button class="btn btn-primary"><i class="fa-solid fa-floppy-disk me-1"></i>Enregistrer NLP</button>
            </div>
        </form>
    </div>

    <!-- ─── SMTP / notifications ────────────────────────────────────── -->
    <div class="card p-4">
        <div class="section-title"><i class="fa-solid fa-paper-plane me-1"></i> Notifications par e-mail (SMTP)</div>
        <p class="text-muted small mb-3">
            Configuration optionnelle. Quand SMTP est désactivé, les convocations
            sont produites et stockées en file pour consultation ; activez-le pour
            que l'application les expédie automatiquement.
        </p>
        <form action="saveSmtp.do" method="post">
            <div class="form-check form-switch mb-3">
                <input class="form-check-input" type="checkbox" id="smtpEnabled" name="smtpEnabled" value="true"
                       ${appSettings.smtpEnabled ? 'checked' : ''}>
                <label class="form-check-label fw-semibold" for="smtpEnabled">Activer l'envoi SMTP</label>
            </div>
            <div class="row g-3">
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">Hôte SMTP</label>
                    <input type="text" name="smtpHost" class="form-control"
                           value="${fn:escapeXml(appSettings.smtpHost)}" placeholder="smtp.gmail.com">
                </div>
                <div class="col-6 col-md-3">
                    <label class="form-label small fw-semibold">Port</label>
                    <input type="number" name="smtpPort" class="form-control"
                           value="${appSettings.smtpPort}" placeholder="587">
                </div>
                <div class="col-6 col-md-3 d-flex align-items-end">
                    <div class="form-check form-switch">
                        <input class="form-check-input" type="checkbox" id="smtpStartTls" name="smtpStartTls" value="true"
                               ${appSettings.smtpStartTls ? 'checked' : ''}>
                        <label class="form-check-label small" for="smtpStartTls">STARTTLS</label>
                    </div>
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">Utilisateur</label>
                    <input type="text" name="smtpUsername" class="form-control"
                           value="${fn:escapeXml(appSettings.smtpUsername)}">
                </div>
                <div class="col-12 col-md-6">
                    <label class="form-label small fw-semibold">Mot de passe</label>
                    <input type="password" name="smtpPassword" class="form-control" autocomplete="new-password"
                           placeholder="${empty appSettings.smtpPassword ? '(vide)' : '(inchangé si laissé vide)'}">
                </div>
                <div class="col-12">
                    <label class="form-label small fw-semibold">Adresse expéditeur</label>
                    <input type="email" name="smtpFrom" class="form-control"
                           value="${fn:escapeXml(appSettings.smtpFrom)}" placeholder="no-reply@etablissement.fr">
                </div>
            </div>
            <div class="text-end mt-3">
                <button class="btn btn-primary"><i class="fa-solid fa-floppy-disk me-1"></i>Enregistrer SMTP</button>
            </div>
        </form>
    </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function toggleStorageMode(mode) {
        document.getElementById('localPanel').style.display = (mode === 'LOCAL') ? '' : 'none';
        document.getElementById('s3Panel').style.display    = (mode === 'S3')    ? '' : 'none';
    }
    toggleStorageMode(document.querySelector('input[name="storageMode"]:checked').value);
</script>
    </div>
</div>
</body>
</html>
