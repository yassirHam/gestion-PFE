<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="d-flex flex-column flex-shrink-0 p-3 text-white bg-dark shadow-lg sidebar-container" style="position: sticky; top: 0; height: 100vh; overflow-y: auto; overflow-x: hidden; z-index: 1000;">
    <a href="index.jsp" class="d-flex align-items-center mb-3 me-md-auto text-white text-decoration-none border-bottom border-secondary pb-3 w-100 logo-container">
        <c:choose>
            <c:when test="${not empty appSettings and appSettings.hasLogo()}">
                <img src="logo.do" alt="Logo" style="height: 32px; margin-right: 12px; object-fit: contain;" class="sidebar-icon">
            </c:when>
            <c:otherwise>
                <i class="fa-solid fa-graduation-cap text-primary sidebar-icon" style="font-size: 24px; margin-right: 12px; width: 24px; text-align: center;"></i>
            </c:otherwise>
        </c:choose>
        <span class="fs-6 fw-bold text-truncate sidebar-text"><c:out value="${empty appSettings.institutionName ? 'Gestion PFE' : appSettings.institutionName}"/></span>
    </a>

    <ul class="nav nav-pills flex-column mb-auto gap-1">
        <li class="nav-item">
            <a href="dashboard.do" class="nav-link d-flex align-items-center ${activeTab == 'dashboard' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-chart-pie sidebar-icon"></i> <span class="sidebar-text">Dashboard</span>
            </a>
        </li>
        <li class="nav-item">
            <a href="affectation.do" class="nav-link d-flex align-items-center ${activeTab == 'affectation' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-users sidebar-icon"></i> <span class="sidebar-text">Affectation</span>
            </a>
        </li>
        <li class="nav-item">
            <a href="config.do" class="nav-link d-flex align-items-center ${activeTab == 'config' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-sliders sidebar-icon"></i> <span class="sidebar-text">Configuration</span>
            </a>
        </li>
        <li class="nav-item">
            <a href="planning.do" class="nav-link d-flex align-items-center ${activeTab == 'planning' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-calendar-days sidebar-icon"></i> <span class="sidebar-text">Planning</span>
            </a>
        </li>
        <li class="nav-item">
            <a href="pv.do" class="nav-link d-flex align-items-center ${activeTab == 'pv' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-file-lines sidebar-icon"></i> <span class="sidebar-text">PVs</span>
            </a>
        </li>

        <li class="mt-2 small text-uppercase text-secondary px-3 sidebar-text-group">Gouvernance</li>
        <li class="nav-item">
            <a href="profGovernance.do" class="nav-link d-flex align-items-center ${activeTab == 'governance' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-chalkboard-user sidebar-icon"></i> <span class="sidebar-text">Professeurs</span>
            </a>
        </li>
        <li class="nav-item">
            <a href="profAvailability.do" class="nav-link d-flex align-items-center text-white">
                <i class="fa-solid fa-calendar-xmark sidebar-icon"></i> <span class="sidebar-text">Indispos profs</span>
            </a>
        </li>

        <li class="mt-2 small text-uppercase text-secondary px-3 sidebar-text-group">Administration</li>
        <li class="nav-item">
            <a href="audit.do" class="nav-link d-flex align-items-center ${activeTab == 'audit' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-clipboard-list sidebar-icon"></i> <span class="sidebar-text">Journal d'audit</span>
            </a>
        </li>

        <li class="nav-item mt-2">
            <a href="settings.do" class="nav-link d-flex align-items-center ${activeTab == 'settings' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-gear sidebar-icon"></i> <span class="sidebar-text">Paramètres</span>
            </a>
        </li>
    </ul>
</div>

<style>
    /* Sidebar Collapsible Styles */
    .sidebar-container {
        width: 72px;
        transition: width 0.3s ease;
    }
    .sidebar-container:hover {
        width: 250px;
    }
    .sidebar-icon {
        width: 24px;
        text-align: center;
        margin-right: 12px;
        flex-shrink: 0;
    }
    .sidebar-text {
        opacity: 0;
        white-space: nowrap;
        transition: opacity 0.2s ease;
        visibility: hidden;
    }
    .sidebar-text-group {
        opacity: 0;
        white-space: nowrap;
        transition: opacity 0.2s ease;
        overflow: hidden;
        height: 0;
        margin-top: 0 !important;
        margin-bottom: 0 !important;
    }
    .sidebar-container:hover .sidebar-text,
    .sidebar-container:hover .sidebar-text-group {
        opacity: 1;
        visibility: visible;
    }
    .sidebar-container:hover .sidebar-text-group {
        height: auto;
        margin-top: 0.5rem !important;
        margin-bottom: 0.25rem !important;
    }
    .logo-container {
        overflow: hidden;
        white-space: nowrap;
    }
    
    /* Scrollbar */
    .sidebar-container::-webkit-scrollbar { width: 6px; }
    .sidebar-container::-webkit-scrollbar-track { background: transparent; }
    .sidebar-container::-webkit-scrollbar-thumb { background: #495057; border-radius: 3px; }
    .sidebar-container::-webkit-scrollbar-thumb:hover { background: #6c757d; }
    
    .nav-pills .nav-link { transition: all 0.2s ease-in-out; border-radius: 8px; padding: 0.45rem 0.75rem; font-size: 0.92rem; }
    .nav-pills .nav-link:hover:not(.active) { background-color: rgba(255,255,255,0.1); transform: translateX(4px); }
    body { margin: 0; overflow-x: hidden; }
    .main-content-wrapper { height: 100vh; overflow-y: auto; background-color: #f8f9fa; }
</style>
