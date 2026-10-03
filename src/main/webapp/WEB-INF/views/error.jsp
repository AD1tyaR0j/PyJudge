<%@ page isErrorPage="true" %>
<c:set var="code" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="msg" value="${requestScope['jakarta.servlet.error.message']}"/>
<t:page title="${code == 404 ? 'Not found' : code == 403 ? 'Forbidden' : 'Error'}" width="medium">
<div class="panel" style="margin-top:40px">
  <div class="panel-body">
    <p class="mono faint" style="margin:0">HTTP ${code}</p>
    <h1>
      <c:choose>
        <c:when test="${code == 404}">Page not found</c:when>
        <c:when test="${code == 403}">You don't have access to this page</c:when>
        <c:when test="${code == 400}">Bad request</c:when>
        <c:otherwise>Something went wrong</c:otherwise>
      </c:choose>
    </h1>
    <c:if test="${not empty msg && code != 500}"><p class="muted">${h:e(msg)}</p></c:if>
    <div class="row"><a class="btn" href="/">Home</a><a class="btn" href="/dashboard">Dashboard</a></div>
  </div>
</div>
</t:page>
