/* Home page hero interactions: particle field, watchful snake, bouncy letters,
   magnetic buttons, and tilting cards. Everything stays still under reduced motion. */
(function () {
  "use strict";

  // ui.js loads deferred (after this script), so read the ✨ toggle directly instead of window.FX
  const motionPref = document.documentElement.dataset.motion;
  const reduce = motionPref === "off" ||
    (motionPref !== "on" && window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  const hero = document.getElementById("hero");
  if (!hero) return;
  const css = getComputedStyle(document.documentElement);
  const rgb = (name) => css.getPropertyValue(name).trim() || "139, 108, 255";
  const COLORS = [rgb("--accent-rgb"), rgb("--accent-2-rgb"), rgb("--accent-3-rgb")];

  // ---------- split "Write. Run." into hoverable letters ----------
  document.querySelectorAll(".split-text").forEach((el) => {
    const text = el.textContent;
    el.setAttribute("aria-label", text);
    el.textContent = "";
    [...text].forEach((ch, i) => {
      const s = document.createElement("span");
      s.className = "char";
      s.setAttribute("aria-hidden", "true");
      s.style.setProperty("--ci", i);
      s.textContent = ch;
      el.appendChild(s);
    });
  });

  // ---------- snake: looks toward the cursor, spins and talks when clicked ----------
  const snake = hero.querySelector(".hero-snake");
  const bubble = hero.querySelector(".snake-bubble");
  const QUIPS = [
    "Hiss… ready to write some Python? 🐍",
    "import this → “Simple is better than complex.”",
    "Tip: start with FizzBuzz, then try Two Sum.",
    "Readability counts. So do test cases. ✔",
    "I'm not venomous, just very well indented.",
    "import antigravity 🚀",
    "There should be one obvious way to do it.",
  ];
  let quip = 0, bubbleTimer = 0;
  if (snake) {
    snake.addEventListener("click", () => {
      snake.classList.remove("spin");
      void snake.offsetWidth;
      snake.classList.add("spin");
      if (window.FX) FX.confetti(snake);
      bubble.textContent = QUIPS[quip++ % QUIPS.length];
      bubble.classList.add("show");
      clearTimeout(bubbleTimer);
      bubbleTimer = setTimeout(() => bubble.classList.remove("show"), 2600);
    });
  }

  if (reduce) {
    drawStaticField();
    return;
  }

  // ---------- magnetic buttons ----------
  hero.querySelectorAll(".magnetic").forEach((btn) => {
    btn.addEventListener("pointermove", (e) => {
      const r = btn.getBoundingClientRect();
      const dx = e.clientX - (r.left + r.width / 2);
      const dy = e.clientY - (r.top + r.height / 2);
      btn.style.transform = `translate(${dx * 0.22}px, ${dy * 0.35}px)`;
    });
    btn.addEventListener("pointerleave", () => { btn.style.transform = ""; });
  });

  // ---------- 3D tilt cards ----------
  document.querySelectorAll(".tilt").forEach((card) => {
    card.addEventListener("pointermove", (e) => {
      const r = card.getBoundingClientRect();
      const x = (e.clientX - r.left) / r.width - 0.5;
      const y = (e.clientY - r.top) / r.height - 0.5;
      card.classList.add("tilting");
      card.style.transform = `perspective(700px) rotateX(${(-y * 8).toFixed(2)}deg) rotateY(${(x * 10).toFixed(2)}deg) translateY(-4px)`;
    });
    card.addEventListener("pointerleave", () => {
      card.classList.remove("tilting");
      card.style.transform = "";
    });
  });

  // ---------- particle field ----------
  const canvas = hero.querySelector(".hero-canvas");
  const ctx = canvas.getContext("2d");
  const GLYPHS = ["def", "for", "in", "if", "{}", "()", "[]", ":", "==", ">>>", "λ", "#", "print", "len", "True", "None", "+=", "%"];
  const LINK_DIST = 115, MOUSE_DIST = 170, REPEL_DIST = 110;
  let W = 0, H = 0, dpr = 1, particles = [], running = false, visible = true, raf = 0;
  const mouse = { x: -9999, y: -9999, active: false };

  function makeParticle(x, y, burst) {
    const glyph = Math.random() < 0.38 || burst;
    const angle = Math.random() * Math.PI * 2;
    const speed = burst ? 2.5 + Math.random() * 4 : 0.12 + Math.random() * 0.3;
    return {
      x, y, vx: Math.cos(angle) * speed, vy: Math.sin(angle) * speed,
      text: glyph ? GLYPHS[(Math.random() * GLYPHS.length) | 0] : null,
      size: glyph ? 11 + Math.random() * 6 : 1.4 + Math.random() * 1.8,
      color: COLORS[(Math.random() * COLORS.length) | 0],
      alpha: glyph ? 0.35 + Math.random() * 0.3 : 0.5 + Math.random() * 0.4,
      life: burst ? 1 : null, // burst particles fade out
    };
  }

  function resize() {
    const r = canvas.getBoundingClientRect();
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    W = r.width; H = r.height;
    canvas.width = W * dpr; canvas.height = H * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    const target = Math.round(Math.min(90, Math.max(30, (W * H) / 13000)));
    particles = particles.filter((p) => p.life === null).slice(0, target);
    while (particles.length < target) particles.push(makeParticle(Math.random() * W, Math.random() * H, false));
  }

  function step() {
    ctx.clearRect(0, 0, W, H);
    // links between nearby particles
    for (let i = 0; i < particles.length; i++) {
      const a = particles[i];
      for (let j = i + 1; j < particles.length; j++) {
        const b = particles[j];
        const dx = a.x - b.x, dy = a.y - b.y, d2 = dx * dx + dy * dy;
        if (d2 < LINK_DIST * LINK_DIST) {
          const k = 1 - Math.sqrt(d2) / LINK_DIST;
          ctx.strokeStyle = `rgba(${a.color}, ${0.18 * k * (a.life ?? 1)})`;
          ctx.lineWidth = 1;
          ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke();
        }
      }
    }
    // links to the cursor
    if (mouse.active) {
      for (const p of particles) {
        const dx = p.x - mouse.x, dy = p.y - mouse.y, d = Math.hypot(dx, dy);
        if (d < MOUSE_DIST) {
          ctx.strokeStyle = `rgba(${COLORS[1]}, ${0.45 * (1 - d / MOUSE_DIST)})`;
          ctx.lineWidth = 1.2;
          ctx.beginPath(); ctx.moveTo(mouse.x, mouse.y); ctx.lineTo(p.x, p.y); ctx.stroke();
        }
      }
      const g = ctx.createRadialGradient(mouse.x, mouse.y, 0, mouse.x, mouse.y, 90);
      g.addColorStop(0, `rgba(${COLORS[1]}, .14)`); g.addColorStop(1, "rgba(0,0,0,0)");
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(mouse.x, mouse.y, 90, 0, Math.PI * 2); ctx.fill();
    }
    // move + draw
    for (let i = particles.length - 1; i >= 0; i--) {
      const p = particles[i];
      if (mouse.active) {
        const dx = p.x - mouse.x, dy = p.y - mouse.y, d = Math.hypot(dx, dy) || 1;
        if (d < REPEL_DIST) {
          const f = (1 - d / REPEL_DIST) * 0.6;
          p.vx += (dx / d) * f; p.vy += (dy / d) * f;
        }
      }
      p.x += p.vx; p.y += p.vy;
      if (p.life !== null) {
        p.vx *= 0.95; p.vy *= 0.95; p.life -= 0.012;
        if (p.life <= 0) { particles.splice(i, 1); continue; }
      } else {
        // ease back toward a gentle drift speed after being pushed
        const sp = Math.hypot(p.vx, p.vy);
        if (sp > 0.45) { p.vx *= 0.94; p.vy *= 0.94; }
        if (p.x < -20) p.x = W + 20; else if (p.x > W + 20) p.x = -20;
        if (p.y < -20) p.y = H + 20; else if (p.y > H + 20) p.y = -20;
      }
      const a = p.alpha * (p.life ?? 1);
      if (p.text) {
        ctx.font = `600 ${p.size}px "JetBrains Mono", Consolas, monospace`;
        ctx.fillStyle = `rgba(${p.color}, ${a})`;
        ctx.fillText(p.text, p.x, p.y);
      } else {
        ctx.fillStyle = `rgba(${p.color}, ${a})`;
        ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2); ctx.fill();
      }
    }
    raf = requestAnimationFrame(step);
  }

  function start() { if (!running && visible && !document.hidden) { running = true; raf = requestAnimationFrame(step); } }
  function stop() { running = false; cancelAnimationFrame(raf); }

  function local(e) {
    const r = canvas.getBoundingClientRect();
    return { x: e.clientX - r.left, y: e.clientY - r.top };
  }
  hero.addEventListener("pointermove", (e) => { Object.assign(mouse, local(e), { active: true }); lookAt(e); });
  hero.addEventListener("pointerleave", () => { mouse.active = false; if (snake) snake.style.setProperty("--look", "0deg"); });
  hero.addEventListener("pointerdown", (e) => {
    if (e.target.closest("a, button, input")) return;
    const { x, y } = local(e);
    for (let i = 0; i < 16; i++) particles.push(makeParticle(x, y, true));
  });

  function lookAt(e) {
    if (!snake) return;
    const r = snake.getBoundingClientRect();
    const dx = e.clientX - (r.left + r.width / 2);
    const dy = e.clientY - (r.top + r.height / 2);
    const deg = Math.max(-25, Math.min(25, dx / 12 + dy / 30));
    snake.style.setProperty("--look", `${deg.toFixed(1)}deg`);
  }

  new IntersectionObserver(([entry]) => { visible = entry.isIntersecting; visible ? start() : stop(); }).observe(hero);
  document.addEventListener("visibilitychange", () => (document.hidden ? stop() : start()));
  window.addEventListener("resize", resize);
  resize();
  start();

  // Reduced motion: a still constellation, no loop, no cursor effects
  function drawStaticField() {
    const c = hero.querySelector(".hero-canvas");
    if (!c) return;
    const x = c.getContext("2d");
    const r = c.getBoundingClientRect();
    c.width = r.width; c.height = r.height;
    for (let i = 0; i < 40; i++) {
      x.fillStyle = `rgba(${COLORS[i % 3]}, .45)`;
      x.beginPath(); x.arc(Math.random() * r.width, Math.random() * r.height, 1.6, 0, Math.PI * 2); x.fill();
    }
  }
})();
