<%@ tag pageEncoding="UTF-8" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="width" %>
<%@ attribute name="head" fragment="true" %>
<%@ attribute name="scripts" fragment="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="h" uri="https://pyjudge/functions" %>
<c:set var="u" value="${requestScope.currentUser}"/>
<c:set var="p" value="${requestScope.path}"/>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="csrf-token" content="${requestScope.csrfToken}">
  <title>${h:e(title)} · ${h:e(site.site_name)}</title>
  <link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'%3E%3Crect width='32' height='32' rx='6' fill='%23f5a524'/%3E%3Ctext x='16' y='21' font-family='monospace' font-size='14' font-weight='700' text-anchor='middle' fill='%231b1303'%3Epy%3C/text%3E%3C/svg%3E">
  <link rel="stylesheet" href="/static/css/app.css">
  <jsp:invoke fragment="head"/>
</head>
<body>
<header class="topbar">
  <a class="logo" href="/"><span class="logo-mark">py</span>${h:e(site.site_name)}</a>
  <nav class="nav">
    <a href="/learn" class="${p.startsWith('/learn') ? 'on' : ''}">Learn</a>
    <a href="/problems" class="${p.startsWith('/problems') ? 'on' : ''}">Exercises</a>
    <a href="/challenges" class="${p.startsWith('/challenges') ? 'on' : ''}">Challenges</a>
    <c:if test="${site.leaderboard_enabled}"><a href="/leaderboard" class="${p == '/leaderboard' ? 'on' : ''}">Leaderboard</a></c:if>
    <c:if test="${u != null}">
      <a href="/dashboard" class="${p.startsWith('/student') || p.startsWith('/instructor') || p.startsWith('/admin') || p.startsWith('/studio') ? 'on' : ''}">${u.admin ? 'Admin' : u.instructor ? 'Instructor' : 'Dashboard'}</a>
    </c:if>
  </nav>
  <div class="top-right">
    <c:choose>
      <c:when test="${u != null}">
        <c:if test="${site.messaging_enabled || u.admin}">
          <a class="btn ghost sm" href="/messages">Messages<c:if test="${unreadMessages > 0}"><span class="count">${unreadMessages}</span></c:if></a>
        </c:if>
        <details class="account">
          <summary><span class="avatar">${h:e(u.initial)}</span><span class="who">${h:e(u.displayName)}</span></summary>
          <div class="menu">
            <div class="menu-head"><strong>${h:e(u.displayName)}</strong><div class="small muted">@${h:e(u.username)} · ${u.role.label}</div></div>
            <a href="/u/${h:e(u.username)}">Profile</a>
            <a href="/submissions">My submissions</a>
            <a href="/settings">Settings</a>
            <form method="post" action="/logout"><input type="hidden" name="csrf_token" value="${requestScope.csrfToken}"><button type="submit">Log out</button></form>
          </div>
        </details>
      </c:when>
      <c:otherwise>
        <a class="btn ghost sm" href="/login">Log in</a>
        <c:if test="${site.registration_open}"><a class="btn primary sm" href="/register">Sign up</a></c:if>
      </c:otherwise>
    </c:choose>
  </div>
</header>
<c:if test="${not empty site.announcement}"><div class="banner">${h:e(site.announcement)}</div></c:if>
<main class="page ${width}">
  <c:if test="${not empty sessionScope.flash}">
    <c:forEach var="f" items="${sessionScope.flash}">
      <div class="alert ${f[0]}" role="${f[0] == 'error' ? 'alert' : 'status'}"><span>${h:e(f[1])}</span><button class="x" type="button" aria-label="Dismiss">&times;</button></div>
    </c:forEach>
    <c:remove var="flash" scope="session"/>
  </c:if>
  <jsp:doBody/>
</main>
<script src="/static/js/app.js"></script>
<jsp:invoke fragment="scripts"/>
</body>
</html>
