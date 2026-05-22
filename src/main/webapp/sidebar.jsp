<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="d-flex flex-column flex-shrink-0 p-3 text-white bg-dark shadow-lg sidebar-container" style="width: 250px; position: sticky; top: 0; height: 100vh; overflow-y: auto;">
    <a href="index.jsp" class="d-flex align-items-center mb-3 me-md-auto text-white text-decoration-none border-bottom border-secondary pb-3 w-100">
        <c:choose>
            <c:when test="${not empty appSettings and appSettings.hasLogo()}">
                <img src="logo.do" alt="Logo" style="height: 32px; margin-right: 12px; object-fit: contain;">
            </c:when>
            <c:otherwise>
                <i class="fa-solid fa-graduation-cap me-2 text-primary" style="font-size: 24px;"></i>
            </c:otherwise>
        </c:choose>
        <span class="fs-6 fw-bold text-truncate"><c:out value="${empty appSettings.institutionName ? 'Gestion PFE' : appSettings.institutionName}"/></span>
    </a>

    <c:if test="${not empty currentUser}">
        <div class="small text-secondary mb-2 px-1">
            <i class="fa-solid fa-circle-user me-1"></i>
            <c:out value="${currentUser.displayName}"/><br>
            <span class="badge bg-secondary"><c:out value="${currentUser.role.label}"/></span>
            <c:if test="${not empty activeSession}">
                <div class="mt-1 text-truncate" title="${activeSession.displayName}">
                    <i class="fa-solid fa-flag me-1"></i><c:out value="${activeSession.displayName}"/>
                </div>
            </c:if>
            <c:if test="${not empty currentVersion}">
                <div class="text-truncate" title="${currentVersion.displayName}">
                    <i class="fa-solid fa-code-branch me-1"></i>
                    <c:out value="${currentVersion.displayName}"/>
                    <span class="badge bg-${currentVersion.state.bootstrap}"><c:out value="${currentVersion.state.label}"/></span>
                </div>
            </c:if>
        </div>
    </c:if>

    <ul class="nav nav-pills flex-column mb-auto gap-1">
        <li class="nav-item">
            <a href="dashboard.do" class="nav-link ${activeTab == 'dashboard' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-chart-pie me-2"></i> Dashboard
            </a>
        </li>
        <li class="nav-item">
            <a href="affectation.do" class="nav-link ${activeTab == 'affectation' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-users me-2"></i> Affectation
            </a>
        </li>
        <li class="nav-item">
            <a href="config.do" class="nav-link ${activeTab == 'config' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-sliders me-2"></i> Configuration
            </a>
        </li>
        <li class="nav-item">
            <a href="planning.do" class="nav-link ${activeTab == 'planning' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-calendar-days me-2"></i> Planning
            </a>
        </li>
        <li class="nav-item">
            <a href="pv.do" class="nav-link ${activeTab == 'pv' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-file-lines me-2"></i> PVs
            </a>
        </li>

        <c:if test="${not empty currentUser}">
            <li class="mt-2 small text-uppercase text-secondary px-3">Operations</li>
            <li class="nav-item">
                <a href="sessions.do" class="nav-link ${activeTab == 'sessions' ? 'active' : 'text-white'}">
                    <i class="fa-solid fa-flag me-2"></i> Sessions / Versions
                </a>
            </li>
            <li class="nav-item">
                <a href="approvals.do" class="nav-link ${activeTab == 'approvals' ? 'active' : 'text-white'}">
                    <i class="fa-solid fa-circle-check me-2"></i> Approbations
                </a>
            </li>
            <li class="nav-item">
                <a href="exceptions.do" class="nav-link ${activeTab == 'exceptions' ? 'active' : 'text-white'}">
                    <i class="fa-solid fa-triangle-exclamation me-2"></i> Incidents
                </a>
            </li>
            <li class="nav-item">
                <a href="notifications.do" class="nav-link ${activeTab == 'notifications' ? 'active' : 'text-white'}">
                    <i class="fa-solid fa-paper-plane me-2"></i> Convocations
                </a>
            </li>
        </c:if>

        <c:if test="${not empty currentUser and (currentUser.role == 'ADMIN_PEDAGOGIQUE' or currentUser.role == 'CHEF_DEPARTEMENT' or currentUser.role == 'COORDINATEUR_FILIERE')}">
            <li class="mt-2 small text-uppercase text-secondary px-3">Gouvernance</li>
            <li class="nav-item">
                <a href="departments.do" class="nav-link ${activeTab == 'governance' ? 'active' : 'text-white'}">
                    <i class="fa-solid fa-building me-2"></i> Départements
                </a>
            </li>
            <li class="nav-item">
                <a href="profGovernance.do" class="nav-link text-white">
                    <i class="fa-solid fa-chalkboard-user me-2"></i> Professeurs
                </a>
            </li>
            <li class="nav-item">
                <a href="profAvailability.do" class="nav-link text-white">
                    <i class="fa-solid fa-calendar-xmark me-2"></i> Indispos profs
                </a>
            </li>
            <li class="nav-item">
                <a href="salleGovernance.do" class="nav-link text-white">
                    <i class="fa-solid fa-door-open me-2"></i> Salles
                </a>
            </li>
        </c:if>

        <c:if test="${not empty currentUser and (currentUser.role == 'ADMIN_PEDAGOGIQUE' or currentUser.role == 'CHEF_DEPARTEMENT')}">
            <li class="nav-item">
                <a href="audit.do" class="nav-link ${activeTab == 'audit' ? 'active' : 'text-white'}">
                    <i class="fa-solid fa-clipboard-list me-2"></i> Journal d'audit
                </a>
            </li>
        </c:if>

        <c:if test="${not empty currentUser and currentUser.role == 'ADMIN_PEDAGOGIQUE'}">
            <li class="mt-2 small text-uppercase text-secondary px-3">Administration</li>
            <li class="nav-item">
                <a href="users.do" class="nav-link ${activeTab == 'users' ? 'active' : 'text-white'}">
                    <i class="fa-solid fa-users-gear me-2"></i> Utilisateurs
                </a>
            </li>
        </c:if>

        <li class="nav-item mt-2">
            <a href="settings.do" class="nav-link ${activeTab == 'settings' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-gear me-2"></i> Paramètres
            </a>
        </li>

        <c:if test="${not empty currentUser}">
            <li class="nav-item mt-2 pt-2 border-top border-secondary">
                <a href="logout.do" class="nav-link text-warning">
                    <i class="fa-solid fa-right-from-bracket me-2"></i> Déconnexion
                </a>
            </li>
        </c:if>
    </ul>
</div>

<style>
    .sidebar-container::-webkit-scrollbar { width: 6px; }
    .sidebar-container::-webkit-scrollbar-track { background: transparent; }
    .sidebar-container::-webkit-scrollbar-thumb { background: #495057; border-radius: 3px; }
    .sidebar-container::-webkit-scrollbar-thumb:hover { background: #6c757d; }
    .nav-pills .nav-link { transition: all 0.2s ease-in-out; border-radius: 8px; padding: 0.45rem 0.75rem; font-size: 0.92rem; }
    .nav-pills .nav-link:hover:not(.active) { background-color: rgba(255,255,255,0.1); transform: translateX(4px); }
    body { margin: 0; overflow-x: hidden; }
    .main-content-wrapper { height: 100vh; overflow-y: auto; background-color: #f8f9fa; }
</style>
