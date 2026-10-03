/* Site-wide motion: reveal-on-scroll, count-up stats, toasts, ripples,
   card spotlight, typing hero, and confetti. Exposed as window.FX. */
(function () {
  "use strict";

  // Motion follows the OS "reduce motion" setting unless the ✨ toggle overrides it
  const root = document.documentElement;
  const systemReduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const reduceMotion = root.dataset.motion === "off" || (systemReduce && root.dataset.motion !== "on");

  function setupMotionToggle() {
    const btn = document.getElementById("motion-toggle");
    if (!btn) return;
    btn.setAttribute("aria-pressed", String(!reduceMotion));
    btn.title = reduceMotion ? "Animations off: click to turn on" : "Animations on: click to turn off";
    btn.addEventListener("click", () => {
      const next = reduceMotion ? "on" : "off";
      try { localStorage.setItem("pyjudge:motion", next); } catch (e) { /* ignore */ }
      root.dataset.motion = next;
      location.reload();
    });
  }

  // ---------- reveal on scroll, staggered per group ----------
  const REVEAL = "main .card, main h1, main h2, main .table-wrap, main .filters, tbody tr, main > .row, .sample";
  function setupReveal() {
    const items = [...document.querySelectorAll(REVEAL)]
      .filter((el) => !el.closest(".hero, .statement, .console, .no-reveal"));
    if (reduceMotion || !("IntersectionObserver" in window)) return;

    const groupIndex = new Map();
    items.forEach((el) => {
      const parent = el.parentElement;
      const i = groupIndex.get(parent) || 0;
      groupIndex.set(parent, i + 1);
      el.style.setProperty("--i", Math.min(i, 12));
      el.classList.add("reveal");
    });

    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => {
        if (!e.isIntersecting) return;
        const el = e.target;
        el.classList.add("in");
        io.unobserve(el);
        // Drop the reveal classes afterwards so the stagger delay can't slow hover effects
        const delay = 650 + (parseInt(el.style.getPropertyValue("--i"), 10) || 0) * 55;
        setTimeout(() => el.classList.remove("reveal", "in"), delay);
      });
    }, { threshold: 0.08, rootMargin: "0px 0px -30px 0px" });
    items.forEach((el) => io.observe(el));
  }

  // ---------- count-up numbers ----------
  function countUp(el) {
    const m = el.textContent.trim().match(/^([#]?)(\d+)(%?)$/);
    if (!m) return;
    const [, prefix, numStr, suffix] = m;
    const target = parseInt(numStr, 10);
    if (target === 0 || reduceMotion) return;
    const duration = Math.min(1400, 500 + target * 20);
    const start = performance.now();
    const step = (now) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      el.textContent = prefix + Math.round(target * eased) + suffix;
      if (t < 1) requestAnimationFrame(step);
    };
    el.textContent = prefix + "0" + suffix;
    requestAnimationFrame(step);
  }
  function setupCounters() {
    const nums = document.querySelectorAll(".stat .num");
    if (!("IntersectionObserver" in window)) return;
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => { if (e.isIntersecting) { countUp(e.target); io.unobserve(e.target); } });
    });
    nums.forEach((n) => io.observe(n));
  }

  // ---------- flash messages → dismissible toasts ----------
  function setupToasts() {
    document.querySelectorAll(".flash").forEach((flash, i) => {
      const btn = document.createElement("button");
      btn.className = "close"; btn.type = "button"; btn.setAttribute("aria-label", "Dismiss"); btn.textContent = "×";
      flash.appendChild(btn);
      const dismiss = () => {
        flash.classList.add("out");
        flash.addEventListener("animationend", () => flash.remove(), { once: true });
      };
      btn.addEventListener("click", dismiss);
      if (!flash.classList.contains("flash-error")) setTimeout(dismiss, 5000 + i * 400);
    });
  }

  // ---------- button ripple ----------
  function setupRipples() {
    document.addEventListener("pointerdown", (e) => {
      const btn = e.target.closest(".btn");
      if (!btn || btn.disabled || reduceMotion) return;
      const rect = btn.getBoundingClientRect();
      const size = Math.max(rect.width, rect.height);
      const r = document.createElement("span");
      r.className = "ripple";
      r.style.cssText = `width:${size}px;height:${size}px;left:${e.clientX - rect.left - size / 2}px;top:${e.clientY - rect.top - size / 2}px`;
      btn.appendChild(r);
      r.addEventListener("animationend", () => r.remove(), { once: true });
    });
  }

  // ---------- spotlight that follows the cursor on cards ----------
  function setupSpotlight() {
    document.addEventListener("pointermove", (e) => {
      const card = e.target.closest && e.target.closest(".card");
      if (!card) return;
      const rect = card.getBoundingClientRect();
      card.style.setProperty("--mx", `${e.clientX - rect.left}px`);
      card.style.setProperty("--my", `${e.clientY - rect.top}px`);
    }, { passive: true });
  }

  // ---------- backdrop parallax: orbs lean toward the cursor ----------
  function setupParallax() {
    if (reduceMotion || !document.querySelector(".backdrop")) return;
    let frame = 0;
    window.addEventListener("pointermove", (e) => {
      if (frame) return;
      frame = requestAnimationFrame(() => {
        frame = 0;
        root.style.setProperty("--px", ((e.clientX / innerWidth) * 2 - 1).toFixed(3));
        root.style.setProperty("--py", ((e.clientY / innerHeight) * 2 - 1).toFixed(3));
      });
    }, { passive: true });
  }

  // ---------- typing effect for the hero snippet ----------
  function setupTyping() {
    const el = document.querySelector("[data-typing]");
    if (!el) return;
    const lines = JSON.parse(el.dataset.typing);
    if (reduceMotion) { el.textContent = lines[0]; return; }
    let line = 0, pos = 0, deleting = false;
    const tick = () => {
      const text = lines[line];
      el.textContent = text.slice(0, pos);
      if (!deleting && pos < text.length) { pos++; setTimeout(tick, 55); }
      else if (!deleting) { deleting = true; setTimeout(tick, 1800); }
      else if (pos > 0) { pos--; setTimeout(tick, 25); }
      else { deleting = false; line = (line + 1) % lines.length; setTimeout(tick, 350); }
    };
    tick();
  }

  // ---------- confetti burst ----------
  function confetti(originEl) {
    if (reduceMotion) return;
    const rect = originEl ? originEl.getBoundingClientRect() : { left: innerWidth / 2, top: innerHeight / 2, width: 0, height: 0 };
    const ox = rect.left + rect.width / 2, oy = rect.top + rect.height / 2;
    const colors = ["#4f9cf9", "#ffd43b", "#3fb950", "#bc8cff", "#f0883e", "#ff6b9d"];
    for (let i = 0; i < 90; i++) {
      const p = document.createElement("div");
      p.className = "confetti";
      p.style.background = colors[i % colors.length];
      if (i % 3 === 0) p.style.borderRadius = "50%";
      document.body.appendChild(p);
      const angle = Math.random() * Math.PI * 2;
      const speed = 6 + Math.random() * 9;
      let x = ox, y = oy, vx = Math.cos(angle) * speed, vy = Math.sin(angle) * speed - 7;
      let rot = Math.random() * 360, vr = (Math.random() - 0.5) * 24, life = 0;
      const maxLife = 70 + Math.random() * 40;
      const frame = () => {
        life++;
        vy += 0.38; vx *= 0.985; x += vx; y += vy; rot += vr;
        p.style.transform = `translate(${x}px, ${y}px) rotate(${rot}deg)`;
        p.style.opacity = String(Math.max(0, 1 - life / maxLife));
        if (life < maxLife) requestAnimationFrame(frame); else p.remove();
      };
      requestAnimationFrame(frame);
    }
  }

  // Re-trigger a CSS animation class on an element
  function play(el, cls) {
    if (!el) return;
    el.classList.remove(cls);
    void el.offsetWidth;
    el.classList.add(cls);
  }

  window.FX = { confetti, play, reduceMotion };

  document.addEventListener("DOMContentLoaded", () => {
    setupMotionToggle();
    setupReveal();
    setupCounters();
    setupToasts();
    setupRipples();
    setupSpotlight();
    setupParallax();
    setupTyping();
  });
})();
