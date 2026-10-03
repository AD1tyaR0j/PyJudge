/* Login & sign-up page behaviour: typing demo, password tools, submit state. */
(function () {
  "use strict";
  // ui.js loads deferred (after this script), so read the ✨ toggle directly instead of window.FX
  const motionPref = document.documentElement.dataset.motion;
  const reduce = motionPref === "off" ||
    (motionPref !== "on" && window.matchMedia("(prefers-reduced-motion: reduce)").matches);

  // ---------- code window: type each line, run, judge, repeat ----------
  const win = document.getElementById("code-window");
  if (win) {
    const CHAR_MS = 38, LINE_GAP = 180;
    let t = 400;
    win.querySelectorAll(".cw-line").forEach((line) => {
      const n = line.textContent.length;
      line.style.setProperty("--n", n);
      line.style.setProperty("--t", `${n * CHAR_MS}ms`);
      line.style.setProperty("--d", `${t}ms`);
      t += n * CHAR_MS + LINE_GAP;
    });
    win.style.setProperty("--run-at", `${t + 300}ms`);
    const cycle = t + 300 + 1300 + 3600; // typing + judging + time to admire the verdict

    const play = () => {
      win.classList.remove("play");
      void win.offsetWidth; // restart CSS animations
      win.classList.add("play");
    };
    play();
    if (!reduce) setInterval(play, cycle);
  }

  // After a failed login the username is kept, so put the cursor where it's needed
  const ident = document.getElementById("identifier");
  const loginPw = document.getElementById("password");
  if (ident && ident.value && loginPw) loginPw.focus();

  // ---------- show / hide password ----------
  document.querySelectorAll(".pw-toggle").forEach((btn) => {
    btn.addEventListener("click", () => {
      const input = document.getElementById(btn.getAttribute("aria-controls"));
      const show = input.type === "password";
      input.type = show ? "text" : "password";
      btn.classList.toggle("on", show);
      btn.setAttribute("aria-label", show ? "Hide password" : "Show password");
      input.focus();
    });
  });

  // ---------- caps lock warning ----------
  const caps = document.querySelector(".caps-hint");
  document.querySelectorAll('input[type="password"]').forEach((input) => {
    const check = (e) => { if (caps && e.getModifierState) caps.classList.toggle("on", e.getModifierState("CapsLock")); };
    input.addEventListener("keydown", check);
    input.addEventListener("keyup", check);
    input.addEventListener("blur", () => caps && caps.classList.remove("on"));
  });

  // ---------- sign-up: strength meter + confirm match ----------
  const pw = document.querySelector('input[name="password"][autocomplete="new-password"]');
  const meter = document.querySelector(".strength");
  const confirm = document.getElementById("confirm");
  const match = document.querySelector(".match-hint");
  const LABELS = ["Use 8+ characters with a mix of letters, numbers, and symbols",
                  "Weak: add more characters", "Fair: mix in numbers or symbols", "Good", "Strong 💪"];
  function score(v) {
    if (!v) return 0;
    let s = v.length >= 8 ? 1 : 0;
    if (v.length >= 12) s++;
    if (/[a-z]/.test(v) && /[A-Z]/.test(v)) s++;
    if (/\d/.test(v) && /[^A-Za-z0-9]/.test(v)) s++;
    return Math.max(1, Math.min(4, s));
  }
  function updateMatch() {
    if (!confirm || !match) return;
    if (!confirm.value) { match.textContent = ""; match.className = "match-hint"; return; }
    const ok = confirm.value === pw.value;
    match.textContent = ok ? "✓ Passwords match" : "✗ Passwords don't match yet";
    match.className = "match-hint " + (ok ? "ok" : "bad");
  }
  if (pw && meter) {
    pw.addEventListener("input", () => {
      const s = score(pw.value);
      meter.dataset.score = s;
      meter.querySelector(".strength-label").textContent = LABELS[pw.value ? s : 0];
      updateMatch();
    });
  }
  if (confirm) confirm.addEventListener("input", updateMatch);

  // ---------- submit: loading state, no double submits ----------
  document.querySelectorAll(".auth-form").forEach((form) => {
    form.addEventListener("submit", (e) => {
      if (confirm && pw && confirm.value !== pw.value) {
        e.preventDefault();
        updateMatch();
        if (window.FX) FX.play(document.querySelector(".auth-card"), "shake");
        confirm.focus();
        return;
      }
      const btn = form.querySelector(".btn-auth");
      if (btn.classList.contains("loading")) { e.preventDefault(); return; }
      btn.classList.add("loading");
    });
  });
  // Coming back via the browser's back button restores the page from cache with the spinner still on
  window.addEventListener("pageshow", () => {
    document.querySelectorAll(".btn-auth.loading").forEach((b) => b.classList.remove("loading"));
  });
})();
