/* Lesson pages: syntax highlighting, runnable playgrounds, quiz checking, reading progress. */
(function () {
  "use strict";
  const L = window.LESSON || {};
  const csrf = document.querySelector('meta[name="csrf-token"]').content;
  const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

  // ---------- highlight static code blocks ----------
  if (window.hljs) {
    document.querySelectorAll(".lesson-body pre code, .q-prompt pre code").forEach((el) => {
      if (!el.className) el.classList.add("language-python");
      hljs.highlightElement(el);
    });
  }

  // ---------- reading progress bar ----------
  const bar = document.querySelector(".read-progress span");
  const article = document.querySelector(".lesson-main");
  if (bar && article) {
    const update = () => {
      const r = article.getBoundingClientRect();
      const total = r.height - innerHeight;
      const pct = total > 0 ? Math.min(1, Math.max(0, -r.top / total)) : 1;
      bar.style.transform = `scaleX(${pct})`;
    };
    addEventListener("scroll", update, { passive: true });
    addEventListener("resize", update);
    update();
  }

  // ---------- playgrounds ----------
  function autosize(ta) {
    ta.style.height = "auto";
    ta.style.height = ta.scrollHeight + 2 + "px";
  }

  document.querySelectorAll(".playground").forEach((pg) => {
    const code = pg.querySelector(".pg-code");
    const stdin = pg.querySelector(".pg-stdin");
    const out = pg.querySelector(".pg-output");
    const runBtn = pg.querySelector(".pg-run");
    const original = code.value;

    autosize(code);
    code.addEventListener("input", () => autosize(code));
    code.addEventListener("keydown", (e) => {
      if (e.key === "Tab") {
        e.preventDefault();
        code.setRangeText("    ", code.selectionStart, code.selectionEnd, "end");
        autosize(code);
      } else if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
        e.preventDefault();
        run();
      }
    });
    pg.querySelector(".pg-reset").addEventListener("click", () => {
      code.value = original;
      autosize(code);
      out.hidden = true;
    });
    runBtn.addEventListener("click", run);

    async function run() {
      out.hidden = false;
      out.className = "pg-output";
      if (!L.loggedIn) {
        out.innerHTML = `<a href="${esc(L.loginUrl)}">Log in</a> to run code. (It's free!)`;
        return;
      }
      runBtn.disabled = true;
      out.innerHTML = '<span class="pg-spin"></span> Running…';
      try {
        const res = await fetch("/api/run", {
          method: "POST",
          headers: { "Content-Type": "application/json", "X-CSRF-Token": csrf },
          body: JSON.stringify({ code: code.value, input: stdin ? stdin.value : "" }),
        });
        const r = await res.json();
        if (!res.ok) throw new Error(r.error || `Request failed (${res.status})`);
        const ok = r.status === "OK";
        let html = r.stdout ? esc(r.stdout) : (ok ? '<span class="muted">(no output)</span>' : "");
        if (r.stderr) html += `<span class="pg-err">${esc(r.stderr)}</span>`;
        if (r.status === "TLE") html += '<span class="pg-err">⏱ Time limit exceeded</span>';
        out.innerHTML = html;
        out.classList.add(ok ? "ok" : "bad");
        if (window.FX && !ok) FX.play(out, "shake");
      } catch (err) {
        out.innerHTML = `<span class="pg-err">${esc(err.message)}</span>`;
      } finally {
        runBtn.disabled = false;
      }
    }
  });

  // ---------- quiz ----------
  const quiz = document.querySelector(".quiz-form");
  if (quiz) {
    quiz.addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = quiz.querySelector("button[type=submit]");
      const resultEl = quiz.querySelector(".quiz-result");
      btn.disabled = true;
      try {
        const res = await fetch(quiz.action, { method: "POST", body: new FormData(quiz) });
        const r = await res.json();
        if (!res.ok) throw new Error(r.error || "Something went wrong");

        r.results.forEach((item) => {
          const fs = quiz.querySelector(`.question[data-qid="${item.id}"]`);
          fs.classList.remove("right", "wrong");
          fs.classList.add(item.correct ? "right" : "wrong");
          fs.querySelectorAll(".option").forEach((label, i) => {
            label.classList.toggle("is-answer", i === item.answer);
            label.classList.toggle("is-wrong", i === item.chosen && !item.correct);
          });
          const ex = fs.querySelector(".explain");
          ex.hidden = false;
          ex.innerHTML = (item.correct ? "✅ Correct. " : "❌ Not quite. ") + item.explanation;
          if (window.FX) FX.play(fs, item.correct ? "verdict-pop" : "shake");
        });

        let msg = `<strong>${r.score}/${r.total}</strong> `;
        if (r.passed) {
          msg += r.saved ? "🎉 Lesson complete!" : "🎉 Nice! Log in to save your progress.";
          msg += ` <a class="btn btn-sm btn-success" href="${esc(r.next_url)}">Next lesson →</a>`;
          if (window.FX) FX.confetti(btn);
          document.querySelectorAll(".side-list li.current").forEach((li) => {
            li.classList.add("done");
            const mark = li.querySelector(".side-mark");
            if (mark && r.saved) mark.textContent = "✓";
          });
        } else {
          msg += "Review the explanations and try again. You need half right to complete the lesson.";
        }
        resultEl.innerHTML = msg;
        btn.textContent = "Try again";
      } catch (err) {
        resultEl.textContent = err.message;
      } finally {
        btn.disabled = false;
      }
    });
    // Changing an answer clears the old marking for that question
    quiz.addEventListener("change", (e) => {
      const fs = e.target.closest(".question");
      if (!fs) return;
      fs.classList.remove("right", "wrong");
      fs.querySelectorAll(".option").forEach((l) => l.classList.remove("is-answer", "is-wrong"));
      fs.querySelector(".explain").hidden = true;
    });
  }

  // ---------- mark complete (lessons without a quiz) ----------
  const done = document.querySelector(".complete-form");
  if (done) {
    done.addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = done.querySelector("button");
      const res = await fetch(done.action, { method: "POST", body: new FormData(done) });
      if (res.ok) {
        btn.textContent = "✓ Completed";
        btn.disabled = true;
        if (window.FX) FX.confetti(btn);
      }
    });
  }
})();
