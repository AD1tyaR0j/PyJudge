/* Thin wrapper around Chart.js so every dashboard chart shares one look.
   Palette: validated categorical slots for the dark surface (#11121e). */
(function () {
  "use strict";

  const SURFACE = "#11121e";
  const INK = "#eceefa";
  const MUTED = "#9196b0";
  const GRID = "rgba(236, 238, 250, 0.07)";
  const SLOTS = ["#3987e5", "#d95926", "#199e70"]; // blue, orange, aqua: fixed order, never cycled

  function base(Chart) {
    Chart.defaults.color = MUTED;
    Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif';
    Chart.defaults.font.size = 12;
    Chart.defaults.animation.duration = window.FX && FX.reduceMotion ? 0 : 900;
  }

  function scales(stacked, yMax, ySuffix) {
    return {
      x: {
        stacked, grid: { display: false }, border: { color: GRID },
        ticks: { color: MUTED, maxRotation: 0, autoSkipPadding: 12 },
      },
      y: {
        stacked, beginAtZero: true, max: yMax, grid: { color: GRID }, border: { display: false },
        ticks: { color: MUTED, precision: 0, padding: 6, callback: (v) => v + (ySuffix || "") },
      },
    };
  }

  function plugins(seriesCount, ySuffix) {
    return {
      legend: {
        display: seriesCount >= 2, position: "top", align: "start",
        labels: { color: INK, usePointStyle: true, pointStyle: "rectRounded", boxWidth: 10, boxHeight: 10, padding: 14 },
      },
      tooltip: {
        mode: "index", intersect: false, backgroundColor: "#090a13", borderColor: "#282b42", borderWidth: 1,
        titleColor: INK, bodyColor: INK, padding: 10, cornerRadius: 8, usePointStyle: true,
        callbacks: { label: (ctx) => ` ${ctx.dataset.label}: ${ctx.formattedValue}${ySuffix || ""}` },
      },
    };
  }

  function bar(id, cfg) {
    const el = document.getElementById(id);
    if (!el || !window.Chart) return;
    base(Chart);
    const stacked = !!cfg.stacked;
    new Chart(el, {
      type: "bar",
      data: {
        labels: cfg.labels,
        datasets: cfg.series.map((s, i) => ({
          label: s.label, data: s.data,
          backgroundColor: SLOTS[s.slot ?? i], hoverBackgroundColor: SLOTS[s.slot ?? i],
          // 4px rounding on the data end only; the baseline edge stays square
          borderRadius: stacked && i < cfg.series.length - 1 ? 0 : 4,
          borderSkipped: "start",
          // 2px surface spacer between stacked segments
          borderColor: SURFACE, borderWidth: stacked ? { top: 2 } : 0,
          maxBarThickness: 22, categoryPercentage: 0.7, barPercentage: 0.85,
        })),
      },
      options: {
        responsive: true, maintainAspectRatio: false, indexAxis: cfg.horizontal ? "y" : "x",
        interaction: { mode: "index", intersect: false },
        scales: cfg.horizontal ? swapAxes(scales(stacked, cfg.max, cfg.suffix)) : scales(stacked, cfg.max, cfg.suffix),
        plugins: plugins(cfg.series.length, cfg.suffix),
      },
    });
  }

  function swapAxes(s) {
    return {
      x: { ...s.y, grid: { color: GRID } },
      y: { ...s.x, grid: { display: false }, ticks: { color: INK, autoSkip: false } },
    };
  }

  function line(id, cfg) {
    const el = document.getElementById(id);
    if (!el || !window.Chart) return;
    base(Chart);
    new Chart(el, {
      type: "line",
      data: {
        labels: cfg.labels,
        datasets: cfg.series.map((s, i) => {
          const color = SLOTS[s.slot ?? i];
          return {
            // monotone keeps the smoothed curve from dipping below zero between whole-number counts
            label: s.label, data: s.data, borderColor: color, backgroundColor: color, borderWidth: 2,
            cubicInterpolationMode: "monotone",
            pointRadius: 0, pointHoverRadius: 5, pointHoverBackgroundColor: color,
            pointHoverBorderColor: SURFACE, pointHoverBorderWidth: 2,
            fill: cfg.series.length === 1 ? { target: "origin", above: color + "1f" } : false,
          };
        }),
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        interaction: { mode: "index", intersect: false },
        scales: scales(false, cfg.max, cfg.suffix),
        plugins: plugins(cfg.series.length, cfg.suffix),
      },
      plugins: [crosshair],
    });
  }

  // Vertical guide line under the hovered x position
  const crosshair = {
    id: "crosshair",
    afterDatasetsDraw(chart) {
      const active = chart.tooltip && chart.tooltip.getActiveElements();
      if (!active || !active.length) return;
      const { ctx, chartArea } = chart;
      const x = active[0].element.x;
      ctx.save();
      ctx.strokeStyle = "rgba(236, 238, 250, 0.25)";
      ctx.lineWidth = 1;
      ctx.setLineDash([3, 3]);
      ctx.beginPath();
      ctx.moveTo(x, chartArea.top);
      ctx.lineTo(x, chartArea.bottom);
      ctx.stroke();
      ctx.restore();
    },
  };

  window.Charts = { bar, line, SLOTS };
})();
