<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <title>Gestion PFE</title>

    <!-- Bootstrap CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>

<body class="bg-light">

<div class="container text-center mt-5">

    <h1 class="mb-5">Gestion des PFE</h1>

    <div class="row justify-content-center">

        <div class="col-md-3">
            <form action="affectation.do" method="get">
                <button class="btn btn-primary w-100 p-3">
                    Affectation
                </button>
            </form>
        </div>

        <div class="col-md-3">
            <form action="planning.do" method="get">
                <button class="btn btn-success w-100 p-3">
                    Planning
                </button>
            </form>
        </div>

        <div class="col-md-3">
            <form action="pvs.do" method="get">
                <button class="btn btn-warning w-100 p-3">
                    PVs
                </button>
            </form>
        </div>

    </div>

</div>

</body>
</html>