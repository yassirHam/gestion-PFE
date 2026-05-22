<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    if (request.getAttribute("appSettings") == null) {
        try {
            request.setAttribute("appSettings", services.AppSettingsService.getInstance().get());
        } catch (Exception ignored) {}
    }
%>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Connexion — <c:out value="${empty appSettings.institutionName ? 'Gestion PFE' : appSettings.institutionName}"/></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <style>
        body {
            background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .login-card {
            background: white;
            border-radius: 16px;
            padding: 2.5rem;
            box-shadow: 0 20px 50px rgba(0,0,0,0.3);
            width: 100%;
            max-width: 420px;
        }
        .brand-row {
            text-align: center;
            margin-bottom: 1.5rem;
        }
        .brand-row img { height: 56px; }
        .brand-row .icon-fallback {
            font-size: 3rem;
            color: #4338ca;
        }
        .form-control {
            padding: 0.75rem 1rem;
            border-radius: 10px;
        }
        .btn-login {
            background: linear-gradient(135deg, #4338ca, #6366f1);
            color: white;
            border: none;
            padding: 0.75rem;
            border-radius: 10px;
            font-weight: 600;
        }
        .hint {
            font-size: 0.8rem;
            color: #64748b;
            text-align: center;
            margin-top: 1rem;
        }
    </style>
</head>
<body>
    <div class="login-card">
        <div class="brand-row">
            <c:choose>
                <c:when test="${not empty appSettings and appSettings.hasLogo()}">
                    <img src="logo.do" alt="Logo">
                </c:when>
                <c:otherwise>
                    <i class="fa-solid fa-graduation-cap icon-fallback"></i>
                </c:otherwise>
            </c:choose>
            <h4 class="mt-3 mb-1 fw-bold"><c:out value="${empty appSettings.institutionName ? 'Gestion PFE' : appSettings.institutionName}"/></h4>
            <c:if test="${not empty appSettings.institutionSubtitle}">
                <p class="text-muted small mb-0"><c:out value="${appSettings.institutionSubtitle}"/></p>
            </c:if>
        </div>

        <c:if test="${not empty loginError}">
            <div class="alert alert-danger small mb-3">
                <i class="fa-solid fa-circle-exclamation me-1"></i><c:out value="${loginError}"/>
            </div>
        </c:if>
        <c:if test="${not empty flashOk}">
            <div class="alert alert-success small mb-3"><c:out value="${flashOk}"/></div>
        </c:if>

        <form method="post" action="login.do">
            <div class="mb-3">
                <label class="form-label small text-uppercase fw-semibold text-muted">Identifiant</label>
                <input type="text" name="username" class="form-control" required autofocus
                       value="<c:out value='${attemptedUsername}'/>">
            </div>
            <div class="mb-3">
                <label class="form-label small text-uppercase fw-semibold text-muted">Mot de passe</label>
                <input type="password" name="password" class="form-control" required>
            </div>
            <button type="submit" class="btn btn-login w-100">
                <i class="fa-solid fa-right-to-bracket me-1"></i> Se connecter
            </button>
        </form>

        <div class="hint">
            Premier démarrage ? Connectez-vous avec <strong>admin / admin</strong> et changez le mot de passe immédiatement.
        </div>
    </div>
</body>
</html>
