<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>Your set</title>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/images/logo96x96.png" type="image/png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/webjars/bootstrap/5.3.2/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/webjars/font-awesome/6.5.1/css/all.min.css">
    <link href="https://cdn.jsdelivr.net/npm/simple-datatables@7.1.2/dist/style.min.css" rel="stylesheet" />
    <script src="${pageContext. request. contextPath}/webjars/bootstrap/5.3.2/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext. request. contextPath}/webjars/jquery/3.7.1/jquery.min.js"></script>
</head>
<body>
<%--header--%>
<jsp:include page="../../../components/header.jsp"></jsp:include>
<main>
    <div class="container" style="margin-top: 96px">
        <table class="table mb-4" id="sets-table">
            <thead>
            <tr>
                <th scope="col">No.</th>
                <th scope="col">Name of set</th>
                <th scope="col">Actions</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${listSet}" var="c" varStatus="loop">
                <tr>
                    <th scope="row">${loop.index + 1}</th>
                    <td><a class="text-decoration-none fs-4 text-black w-100" href="./set/get?setId=${c.getSId()}">${c.getSName()}</a></td>
                    <td>
                        <a href="./set/update?setId=${c.getSId()}" class="btn btn-primary ms-1">Edit</a>
                        <a href="./set/delete?setId=${c.getSId()}" class="btn btn-danger ms-1">Delete</a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</main><!-- End #main -->

<jsp:include page="../../../components/footer.jsp"></jsp:include>
<script src="https://cdn.jsdelivr.net/npm/simple-datatables@7.1.2/dist/umd/simple-datatables.min.js"
        crossorigin="anonymous"></script>
<script src="${pageContext.request.contextPath}/js/ManageSet.js"></script>
</body>
</html>