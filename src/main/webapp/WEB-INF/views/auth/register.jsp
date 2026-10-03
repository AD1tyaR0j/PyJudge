<t:page title="Sign up" width="narrow">
<div class="panel auth-box">
  <div class="panel-body">
    <h1>Create your account</h1>
    <p class="muted">Student accounts are free. Instructor accounts are created by an administrator.</p>
    <c:forEach var="err" items="${errors}"><div class="alert error" role="alert"><span>${h:e(err)}</span></div></c:forEach>
    <form method="post" action="/register">
      <input type="hidden" name="csrf_token" value="${csrfToken}">
      <div class="field"><label for="name">Full name</label>
        <input id="name" name="name" type="text" value="${h:e(param.name)}" required maxlength="100" autocomplete="name" autofocus></div>
      <div class="field"><label for="username">Username <span class="hint">3–30 letters, numbers, or _</span></label>
        <input id="username" name="username" type="text" value="${h:e(param.username)}" required pattern="[A-Za-z0-9_]{3,30}" autocomplete="username"></div>
      <div class="field"><label for="email">Email</label>
        <input id="email" name="email" type="email" value="${h:e(param.email)}" required autocomplete="email"></div>
      <div class="field"><label for="password">Password <span class="hint">at least 8 characters</span></label>
        <input id="password" name="password" type="password" required minlength="8" autocomplete="new-password"></div>
      <div class="field"><label for="confirm">Confirm password</label>
        <input id="confirm" name="confirm" type="password" required minlength="8" autocomplete="new-password"></div>
      <button class="btn primary block" type="submit">Create account</button>
    </form>
  </div>
</div>
<p class="small muted" style="text-align:center; margin-top:14px">Already have an account? <a href="/login">Log in</a></p>
</t:page>
