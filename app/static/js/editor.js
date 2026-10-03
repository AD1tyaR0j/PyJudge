(function () {
  "use strict";

  const P = window.PROBLEM;
  const MONACO = "https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.45.0/min";
  const csrf = document.querySelector('meta[name="csrf-token"]').content;
  const draftKey = "pyjudge:draft:" + P.slug;
  const $ = (sel) => document.querySelector(sel);

  // ---------- storage (may be unavailable in private windows) ----------
  function loadDraft() { try { return localStorage.getItem(draftKey); } catch (e) { return null; } }
  function saveDraft(code) { try { localStorage.setItem(draftKey, code); } catch (e) { /* ignore */ } }

  const initialCode = loadDraft() ?? P.lastCode ?? P.starter;

  // ---------- editor (Monaco, with a plain textarea fallback) ----------
  let getCode, setCode;
  const host = $("#editor");

  function useTextarea() {
    host.innerHTML = "";
    const ta = document.createElement("textarea");
    ta.className = "fallback mono";
    ta.spellcheck = false;
    ta.value = initialCode;
    ta.addEventListener("keydown", (e) => {
      if (e.key === "Tab") {
        e.preventDefault();
        const s = ta.selectionStart;
        ta.setRangeText("    ", s, ta.selectionEnd, "end");
      }
    });
    ta.addEventListener("input", () => saveDraft(ta.value));
    host.appendChild(ta);
    getCode = () => ta.value;
    setCode = (v) => { ta.value = v; };
  }

  useTextarea(); // usable immediately; upgraded to Monaco once it loads

  if (window.require && window.require.config) {
    window.MonacoEnvironment = {
      getWorkerUrl: () =>
        "data:text/javascript;charset=utf-8," +
        encodeURIComponent(
          `self.MonacoEnvironment={baseUrl:'${MONACO}/'};importScripts('${MONACO}/vs/base/worker/workerMain.js');`
        ),
    };
    require.config({ paths: { vs: MONACO + "/vs" } });
    require(["vs/editor/editor.main"], function () {
      const current = getCode();
      host.innerHTML = "";
      monaco.editor.defineTheme("nebula", {
        base: "vs-dark", inherit: true,
        rules: [
          { token: "keyword", foreground: "c792ff" },
          { token: "string", foreground: "2de2c4" },
          { token: "number", foreground: "ffb547" },
          { token: "comment", foreground: "6b7094", fontStyle: "italic" },
        ],
        colors: {
          "editor.background": "#0d0e1a",
          "editor.lineHighlightBackground": "#171930",
          "editorLineNumber.foreground": "#4a4e6e",
          "editorLineNumber.activeForeground": "#b9a6ff",
          "editor.selectionBackground": "#8b6cff40",
          "editorCursor.foreground": "#2de2c4",
        },
      });
      const editor = monaco.editor.create(host, {
        value: current,
        language: "python",
        theme: "nebula",
        automaticLayout: true,
        fontSize: 14,
        tabSize: 4,
        insertSpaces: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        padding: { top: 12 },
      });
      editor.onDidChangeModelContent(() => saveDraft(editor.getValue()));
      editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, runCode);
      editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.Enter, submitCode);
      getCode = () => editor.getValue();
      setCode = (v) => editor.setValue(v);
      editor.focus();
    }, function () { /* CDN unreachable: keep the textarea */ });
  }

  // ---------- tabs ----------
  function showTab(name) {
    document.querySelectorAll(".tabs button").forEach((b) => b.classList.toggle("active", b.dataset.tab === name));
    document.querySelectorAll(".tab-panel").forEach((p) => p.classList.toggle("active", p.dataset.panel === name));
  }
  document.querySelectorAll(".tabs button").forEach((b) => b.addEventListener("click", () => showTab(b.dataset.tab)));

  document.querySelectorAll("[data-use-input]").forEach((btn) => {
    btn.addEventListener("click", () => {
      $("#stdin").value = document.querySelectorAll(".sample-in")[+btn.dataset.useInput].textContent;
      showTab("input");
      if (window.FX) FX.play($("#stdin"), "flash-input");
    });
  });

  $("#reset-btn").addEventListener("click", () => {
    if (confirm("Replace your code with the starter code?")) { setCode(P.starter); saveDraft(P.starter); }
  });

  // ---------- helpers ----------
  function esc(s) {
    return String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  }

  function badge(v) {
    return `<span class="badge v-${esc(v.toLowerCase())}">${esc(P.labels[v] || v)}</span>`;
  }

  async function api(path, options = {}) {
    const res = await fetch("/api" + path, {
      ...options,
      headers: { "Content-Type": "application/json", "X-CSRF-Token": csrf, ...(options.headers || {}) },
    });
    let data = {};
    try { data = await res.json(); } catch (e) { /* non-JSON error page */ }
    if (!res.ok) throw new Error(data.error || `Request failed (${res.status})`);
    return data;
  }

  function judgingHtml(text) {
    return `<div class="judging"><span class="dots"><span></span><span></span><span></span></span><span>${esc(text)}</span></div>` +
      `<div class="test-progress"><span></span></div>`;
  }

  function busy(isBusy) {
    ["#run-btn", "#submit-btn"].forEach((id) => { const b = $(id); if (b) b.disabled = isBusy; });
  }

  // ---------- run ----------
  async function runCode() {
    if (!P.loggedIn || $("#run-btn").disabled) return;
    busy(true);
    showTab("output");
    $("#run-output").innerHTML = judgingHtml("Running your code…");
    try {
      const r = await api("/run", { method: "POST", body: JSON.stringify({ code: getCode(), input: $("#stdin").value, problem_id: P.id }) });
      const statusText = { OK: "Finished", TLE: "Time Limit Exceeded", MLE: "Memory Limit Exceeded", RE: "Runtime Error", CE: "Syntax Error" }[r.status] || r.status;
      const ok = r.status === "OK";
      let html = `<div class="result-enter"><div class="result-head"><strong class="verdict-pop" style="color:var(--${ok ? "green" : "red"})">${esc(statusText)}</strong>`;
      html += `<span class="muted small">${r.time_ms} ms</span></div>`;
      html += `<div class="lbl small muted">stdout</div><pre>${esc(r.stdout) || '<span class="muted">(no output)</span>'}</pre>`;
      if (r.stderr) html += `<div class="lbl small muted" style="margin-top:10px">stderr</div><pre style="color:var(--red)">${esc(r.stderr)}</pre>`;
      $("#run-output").innerHTML = html + "</div>";
      if (!ok && window.FX) FX.play($("#run-output .result-head"), "shake");
    } catch (e) {
      $("#run-output").innerHTML = `<span style="color:var(--red)">${esc(e.message)}</span>`;
    } finally {
      busy(false);
    }
  }

  // ---------- submit ----------
  async function submitCode() {
    if (!P.loggedIn || $("#submit-btn").disabled) return;
    busy(true);
    showTab("result");
    $("#submit-result").innerHTML = judgingHtml("Submitting…");
    try {
      let sub = await api("/submit", { method: "POST", body: JSON.stringify({ code: getCode(), problem_id: P.id }) });
      if (sub.verdict === "PENDING") $("#submit-result").innerHTML = judgingHtml(`Judging against ${sub.total} test cases…`);
      while (sub.verdict === "PENDING") {
        await new Promise((r) => setTimeout(r, 700));
        sub = await api("/submissions/" + sub.submission_id);
      }
      renderResult(sub);
    } catch (e) {
      $("#submit-result").innerHTML = `<span style="color:var(--red)">${esc(e.message)}</span>`;
    } finally {
      busy(false);
    }
  }

  function renderResult(s) {
    const link = P.submissionUrl.replace(/0$/, s.submission_id);
    const accepted = s.verdict === "AC";
    let html = `<div class="result-enter"><div class="result-head${accepted ? " verdict-glow-ac" : ""}">`;
    html += `<span class="big verdict-pop">${badge(s.verdict)}</span>`;
    html += `<span>${s.passed}/${s.total} tests passed</span>`;
    if (s.runtime_ms != null) html += `<span class="muted small">max ${s.runtime_ms} ms</span>`;
    html += `<a class="small" href="${esc(link)}">Submission #${s.submission_id}</a></div>`;
    html += `<div class="bar" style="margin-bottom:12px"><span style="width:${s.total ? (100 * s.passed / s.total) : 0}%;` +
            `background:var(--${accepted ? "green" : "red"})"></span></div>`;
    if (accepted) html += `<p style="color:var(--green);margin:0">🎉 All test cases passed. Nice work!</p>`;
    if (s.error) html += `<pre>${esc(s.error)}</pre>`;
    $("#submit-result").innerHTML = html + "</div>";

    if (!window.FX) return;
    if (accepted) FX.confetti($("#submit-result .verdict-pop"));
    else if (s.verdict !== "IE") FX.play($("#submit-result .result-head"), "shake");
  }

  const runBtn = $("#run-btn"), submitBtn = $("#submit-btn");
  if (runBtn) runBtn.addEventListener("click", runCode);
  if (submitBtn) submitBtn.addEventListener("click", submitCode);

  // Shortcuts for the textarea fallback (Monaco registers its own)
  document.addEventListener("keydown", (e) => {
    if (!(e.ctrlKey || e.metaKey) || e.key !== "Enter" || !e.target.classList.contains("fallback")) return;
    e.preventDefault();
    e.shiftKey ? submitCode() : runCode();
  });
})();
