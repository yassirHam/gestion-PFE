<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="d-flex flex-column flex-shrink-0 p-3 text-white bg-dark shadow-lg sidebar-container" style="width: 250px; position: sticky; top: 0; height: 100vh; overflow-y: auto;">
    <a href="index.jsp" class="d-flex align-items-center mb-4 me-md-auto text-white text-decoration-none border-bottom border-secondary pb-3 w-100">
        <c:choose>
            <c:when test="${not empty appSettings and appSettings.hasLogo()}">
                <img src="logo.do" alt="Logo" style="height: 32px; margin-right: 12px; object-fit: contain;">
            </c:when>
            <c:otherwise>
                <i class="fa-solid fa-graduation-cap me-2 text-primary" style="font-size: 24px;"></i>
            </c:otherwise>
        </c:choose>
        <span class="fs-5 fw-bold text-truncate"><c:out value="${empty appSettings.institutionName ? 'Gestion PFE' : appSettings.institutionName}"/></span>
    </a>
    
    <ul class="nav nav-pills flex-column mb-auto gap-2">
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
        <li class="nav-item">
            <a href="settings.do" class="nav-link ${activeTab == 'settings' ? 'active' : 'text-white'}">
                <i class="fa-solid fa-gear me-2"></i> Paramètres
            </a>
        </li>
    </ul>
</div>

<style>
    /* Custom scrollbar for sidebar */
    .sidebar-container::-webkit-scrollbar {
        width: 6px;
    }
    .sidebar-container::-webkit-scrollbar-track {
        background: transparent;
    }
    .sidebar-container::-webkit-scrollbar-thumb {
        background: #495057;
        border-radius: 3px;
    }
    .sidebar-container::-webkit-scrollbar-thumb:hover {
        background: #6c757d;
    }
    
    .nav-pills .nav-link {
        transition: all 0.2s ease-in-out;
        border-radius: 8px;
    }
    .nav-pills .nav-link:hover:not(.active) {
        background-color: rgba(255,255,255,0.1);
        transform: translateX(4px);
    }
    
    /* Global layout adjustments to work with sidebar */
    body {
        margin: 0;
        overflow-x: hidden;
    }
    
    .main-content-wrapper {
        height: 100vh;
        overflow-y: auto;
        background-color: #f8f9fa;
    }
</style>
