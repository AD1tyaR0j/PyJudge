<t:page title="Log in" width="narrow">
<div class="panel auth-box">
  <div class="panel-body">
    <h1>Log in</h1>
    <p class="muted">Welcome back. Pick up where you left off.</p>
    <c:if test="${not empty error}"><div class="alert error" role="alert"><span>${h:e(error)}</span></div></c:if>
    <form method="post" action="/login">
      <input type="hidden" name="csrf_token" value="${csrfToken}">
      <input type="hidden" name="next" value="${h:e(param.next)}">
      <div class="field">
        <label for="identifier">Username or email</label>
        <input id="identifier" name="identifier" type="text" value="${h:e(identifier)}" required autocomplete="username"
               ${empty identifier ? 'autofocus' : ''}>
      </div>
      <div class="field">
        <label for="password">Password</label>
        <input id="password" name="password" type="password" required autocomplete="current-password" ${empty identifier ? '' : 'autofocus'}>
      </div>
      <div class="field"><label class="check"><input type="checkbox" name="remember" value="1"> Keep me logged in</label></div>
      <button class="btn primary block" type="submit">Log in</button>
    </form>
  </div>
</div>
<p class="small muted" style="text-align:center; margin-top:14px">
  <c:choose>
    <c:when test="${site.registration_open}">No account? <a href="/register">Create a student account</a></c:when>
    <c:otherwise>Need an account? Ask an administrator.</c:otherwise>
  </c:choose>
</p>
</t:page>
